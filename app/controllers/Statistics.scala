package controllers

import play.api.mvc._
import com.mongodb.casbah.MongoConnection
import conf.SosMessageConfig

object Statistics extends Controller {

  val EventLogsCollectionName = "eventlogs"

  def index = Action { implicit request =>
//    val eventLogOrder = MongoDBObject("title" -> 1)
//    val eventLogs = eventLogsCollection.find().sort(eventLogOrder).foldLeft(List[DBObject]())((l, a) =>
//      a :: l
//    ).reverse
    Ok(views.html.stats.index())
  }

  def hello = Action { implicit request =>
    Ok("HELLO")
  }

}
