package com.github.ivankliuk.fs2_kafka_poc

import cats.effect.{IO, IOApp}
import cats.syntax.show._
import cats.syntax.parallel._
import fs2.{Pipe, Stream}
import fs2.kafka._

/**
 * fs2-kafka notes:
 * Offset commits are usually done in batches for performance reasons. We normally don't need to commit every offset,
 * but only the last processed offset. There is a trade-off in how much reprocessing we have to do when we restart
 * versus the performance implication of committing more frequently.
 *
 * The following implementation commits once every 500 offsets or 15 seconds, whichever happens first. Alternatively,
 * we can use commitBatch which uses the underlying chunking of the Stream, committing once every Chunk,
 * or the commitBatchOption function which does the same except when offsets are wrapped in Option.
 */
object Main extends IOApp.Simple {
  private def generatePayload(numberOfPayloadMessages: Option[Long] = None): Stream[IO, (Int, String)] = {
    val payloads = Stream(Config.RemoteApi.Objects: _*)
    val partitions = Stream.range(0, payloads.compile.toList.length)
    val stream = partitions.zip(payloads)
    numberOfPayloadMessages.fold(stream.repeat)(stream.repeatN)
  }

  private val processConsumedRecord: CommittableConsumerRecord[IO, String, String] => IO[Option[CommittableOffset[IO]]] =
    ccr =>
      RemoteApiService.call(ccr.record.value).flatMap(value => PersistenceService.save(value, ccr.record.partition)) // flatMap
        .redeemWith(
          throwable => Logger.error(s"Caught error during processing record ${ccr.record.show}): $throwable") *> IO.pure(None),
          _ => IO(Some(ccr.offset))
        )

  private val fanOutPipe: Pipe[IO, KafkaConsumer[IO, String, String], Option[CommittableOffset[IO]]] =
    _.flatMap(_.partitionedStream)
      .map(_.parEvalMapUnordered(Config.Consumer.ConcurrentProcessors)(processConsumedRecord))
      .parJoinUnbounded

  private val successfulOperationsFilterPipe: Pipe[IO, Option[CommittableOffset[IO]], CommittableOffset[IO]] =
    _.collect { case Some(i) => i }

  private val consumer: Stream[IO, Unit] =
    KafkaConsumer.stream(Config.Consumer.Settings)
      .evalTap(_.subscribeTo(Config.Topic))
      .through(fanOutPipe)
      .through(successfulOperationsFilterPipe)
      .through(commitBatchWithin(Config.Consumer.CommitOffsetsInBatchOf, Config.Consumer.CommitOffsetsInTimeWindow))

  private val producer: Stream[IO, ProducerResult[Unit, String, String]] = generatePayload(Config.Producer.AmountMessagesToProduce)
    .map { case (partition, payload) =>
      ProducerRecords.one(ProducerRecord(Config.Topic, Config.Key, payload).withPartition(partition))
    }
    .through(KafkaProducer.pipe(Config.Producer.Settings))

  override def run: IO[Unit] =
    (consumer.compile.drain &> producer.compile.drain).handleErrorWith {
      throwable => Logger.error(s"Unexpected error occurred: ${throwable.getMessage}")
    }

}