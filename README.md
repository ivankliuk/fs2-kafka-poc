FS2 Kafka PoC
=============

Summary
-------

This project uses [fs2-kafka](https://fd4s.github.io/fs2-kafka) as [Apache Kafka](https://kafka.apache.org/) connector 
and [Akka HTTP as a client](https://doc.akka.io/docs/akka-http/current/client-side/request-level.html) for a 
[remote mocked REST API ](https://mocky.io). To run this project on MacOS or Linux see the following sections. 

Installation
------------

Install [Docker](https://www.docker.com)

Download or copy the contents of the Confluent Platform all-in-one Docker Compose file, for example:

```bash
curl --silent --output docker-compose.yml \
https://raw.githubusercontent.com/confluentinc/cp-all-in-one/6.1.1-post/cp-all-in-one/docker-compose.yml
```

Start Confluent Platform with the -d option to run in detached mode:

```bash
docker-compose up -d
```

To verify that the services are up and running, run the following command:

```bash
docker-compose ps
```

If the state is not `Up`, rerun the `docker-compose up -d` command.

Running
-------

To run the application:

```bash
$ sbt run
```
