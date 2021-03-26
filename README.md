FS2 Kafka PoC
=============

Summary
-------

This project uses:
* [embedded-kafka](https://github.com/embeddedkafka/embedded-kafka) as Kafka broker
* [fs2-kafka](https://fd4s.github.io/fs2-kafka) as [Apache Kafka](https://kafka.apache.org/) connector 
* [Akka HTTP as a client](https://doc.akka.io/docs/akka-http/current/client-side/request-level.html) for a 
[remote mocked REST API ](https://mocky.io). 

Running
-------

To run the application:

```bash
$ sbt run
```

TODO
----

* [Graceful shutdown](https://fd4s.github.io/fs2-kafka/docs/consumers#graceful-shutdown)
