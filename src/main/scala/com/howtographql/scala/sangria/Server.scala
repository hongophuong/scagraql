package com.howtographql.scala.sangria

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import spray.json.JsValue
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.Await
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

object Server extends App {

  val PORT = 8080

  implicit val actorSystem = ActorSystem("graphql-server")
  implicit val materializer = ActorMaterializer()

  import scala.concurrent.duration._

  scala.sys.addShutdownHook(() -> shutdown())


  val route: Route =
    (post & path("graphql")) {
      val um = as[JsValue]
      entity(um) { requestJson =>
        GraphQLServer.endpoint(requestJson)
      }
    } ~ {
      getFromResource("graphiql.html")
    }
  Http().bindAndHandle(route, "0.0.0.0", PORT)
  println(s"open a browser with URL: http://localhost:$PORT")


  def shutdown(): Unit = {
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 30 seconds)
  }
}
