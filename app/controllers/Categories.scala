package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import com.mongodb.DBObject
import com.mongodb.casbah.query.Imports._
import db.DB
import com.mongodb.casbah.commons.conversions.scala._
import org.joda.time.DateTime

case class Category(name: String, color: String)

object Categories extends SosMessageController {

  val CategoriesCollectionName = "categories"
  val MessagesCollectionName = "messages"

  val categoryForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "color" -> text(minLength = 9)
    )(Category.apply)(Category.unapply)
  )

  def index = Auth { implicit ctx => _ =>
    DB.collection(CategoriesCollectionName) {
      categoriesCollection =>
        val categoryOrder = MongoDBObject("name" -> 1)
        val categories = categoriesCollection.find().sort(categoryOrder).foldLeft(List[DBObject]())((l, a) =>
          a :: l
        ).reverse

        DB.collection(MessagesCollectionName) {
          messagesCollection =>
            val messagesCountByCategory = categories.foldLeft(Map[String, Long]())((m, o) => {
              val count = messagesCollection.count(MongoDBObject("categoryId" -> o.get("_id"), "state" -> "approved"))
              m + (o.get("_id").toString -> count)
            })

            Ok(views.html.categories.index(categories, messagesCountByCategory, categoryForm))
        }
    }
  }

  def save = Auth { implicit ctx => _ =>
    implicit val req = ctx.req
    categoryForm.bindFromRequest().fold(
      formWithErrors => {
        Redirect(routes.Categories.index)
      },
      category => {
        DB.collection(CategoriesCollectionName) {
          c =>
            val builder = MongoDBObject.newBuilder
            builder += "name" -> category.name
            val color = if (category.color.startsWith("#")) category.color else "#" + category.color
            builder += "color" -> color
            builder += "createdAt" -> DateTime.now()
            builder += "modifiedAt" -> DateTime.now()
            builder += "lastAddedMessageAt" -> DateTime.now()
            c += builder.result

            Redirect(routes.Categories.index).flashing("actionDone" -> "categoryAdded")
        }
      }
    )
  }

  def delete(id: String) = Auth { implicit ctx => _ =>
    DB.collection(CategoriesCollectionName) {
      c =>
        val oid = new ObjectId(id)
        val o = MongoDBObject("_id" -> oid)
        c.remove(o)
        Redirect(routes.Categories.index).flashing("actionDone" -> "categoryDeleted")
    }
  }

  def edit(id: String) = Auth { implicit ctx => _ =>
    DB.collection(CategoriesCollectionName) {
      c =>
        val q = MongoDBObject("_id" -> new ObjectId(id))
        c.findOne(q).map { category =>
          val c = Category(category.get("name").toString, category.get("color").toString)
          Ok(views.html.categories.edit(id, categoryForm.fill(c)))
        }.getOrElse(NotFound)
    }
  }

  def update(id: String) = Auth { implicit ctx => _ =>
    implicit val req = ctx.req
    categoryForm.bindFromRequest.fold(
      formWithErrors => {
        Redirect(routes.Categories.edit(id))
      },
      category => {
        DB.collection(CategoriesCollectionName) {
          c =>
            val q = MongoDBObject("_id" -> new ObjectId(id))
            val color = if (category.color.startsWith("#")) category.color else "#" + category.color
            val o = $set("name" -> category.name, "color" -> color, "modifiedAt" -> DateTime.now())
            c.update(q, o, false, false)
            Redirect(routes.Categories.index).flashing("actionDone" -> "categoryUpdated")
        }
      }
    )
  }

}
