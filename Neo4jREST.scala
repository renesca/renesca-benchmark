package renesca.benchmark

import akka.actor.ActorSystem
import akka.util.Timeout
import spray.client.pipelining._
import spray.http.HttpHeaders.{Authorization, Location, RawHeader, `Content-Type`}
import spray.http.HttpMethods._
import spray.http.{HttpRequest, _}
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import HttpCharsets._
import MediaTypes._

import play.api.libs.json._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class RawRestService(val server: String, credentials: Option[BasicHttpCredentials] = None, implicit val timeout: Timeout = Timeout(60.seconds)) {
  // http://spray.io/documentation/1.2.2/spray-can/http-client/request-level/
  // http://spray.io/documentation/1.2.2/spray-client/
  implicit val actorSystem: ActorSystem = ActorSystem()

  // dispatcher provides execution context

  import actorSystem.dispatcher

  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  private def awaitResponse(request: HttpRequest): HttpResponse = Await.result(pipeline(request), timeout.duration)

  private def buildUri(path: String) = Uri(s"$server$path")

  private def buildHttpPostRequest(path: String, jsonRequest: String): HttpRequest = {
    val headers = new mutable.ListBuffer[HttpHeader]()
    headers ++= credentials.map(Authorization(_))
    headers += RawHeader("X-Stream", "true")

    Post(
      buildUri(path),
      HttpEntity(MediaTypes.`application/json`, jsonRequest)
    ).withHeaders(headers.toList)
  }

  private def buildHttpPutRequest(path: String, jsonRequest: String): HttpRequest = {
    val headers = new mutable.ListBuffer[HttpHeader]()
    headers ++= credentials.map(Authorization(_))
    headers += RawHeader("X-Stream", "true")

    Put(
      buildUri(path),
      HttpEntity(MediaTypes.`application/json`, jsonRequest)
    ).withHeaders(headers.toList)
  }

  def awaitPostResponse(path: String, jsonRequest: String): JsValue = {
    val httpRequest = buildHttpPostRequest(path, jsonRequest)
    // println(httpRequest)
    val httpResponse = awaitResponse(httpRequest)
    // println(httpResponse)
    val jsonResponse = httpResponse.entity.asString
    Json.parse(jsonResponse)
  }

  def awaitPutResponse(path: String, jsonRequest: String): JsValue = {
    val httpRequest = buildHttpPutRequest(path, jsonRequest)
    // println(httpRequest)
    val httpResponse = awaitResponse(httpRequest)
    // println(httpResponse)
    val jsonResponse = httpResponse.entity.asString
    if (jsonResponse.nonEmpty) Json.parse(jsonResponse) else JsString("")
  }
}
