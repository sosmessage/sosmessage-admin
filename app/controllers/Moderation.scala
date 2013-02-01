package controllers

import play.api.mvc._
import org.bson.types.ObjectId
import org.joda.time.DateTime
import com.mongodb.casbah.Imports._
import db.DB

object Moderation extends SosMessageController {

  val CategoriesCollectionName = "categories"
  val MessagesCollectionName = "messages"

  def index(state: String = "waiting") = Auth { implicit ctx => _ =>
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

  def approve(messageId: String, selectedTab: String) = Auth { implicit ctx => _ =>
    DB.collection(MessagesCollectionName) {
      messagesCollection =>
        val oid = new ObjectId(messageId)
        var message = messagesCollection.findOne(MongoDBObject("_id" -> oid)).get
        message += ("state" -> "approved")
        messagesCollection.save(message)

        DB.collection(CategoriesCollectionName) {
          categoriesCollection =>
            val q = MongoDBObject("_id" -> message.get("categoryId"))
            val o = $set(Seq("lastAddedMessageAt" -> DateTime.now()))
            categoriesCollection.update(q, o, false, false)

            Redirect(routes.Moderation.index(selectedTab)).flashing("actionDone" -> "messageApproved")
        }
    }
  }

  def reject(messageId: String, selectedTab: String) = Auth { implicit ctx => _ =>
    DB.collection(MessagesCollectionName) {
      c =>
        val oid = new ObjectId(messageId)
        var o = c.findOne(MongoDBObject("_id" -> oid)).get
        o += ("state" -> "rejected")
        c.save(o)
        Redirect(routes.Moderation.index(selectedTab)).flashing("actionDone" -> "messageRejected")
    }
  }

  def delete(messageId: String, selectedTab: String) = Auth { implicit ctx => _ =>
    DB.collection(MessagesCollectionName) {
      c =>
        val oid = new ObjectId(messageId)
        val o = MongoDBObject("_id" -> oid)
        c.remove(o)
        Redirect(routes.Moderation.index(selectedTab)).flashing("actionDone" -> "messageDeleted")
    }
  }

  def deleteAll(state: String) = Auth { implicit ctx => _ =>
    DB.collection(MessagesCollectionName) {
      c =>
        val o = MongoDBObject("state" -> state)
        c.remove(o)
        Redirect(routes.Moderation.index("waiting")).flashing("actionDone" -> (state + "MessagesDeleted"))
    }
  }

  def approveAll(state: String) = Auth { implicit ctx => _ =>
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
              val o = $set(Seq("lastAddedMessageAt" -> DateTime.now()))
              categoriesCollection.update(q, o, false, false)
            }
        }

        messagesCollection.update(MongoDBObject("state" -> state), $set(Seq("state" -> "approved")), false, true)
        Redirect(routes.Moderation.index("waiting")).flashing("actionDone" -> (state + "MessagesApproved"))
    }
  }
  def rejectAll(state: String) = Auth { implicit ctx => _ =>
    DB.collection(MessagesCollectionName) {
      c =>
        c.update(MongoDBObject("state" -> state), $set(Seq("state" -> "rejected")), false, true)
        Redirect(routes.Moderation.index("waiting")).flashing("actionDone" -> (state + "MessagesRejected"))
    }
  }

}
