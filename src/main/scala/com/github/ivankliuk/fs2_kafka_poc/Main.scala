package com.github.ivankliuk.fs2_kafka_poc

import cats.effect.{IO, IOApp}
import cats.syntax.parallel._
import cats.syntax.show._
import fs2.kafka._
import fs2.{Pipe, Stream}
import net.manub.embeddedkafka.EmbeddedKafka

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

  private val processConsumedRecord: CommittableConsumerRecord[IO, String, String] => IO[CommittableOffset[IO]] =
    ccr =>
      RemoteApiService.call(ccr.record.value).flatMap(value => PersistenceService.save(value, ccr.record.partition)) // flatMap
        .redeemWith(
          throwable => Log(s"Caught error during processing record ${ccr.record.show}): $throwable") *> IO.raiseError(throwable),
          _ => IO(ccr.offset)
        )

  private val fanOutPipe: Pipe[IO, Stream[IO, CommittableConsumerRecord[IO, String, String]], Stream[IO, Unit]] =
    _.map { streamOfStreams =>
      streamOfStreams.parEvalMapUnordered(Config.Consumer.ConcurrentProcessors)(processConsumedRecord)
        // We commit each partition independently.
        .through(commitBatchWithin(Config.Consumer.CommitOffsetsInBatchOf, Config.Consumer.CommitOffsetsInTimeWindow))
    }

  private val consumer: Stream[IO, Unit] =
    KafkaConsumer.stream(Config.Consumer.Settings)
      .evalTap(_.subscribeTo(Config.Topic))
      .flatMap(_.partitionedStream)
      .through(fanOutPipe)
      .parJoinUnbounded

  private val producer: Stream[IO, ProducerResult[Unit, String, String]] =
    generatePayload(Config.Producer.AmountMessagesToProduce)
      .map { case (partition, payload) =>
        ProducerRecords.one(ProducerRecord(Config.Topic, Config.Key, payload).withPartition(partition))
      }
      .through(KafkaProducer.pipe(Config.Producer.Settings))

  override def run: IO[Unit] = {
    IO {
      EmbeddedKafka.start()
      EmbeddedKafka.createCustomTopic(Config.Topic, partitions = 3)
    } >> (consumer.compile.drain &> producer.compile.drain) >> IO(EmbeddedKafka.stop())
  }.handleErrorWith {
    throwable => Log(s"Unexpected error occurred: ${throwable.getMessage}")
  }

}