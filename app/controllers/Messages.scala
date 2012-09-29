package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.joda.time.DateTime
import com.mongodb.DBObject
import com.mongodb.casbah.query.Imports._
import db.DB

case class Message(categoryId: String, text: String, contributorName: String,
  approved: Option[String])

object Messages extends Controller with Secured {

  val CategoriesCollectionName = "categories"
  val MessagesCollectionName = "messages"

  val messageForm = Form(
    mapping(
      "categoryId" -> nonEmptyText,
      "text" -> nonEmptyText,
      "contributorName" -> text,
      "approved" -> optional(text)
    )(Message.apply)(Message.unapply)
  )

  def indexNoCategory = IsAuthenticated { _ =>
    implicit request =>
      DB.collection(CategoriesCollectionName) {
        c =>
          val categoryOrder = MongoDBObject("name" -> 1)
          val categories = c.find().sort(categoryOrder).foldLeft(List[DBObject]())((l, a) =>
            a :: l
          ).reverse
          val categoryId = categories(0).get("_id").toString
          Redirect(routes.Messages.index(categoryId))
      }
  }

  def index(categoryId: String) = IsAuthenticated { _ =>
    implicit request =>
      DB.collection(CategoriesCollectionName) {
        categoriesCollection =>
          val categoryOrder = MongoDBObject("name" -> 1)
          val categories = categoriesCollection.find().sort(categoryOrder).foldLeft(List[DBObject]())((l, a) =>
            a :: l
          ).reverse

          DB.collection(MessagesCollectionName) {
            messagesCollection =>
              val q = MongoDBObject("categoryId" -> new ObjectId(categoryId), "state" -> "approved")
              val order = MongoDBObject("createdAt" -> -1)
              val messages = messagesCollection.find(q).limit(0).sort(order).toSeq.map( message =>
                computeRatingInformation(message).asDBObject
              )
              Ok(views.html.messages.index(categories, categoryId, messages.toList, messageForm))
          }
      }
  }

  def computeRatingInformation(message: MongoDBObject): MongoDBObject = {
    message.get("ratings") match {
      case None => {
        val builder = MongoDBObject.newBuilder
        builder += ("ratingCount" -> 0L)
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

  def save(selectedCategoryId: String) = IsAuthenticated { _ =>
    implicit request =>
      messageForm.bindFromRequest().fold(
        formWithErrors => {
          Redirect(routes.Messages.index(selectedCategoryId))
        },
        message => {
          DB.collection(MessagesCollectionName) {
            c =>
              val oid = new ObjectId(message.categoryId)
              val q = MongoDBObject("_id" -> oid)
              val category = c.findOne(q).get
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
                  val o = $set("lastAddedMessageAt" -> DateTime.now())
                  c.update(q, o, false, false)
                  "messageAdded"
              }
              builder += "createdAt" -> DateTime.now()
              builder += "modifiedAt" -> DateTime.now()
              builder += "random" -> scala.math.random
              c += builder.result

              Redirect(routes.Messages.index(category.get("_id").toString)).flashing("actionDone" -> actionDone)
          }
        }
      )
  }

  def delete(selectedCategoryId: String, messageId: String) = IsAuthenticated { _ =>
    implicit request =>
      DB.collection(MessagesCollectionName) {
        c =>
          val oid = new ObjectId(messageId)
          val o = MongoDBObject("_id" -> oid)
          c.remove(o)
          Redirect(routes.Messages.index(selectedCategoryId)).flashing("actionDone" -> "messageDeleted")
      }
  }

  def edit(categoryId: String, messageId: String) = IsAuthenticated { _ =>
    implicit request =>
      DB.collection(CategoriesCollectionName) {
        categoriesCollection =>
          val categoryOrder = MongoDBObject("name" -> 1)
          val categories = categoriesCollection.find().sort(categoryOrder).foldLeft(List[DBObject]())((l, a) =>
            a :: l
          ).reverse

          DB.collection(MessagesCollectionName) {
            messagesCollection =>
              val q = MongoDBObject("_id" -> new ObjectId(messageId))
              messagesCollection.findOne(q).map {
                message =>
                  val m = Message(message.get("categoryId").toString, message.get("text").toString,
                    message.get("contributorName").toString, None)
                  Ok(views.html.messages.edit(categories, categoryId, messageId, messageForm.fill(m)))
              }.getOrElse(NotFound)
          }
      }
  }

  def update(categoryId: String, messageId: String) = IsAuthenticated { _ =>
    implicit request =>
      messageForm.bindFromRequest.fold(
        formWithErrors => {
          Redirect(routes.Messages.index(categoryId))
        },
        message => {
          DB.collection(MessagesCollectionName) {
            c =>
              val newCategoryId = message.categoryId
              val q = MongoDBObject("_id" -> new ObjectId(messageId))
              val o = $set("categoryId" -> new ObjectId(newCategoryId), "text" -> message.text,
                "contributorName" -> message.contributorName, "modifiedAt" -> DateTime.now())
              c.update(q, o, false, false)
              Redirect(routes.Messages.index(newCategoryId)).flashing("actionDone" -> "messageUpdated")
          }
        }
      )
  }

}
