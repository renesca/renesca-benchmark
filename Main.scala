package renesca.benchmark

import renesca.{DbService, RestService, Transaction, Query}
import spray.http.BasicHttpCredentials
import Benchmark._

object Main extends App {
  val credentials = BasicHttpCredentials("neo4j", "testingpw")

  val renescaDb = {
    val db = new DbService
    db.restService = new RestService("http://localhost:7474", Some(credentials))
    db
  }

  val rawREST = {
    new RawRestService("http://localhost:7474", Some(credentials))
  }

  // only proceed if database is available and empty
  val wholeGraph = renescaDb.queryWholeGraph
  if (wholeGraph.nonEmpty) {
    renescaDb.restService.actorSystem.shutdown()
    rawREST.actorSystem.shutdown()
    sys.error("Database is not empty.")
  }

  val iterations = 5
  for (i <- 0 until iterations) {
    val durationRenesca = benchmark(20) { Examples.idiomatic(renescaDb) }
    val durationRest = benchmark(20) { Examples.rest(rawREST) }
    println(durationRest, durationRenesca)
  }

  // shut down actor systems
  renescaDb.restService.actorSystem.shutdown()
  rawREST.actorSystem.shutdown()
}
