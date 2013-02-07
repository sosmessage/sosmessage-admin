package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import org.bson.types.ObjectId
import org.joda.time.DateTime
import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import db.DB

case class Message(categoryId: String, text: String, contributorName: String,
  approved: Option[String])

object Messages extends SosMessageController {

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

  def indexNoCategory = Auth { implicit ctx => _ =>
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

  def index(categoryId: String, sort: String = "all") = Auth { implicit ctx => _ =>
      DB.collection(CategoriesCollectionName) {
        categoriesCollection =>
          val categoryOrder = MongoDBObject("name" -> 1)
          val categories = categoriesCollection.find().sort(categoryOrder).foldLeft(List[DBObject]())((l, a) =>
            a :: l
          ).reverse

          DB.collection(MessagesCollectionName) {
            messagesCollection =>
              sort match {
                case "best" => {
                  val q = MongoDBObject("categoryId" -> new ObjectId(categoryId), "state" -> "approved")
                  val messages = messagesCollection.find(q).toSeq.map(message => computeRatingInformation(message).asDBObject)
                   .sortBy(m => m.get("score").asInstanceOf[Double]).reverse.slice(0, 20)
                  Ok(views.html.messages.index(categories, categoryId, messages.toList, messageForm, "best"))
                }
                case "worst" => {
                  val q = MongoDBObject("categoryId" -> new ObjectId(categoryId), "state" -> "approved")
                  val messages = messagesCollection.find(q).toSeq.map(message => computeRatingInformation(message).asDBObject)
                   .sortBy(m => m.get("score").asInstanceOf[Double]).slice(0, 20)
                  Ok(views.html.messages.index(categories, categoryId, messages.toList, messageForm, "worst"))
                }
                case _ => {
                  val q = MongoDBObject("categoryId" -> new ObjectId(categoryId), "state" -> "approved")
                  val order = MongoDBObject("createdAt" -> -1)
                  val messages = messagesCollection.find(q).limit(0).sort(order).toSeq.map(message =>
                    computeRatingInformation(message).asDBObject
                  )
                  Ok(views.html.messages.index(categories, categoryId, messages.toList, messageForm, "all"))
                }
              }

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
        var votePlus = 0
        var voteMinus = 0

        val ratings = new MongoDBObject(r.asInstanceOf[DBObject])
        for ((k, v) <- ratings) {
          val value = v.asInstanceOf[Int]
          val vote = if (value == 1.0) -1 else 1
          if (vote == 1) votePlus += 1 else voteMinus += 1
          count += 1
          total += value
        }

        val builder = MongoDBObject.newBuilder
        val avg = if (total == 0 || count == 0) 0.0 else total / count
        builder += ("ratingCount" -> count)
        builder += ("rating" -> avg)

        val s = votePlus - voteMinus
        val sign = if (s < 0) -1.0 else if (s > 0) 1.0 else 0
        val score = sign * ((20 * 3) + (count * avg)) / (20 + count)
        builder += ("score" -> score)

        message.putAll(builder.result())
        message.removeField("ratings")
      }
    }
    message
  }

  def save(selectedCategoryId: String) = Auth { implicit ctx => _ =>
    implicit val req = ctx.req
    messageForm.bindFromRequest().fold(
      formWithErrors => {
        Redirect(routes.Messages.index(selectedCategoryId))
      },
      message => {
        DB.collection(CategoriesCollectionName) {
        categoriesCollection =>
          DB.collection(MessagesCollectionName) {
            messagesCollection =>
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
                  val o = $set(Seq("lastAddedMessageAt" -> DateTime.now()))
                  categoriesCollection.update(q, o, false, false)
                  "messageAdded"
              }
              builder += "createdAt" -> DateTime.now()
              builder += "modifiedAt" -> DateTime.now()
              builder += "random" -> scala.math.random
              messagesCollection += builder.result

              Redirect(routes.Messages.index(category.get("_id").toString)).flashing("actionDone" -> actionDone)
          }
        }
      }
    )
  }

  def delete(selectedCategoryId: String, messageId: String) = Auth { implicit ctx => _ =>
      DB.collection(MessagesCollectionName) {
        c =>
          val oid = new ObjectId(messageId)
          val o = MongoDBObject("_id" -> oid)
          c.remove(o)
          Redirect(routes.Messages.index(selectedCategoryId)).flashing("actionDone" -> "messageDeleted")
      }
  }

  def edit(categoryId: String, messageId: String) = Auth { implicit ctx => _ =>
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

  def update(categoryId: String, messageId: String) = Auth { implicit ctx => _ =>
    implicit val req = ctx.req
    messageForm.bindFromRequest.fold(
      formWithErrors => {
        Redirect(routes.Messages.index(categoryId))
      },
      message => {
        DB.collection(MessagesCollectionName) {
          c =>
            val newCategoryId = message.categoryId
            val q = MongoDBObject("_id" -> new ObjectId(messageId))
            val o = $set(Seq("categoryId" -> new ObjectId(newCategoryId), "text" -> message.text,
              "contributorName" -> message.contributorName, "modifiedAt" -> DateTime.now()))
            c.update(q, o, false, false)
            Redirect(routes.Messages.index(newCategoryId)).flashing("actionDone" -> "messageUpdated")
        }
      }
    )
  }

}
