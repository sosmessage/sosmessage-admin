package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import java.util.Date
import com.mongodb.DBObject
import com.mongodb.casbah.query.Imports._
import conf.SosMessageConfiguration
import com.mongodb.casbah.MongoConnection

case class Message(categoryId: String, text: String, contributorName: String,
  approved: Option[String])

object Messages extends Controller {

  val config = SosMessageConfiguration.getConfig

  val CategoriesCollectionName = "categories"
  val MessagesCollectionName = "messages"

  val dataBaseName = config[String]("database.name", "sosmessage")

  val mongo = MongoConnection(config[String]("database.host", "127.0.0.1"), config[Int]("database.port", 27017))

  val categoriesCollection = mongo(dataBaseName)(CategoriesCollectionName)
  val messagesCollection = mongo(dataBaseName)(MessagesCollectionName)

  val messageForm = Form(
    mapping(
      "categoryId" -> nonEmptyText,
      "text" -> nonEmptyText,
      "contributorName" -> text,
      "approved" -> optional(text)
    )(Message.apply)(Message.unapply)
  )

  def indexNoCategory = Action {
    implicit request =>
      val categoryOrder = MongoDBObject("name" -> 1)
      val categories = categoriesCollection.find().sort(categoryOrder).foldLeft(List[DBObject]())((l, a) =>
        a :: l
      ).reverse
      val categoryId = categories(0).get("_id").toString
      Redirect(routes.Messages.index(categoryId))
  }

  def index(categoryId: String) = Action {
    implicit request =>
      val categoryOrder = MongoDBObject("name" -> 1)
      val categories = categoriesCollection.find().sort(categoryOrder).foldLeft(List[DBObject]())((l, a) =>
        a :: l
      ).reverse

      val q = MongoDBObject("categoryId" -> new ObjectId(categoryId), "state" -> "approved")
      val order = MongoDBObject("createdAt" -> -1)
      val messages = messagesCollection.find(q).limit(1000).sort(order).toSeq.map( message =>
        computeRatingInformation(message).asDBObject
      )
      Ok(views.html.messages.index(categories, categoryId, messages.toList, messageForm))
  }

  def computeRatingInformation(message: MongoDBObject): MongoDBObject = {
    message.get("ratings") match {
      case None => {
        val builder = MongoDBObject.newBuilder
        builder += ("ratingCount" -> 0)
        builder += ("rating" -> 0.0)
        message.putAll(builder.result())
      }
      case Some(r) => {
        var count: Long = 0
        var total: Double = 0.0

        val ratings = new MongoDBObject(r.asInstanceOf[DBObject])
        for ((k, v) <- ratings) {
          val value = v.asInstanceOf[Int]
          count += 1
          total += value
        }

        val builder = MongoDBObject.newBuilder
        val avg = if (total == 0 || count == 0) 0.0 else total / count
        builder += ("ratingCount" -> count)
        builder += ("rating" -> avg)
        message.putAll(builder.result())
        message.removeField("ratings")
      }
    }
    message
  }

  def save(selectedCategoryId: String) = Action {
    implicit request =>
      messageForm.bindFromRequest().fold(
        formWithErrors => {
          Redirect(routes.Messages.index(selectedCategoryId))
        },
        message => {
          val oid = new ObjectId(message.categoryId)
          val q = MongoDBObject("_id" -> oid)
          val category = categoriesCollection.findOne(q).get
          val builder = MongoDBObject.newBuilder
          builder += "categoryId" -> category.get("_id")
          builder += "category" -> category.get("name")
          builder += "text" -> message.text
          builder += "contributorName" -> message.contributorName
          val actionDone = message.approved match {
            case None =>
              builder += "state" -> "waiting"
              "messageWaiting"
            case Some(s) =>
              builder += "state" -> "approved"
              val o = $set("lastAddedMessageAt" -> new Date())
              categoriesCollection.update(q, o, false, false)
              "messageAdded"
          }
          builder += "createdAt" -> new Date()
          builder += "modifiedAt" -> new Date()
          builder += "random" -> scala.math.random
          messagesCollection += builder.result

          Redirect(routes.Messages.index(category.get("_id").toString)).flashing("actionDone" -> actionDone)
        }
      )
  }

  def delete(selectedCategoryId: String, messageId: String) = Action {
    implicit request =>
      val oid = new ObjectId(messageId)
      val o = MongoDBObject("_id" -> oid)
      messagesCollection.remove(o)
      Redirect(routes.Messages.index(selectedCategoryId)).flashing("actionDone" -> "messageDeleted")
  }

  def edit(categoryId: String, messageId: String) = Action {
    implicit request =>
      val categoryOrder = MongoDBObject("name" -> 1)
      val categories = categoriesCollection.find().sort(categoryOrder).foldLeft(List[DBObject]())((l, a) =>
        a :: l
      ).reverse
      val q = MongoDBObject("_id" -> new ObjectId(messageId))
      messagesCollection.findOne(q).map {
        message =>
          val m = Message(message.get("categoryId").toString, message.get("text").toString,
            message.get("contributorName").toString, None)
          Ok(views.html.messages.edit(categories, categoryId, messageId, messageForm.fill(m)))
      }.getOrElse(NotFound)
  }

  def update(categoryId: String, messageId: String) = Action {
    implicit request =>
      messageForm.bindFromRequest.fold(
        formWithErrors => {
          Redirect(routes.Messages.index(categoryId))
        },
        message => {
          val newCategoryId = message.categoryId
          val q = MongoDBObject("_id" -> new ObjectId(messageId))
          val o = $set("categoryId" -> new ObjectId(newCategoryId), "text" -> message.text,
            "contributorName" -> message.contributorName, "modifiedAt" -> new Date())
          messagesCollection.update(q, o, false, false)
          Redirect(routes.Messages.index(newCategoryId)).flashing("actionDone" -> "messageUpdated")
        }
      )
  }

}
