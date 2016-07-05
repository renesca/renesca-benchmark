package renesca.benchmark

import renesca.graph.{Node, Relation, Id}
import renesca.parameter._
import renesca.parameter.implicits._
import renesca.{DbService, Query}

import play.api.libs.json._

import Benchmark._

object Examples {
  def changeTracking(db: DbService): Double = {
    time {
      {
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
      }

      {
        val tx = db.newTransaction
        val graph = tx.queryGraph(Query(
          """MATCH (n:ANIMAL {name: {name}}) return n""",
          Map("name" -> "hippo")
        ))
        val hippo = graph.nodes.head
        hippo.properties("nose") = true
        tx.commit.persistChanges(graph)
      }
    }
  }

  // def rest(db: RawRestService): Double = {
  def separateQueries(db: DbService): Double = {
    time {
      {
        val tx = db.newTransaction
        implicit val graph = tx.queryGraph("MATCH (n:ANIMAL)-[r]->() RETURN n,r")
        val snake = graph.nodes.find(_.properties("name").
          asInstanceOf[StringPropertyValue] == "snake").get
        val snakeid = snake.origin.asInstanceOf[Id].id
        tx.query("MATCH (snake) WHERE id(snake) = {snakeid} SET snake:REPTILE, snake.hungry = true", Map("snakeid" -> snakeid))
        val hippoid = tx.queryGraph("""CREATE (hippo:ANIMAL {name: "hippo"}) RETURN hippo""").nodes.head.origin.asInstanceOf[Id].id

        tx.query("""MATCH (snake),(hippo) WHERE id(snake) = {snakeid} AND id(hippo) = {hippoid} CREATE (snake)-[:EATS]->(hippo)""", Map("snakeid" -> snakeid, "hippoid" -> hippoid))

        tx.commit.persistChanges(graph)
      }

      {
        val tx = db.newTransaction
        val graph = tx.queryGraph(Query(
          """MATCH (n:ANIMAL {name: {name}}) return n""",
          Map("name" -> "hippo")
        ))
        val hippoid = graph.nodes.head.origin.asInstanceOf[Id].id
        tx.query("MATCH (hippo) WHERE id(hippo) = {hippoid} SET hippo.nose = true", Map("hippoid" -> hippoid))
        tx.commit.persistChanges(graph)
      }
    }
  }
}
