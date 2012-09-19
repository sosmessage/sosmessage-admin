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

case class Announcement(title: String, text: String, url: String, validateButton: String, cancelButton: String)

object Announcements extends Controller {

  val AnnouncementsCollectionName = "announcements"

  val announcementForm = Form(
    mapping(
      "title" -> nonEmptyText,
      "text" -> nonEmptyText,
      "url" -> text,
      "validateButton" -> text,
      "cancelButton" -> text
    )(Announcement.apply)(Announcement.unapply)
  )

  def index = Action { implicit request =>
    DB.collection(AnnouncementsCollectionName) {
      c =>
        val announcementOrder = MongoDBObject("title" -> 1)
        val announcements = c.find().sort(announcementOrder).foldLeft(List[DBObject]())((l, a) =>
          a :: l
        ).reverse
        Ok(views.html.announcements.index(announcements, announcementForm))
    }
  }

  def save = Action { implicit request =>
    announcementForm.bindFromRequest().fold(
      formWithErrors => {
        Redirect(routes.Announcements.index)
      },
      announcement => {
        DB.collection(AnnouncementsCollectionName) {
          c =>
            val builder = MongoDBObject.newBuilder
            builder += "title" -> announcement.title
            builder += "text" -> announcement.text
            builder += "url" -> announcement.url
            builder += "buttons" -> MongoDBObject("validate" -> announcement.validateButton, "cancel" -> announcement.cancelButton)
            builder += "createdAt" -> DateTime.now()
            builder += "modifiedAt" -> DateTime.now()
            c += builder.result

            Redirect(routes.Announcements.index).flashing("actionDone" -> "announcementAdded")
        }
      }
    )
  }

  def delete(id: String) = Action { implicit request =>
    DB.collection(AnnouncementsCollectionName) {
      c =>
        val oid = new ObjectId(id)
        val o = MongoDBObject("_id" -> oid)
        c.remove(o)
        Redirect(routes.Announcements.index).flashing("actionDone" -> "announcementDeleted")
    }
  }

  def edit(id: String) = Action { implicit request =>
    DB.collection(AnnouncementsCollectionName) {
      c =>
        val q = MongoDBObject("_id" -> new ObjectId(id))
        c.findOne(q).map { announcement =>
          val a = Announcement(announcement.get("title").toString, announcement.get("text").toString,
            announcement.get("url").toString, announcement.get("buttons").asInstanceOf[DBObject].get("validate").toString,
            announcement.get("buttons").asInstanceOf[DBObject].get("cancel").toString)
          Ok(views.html.announcements.edit(id, announcementForm.fill(a)))
        }.getOrElse(NotFound)
    }
  }

  def update(id: String) = Action { implicit request =>
    announcementForm.bindFromRequest.fold(
      formWithErrors => {
        Redirect(routes.Announcements.edit(id))
      },
      announcement => {
        DB.collection(AnnouncementsCollectionName) {
          c =>
            val q = MongoDBObject("_id" -> new ObjectId(id))
            val o = $set("title" -> announcement.title, "text" -> announcement.text, "url" -> announcement.url,
              "buttons" -> MongoDBObject("validate" -> announcement.validateButton, "cancel" -> announcement.cancelButton),
              "modifiedAt" -> DateTime.now())
            c.update(q, o, false, false)
            Redirect(routes.Announcements.index).flashing("actionDone" -> "announcementUpdated")
        }
      }
    )
  }

}
