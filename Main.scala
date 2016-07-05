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

  def prepareAndCleanup[T](code: => T): T = {
    try {
      renescaDb.query("CREATE (:ANIMAL {name:'snake'})-[:EATS]->(:ANIMAL {name:'dog'})")
      code
    } finally {
      renescaDb.query("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r")
    }
  }

  val iterations = 5
  val times = 100
  for (i <- 0 until iterations) {
    val durationSeparate = List.fill(times) {
      prepareAndCleanup {
        Examples.separateQueries(renescaDb)
      }
    }.sum / times
    val durationChangeTracking = List.fill(times) {
      prepareAndCleanup {
        Examples.changeTracking(renescaDb)
      }
    }.sum / times
    println(durationSeparate, durationChangeTracking)
    Thread.sleep(1000)
  }

  // shut down actor systems
  renescaDb.restService.actorSystem.shutdown()
  rawREST.actorSystem.shutdown()
}
