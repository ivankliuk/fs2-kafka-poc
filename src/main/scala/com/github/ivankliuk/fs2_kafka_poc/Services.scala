package com.github.ivankliuk.fs2_kafka_poc

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import cats.effect.IO
import cats.syntax.all._

import scala.util.Random

object PersistenceService {
  def save(value: String, partition: Int): IO[Unit] = Random.nextInt match {
    // A random chance of failure is introduced to model an error during the saving.
    case x if x % 2 == 0 => IO.raiseError(new PersistenceServiceException(s"Unable to save $value from partition $partition"))
    case _ => Log(s"Saved $value from partition $partition to the repository")
  }
}

object RemoteApiService {

  private implicit val system = ActorSystem(Behaviors.empty, Config.Akka.SystemName)
  private implicit val executionContext = system.executionContext

  def call(objectId: String): IO[String] = Log(s"Remote Url to be called ${Config.RemoteApi.Uri}$objectId") *>
    IO.fromFuture {
      IO {
        // A random chance of failure is introduced to model an error when calling the remote API.
        val errorMixinObjectId = if (Random.nextInt(21) > 2) objectId else "non_existent_id"
        Http().singleRequest(HttpRequest(uri = s"${Config.RemoteApi.Uri}$errorMixinObjectId"))
      }
    } >>= { // flatMap
    case HttpResponse(OK, _, entity, _) => IO.fromFuture {
      IO(Unmarshal(entity).to[String].map(_.replace("\n", "").replace("\r", "")))
    }
    case _ => IO.raiseError(new RemoteApiServiceException(s"Unable to fetch $objectId from the remote API"))
  }
}
