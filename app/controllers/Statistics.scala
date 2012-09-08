package controllers

import play.api.mvc._
import com.mongodb.casbah.MongoConnection
import conf.SosMessageConfiguration

object Statistics extends Controller {

  val EventLogsCollectionName = "eventlogs"

  val config = SosMessageConfiguration.getConfig

  val dataBaseName = config[String]("database.name", "sosmessage")

  val mongo = MongoConnection(config[String]("database.host", "127.0.0.1"), config[Int]("database.port", 27017))

  val eventLogsCollection = mongo(dataBaseName)(EventLogsCollectionName)

  def index = Action { implicit request =>
//    val eventLogOrder = MongoDBObject("title" -> 1)
//    val eventLogs = eventLogsCollection.find().sort(eventLogOrder).foldLeft(List[DBObject]())((l, a) =>
//      a :: l
//    ).reverse
    Ok(views.html.stats.index())
  }

}
