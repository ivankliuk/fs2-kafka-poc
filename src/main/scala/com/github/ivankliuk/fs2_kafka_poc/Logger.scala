package com.github.ivankliuk.fs2_kafka_poc

import cats.effect.IO

object Logger {
  def info(message: String): IO[Unit] = IO(println(s"INFO:  $message"))
  def error(message: String): IO[Unit] = IO(println(s"ERROR: $message"))
}
