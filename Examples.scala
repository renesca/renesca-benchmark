package renesca.benchmark

import renesca.graph.{Node, Relation}
import renesca.parameter._
import renesca.parameter.implicits._
import renesca.{DbService, Query}

import play.api.libs.json._

object Examples {
  def idiomatic(db: DbService) {
    try {
      db.query("CREATE (:ANIMAL {name:'snake'})-[:EATS]->(:ANIMAL {name:'dog'})")

      val tx = db.newTransaction
      implicit val graph = tx.queryGraph("MATCH (n:ANIMAL)-[r]->() RETURN n,r")
      val snake = graph.nodes.find(_.properties("name").
        asInstanceOf[StringPropertyValue] == "snake").get
      snake.labels += "REPTILE"
      snake.properties("hungry") = true
      val hippo = Node.create
      hippo.labels += "ANIMAL"
      hippo.properties("name") = "hippo"
      graph.nodes += hippo
      graph.relations += Relation.create(snake, "EATS", hippo)
      tx.commit.persistChanges(graph)

      db.transaction { tx =>
        val hippo = tx.queryGraph(
          Query(
            """MATCH (n:ANIMAL {name: {name}}) return n""",
            Map("name" -> "hippo")
          )
        ).nodes.head
        hippo.properties("nose") = true
      }

    } finally {
      db.query("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r")
    }
  }

  def rest(db: RawRestService) {
    try {
      val r1 = db.awaitPostResponse("/db/data/transaction/commit", """{ "statements" : [{ "statement" : 
    "CREATE (:ANIMAL {name:'snake'})-[:EATS]->(:ANIMAL {name:'dog'})"
    }] }""")

      val r2 = db.awaitPostResponse("/db/data/transaction", """{ "statements" : [{ "statement" : 
    "MATCH (n:ANIMAL)-[r]->() RETURN n,r"
    }] }""")
      val txUrl = (r2 \ "commit").as[String].stripPrefix(db.server).stripSuffix("/commit")
      val snakeId = ((((r2 \ "results")(0) \ "data")(0) \ "meta").as[Seq[JsValue]].find(j => (j \ "type").as[String] == "node").get \ "id").as[Long]

      val r3 = db.awaitPutResponse(txUrl + s"/node/$snakeId/properties/hungry", """true""")
    } finally {
      db.awaitPostResponse("/db/data/transaction/commit", """{ "statements" : [{ "statement" : "MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r"}] }""")
    }
  }
}
