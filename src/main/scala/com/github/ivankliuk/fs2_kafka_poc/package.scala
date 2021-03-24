package com.github.ivankliuk

import cats.Show
import fs2.kafka.ConsumerRecord

package object fs2_kafka_poc {
  implicit val committableConsumerRecordShow: Show[ConsumerRecord[String, String]] = Show.show[ConsumerRecord[String, String]](r =>
    s"ConsumerRecord(topic = ${r.topic}, partition = ${r.partition}, key = ${r.key}, value = ${r.value}")
}
