package controllers

import play.api.mvc._
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import com.mongodb.casbah.query.Imports._
import java.util.Date
import db.DB

object Moderation extends Controller {

  val CategoriesCollectionName = "categories"
  val MessagesCollectionName = "messages"

  def index(state: String = "waiting") = Action { implicit request =>
    DB.collection(MessagesCollectionName) {
      c =>
        val messageOrder = MongoDBObject("createdAt" -> -1)
        val q = MongoDBObject("state" -> state)
        val messages = c.find(q).sort(messageOrder).foldLeft(List[DBObject]())((l, a) =>
          a :: l
        ).reverse
        Ok(views.html.moderation.index(state, messages))
    }
  }

  def approve(messageId: String, selectedTab: String) = Action { implicit request =>
    DB.collection(MessagesCollectionName) {
      messagesCollection =>
        val oid = new ObjectId(messageId)
        var message = messagesCollection.findOne(MongoDBObject("_id" -> oid)).get
        message += ("state" -> "approved")
        messagesCollection.save(message)

        DB.collection(CategoriesCollectionName) {
          categoriesCollection =>
            val q = MongoDBObject("_id" -> message.get("categoryId"))
            val o = $set("lastAddedMessageAt" -> new Date())
            categoriesCollection.update(q, o, false, false)

            Redirect(routes.Moderation.index(selectedTab)).flashing("actionDone" -> "messageApproved")
        }
    }
  }

  def reject(messageId: String, selectedTab: String) = Action { implicit request =>
    DB.collection(MessagesCollectionName) {
      c =>
        val oid = new ObjectId(messageId)
        var o = c.findOne(MongoDBObject("_id" -> oid)).get
        o += ("state" -> "rejected")
        c.save(o)
        Redirect(routes.Moderation.index(selectedTab)).flashing("actionDone" -> "messageRejected")
    }
  }

  def delete(messageId: String, selectedTab: String) = Action { implicit request =>
    DB.collection(MessagesCollectionName) {
      c =>
        val oid = new ObjectId(messageId)
        val o = MongoDBObject("_id" -> oid)
        c.remove(o)
        Redirect(routes.Moderation.index(selectedTab)).flashing("actionDone" -> "messageDeleted")
    }
  }

  def deleteAll(state: String) = Action { implicit request =>
    DB.collection(MessagesCollectionName) {
      c =>
        val o = MongoDBObject("state" -> state)
        c.remove(o)
        Redirect(routes.Moderation.index("waiting")).flashing("actionDone" -> (state + "MessagesDeleted"))
    }
  }

  def approveAll(state: String) = Action { implicit request =>
    DB.collection(MessagesCollectionName) {
      messagesCollection =>
        val keys = MongoDBObject("categoryId" -> 1)
        val categoryIds = messagesCollection.find(MongoDBObject("state" -> state), keys).foldLeft(List[ObjectId]())((l, a) =>
          a.get("categoryId").asInstanceOf[ObjectId] :: l
        ).distinct

        DB.collection(CategoriesCollectionName) {
          categoriesCollection =>
            categoryIds.map { id =>
              val q = MongoDBObject("_id" -> id)
              val o = $set("lastAddedMessageAt" -> new Date())
              categoriesCollection.update(q, o, false, false)
            }
        }

        messagesCollection.update(MongoDBObject("state" -> state), $set("state" -> "approved"), false, true)
        Redirect(routes.Moderation.index("waiting")).flashing("actionDone" -> (state + "MessagesApproved"))
    }
  }
  def rejectAll(state: String) = Action { implicit request =>
    DB.collection(MessagesCollectionName) {
      c =>
        c.update(MongoDBObject("state" -> state), $set("state" -> "rejected"), false, true)
        Redirect(routes.Moderation.index("waiting")).flashing("actionDone" -> (state + "MessagesRejected"))
    }
  }

}
