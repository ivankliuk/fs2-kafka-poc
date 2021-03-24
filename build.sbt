name := "fs2-kafka-poc"

version := "0.1"

scalaVersion := "2.13.5"

val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.4"

libraryDependencies ++= Seq(
//  "ch.qos.logback" % "logback-classic" % "1.2.3" % Runtime,
  "com.github.fd4s" %% "fs2-kafka" % "3.0.0-M2",
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
)

Compile / run / fork := true
