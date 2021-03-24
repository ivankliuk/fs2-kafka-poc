package com.github.ivankliuk.fs2_kafka_poc

import cats.effect.IO
import fs2.kafka.{AutoOffsetReset, ConsumerSettings, ProducerSettings}

import scala.concurrent.duration._

object Config {
  val BootstrapServers = "localhost:9092"
  val Topic = "default-topic"
  val Key = "default-key"

  object Consumer {
    val CommitOffsetsInBatchOf = 500
    val CommitOffsetsInTimeWindow: FiniteDuration = 15.seconds
    val ConcurrentProcessors = 3
    val Settings: ConsumerSettings[IO, String, String] =
      ConsumerSettings[IO, String, String]
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withBootstrapServers(BootstrapServers)
        .withGroupId("default-consumer-group-id")
  }

  object Producer {
    val AmountMessagesToProduce: Option[Long] = Some(20)
    val Settings: ProducerSettings[IO, String, String] =
      ProducerSettings[IO, String, String]
        .withBootstrapServers(BootstrapServers)
  }

  object Akka {
    val SystemName = "default-actor-system"
  }

  object RemoteApi {
    val Uri = "https://run.mocky.io/v3/"
    val Objects = Seq(
      "efbdf571-67eb-47f5-b15f-c42def230b9d",
      "a31a8dfd-9a2f-4ac3-872f-5804faaee4a0",
      "6f74ca30-c15d-403f-b816-d350d4cb3499"
    )
  }

}
