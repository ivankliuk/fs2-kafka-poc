package com.github.ivankliuk.fs2_kafka_poc

import cats.effect.IO

object Log {
  def apply(message: String): IO[Unit] = IO(println(s" >>> $message"))
}
