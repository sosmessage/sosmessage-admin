package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import com.mongodb.casbah.query.Imports._
import com.mongodb.DBObject
import org.joda.time.DateTime
import db.DB

case class SosMessageApp(name: String, lang: String, title: String)

case class NewAnnouncement(id: String)
case class NewCategory(id: String)

object SosMessageApps extends Controller {

  val AnnouncementsCollectionName = "announcements"
  val CategoriesCollectionName = "categories"
  val MessagesCollectionName = "messages"
  val AppsCollectionName = "sosmessageapps"

  val appForm = Form(
    mapping(
      "name" -> (
        nonEmptyText
        verifying ("Should not contain '_' character.", name => { !name.contains("_") })
      ),
      "lang" -> nonEmptyText,
      "title" -> nonEmptyText
    )(SosMessageApp.apply)(SosMessageApp.unapply)
  )

  val addCategoryForm = Form(
    mapping(
      "id" -> nonEmptyText
    )(NewCategory.apply)(NewCategory.unapply)
  )

  val addAnnouncementForm = Form(
    mapping(
      "id" -> nonEmptyText
    )(NewAnnouncement.apply)(NewAnnouncement.unapply)
  )

  def index = Action { implicit request =>
    DB.collection(AppsCollectionName) {
      c =>
        val apps = c.find().foldLeft(List[DBObject]())((l, a) =>
          a :: l
        ).reverse
        Ok(views.html.apps.index(apps, appForm))
    }
  }

  def save = Action { implicit request =>
    appForm.bindFromRequest().fold(
      formWithErrors => {
        Redirect(routes.SosMessageApps.index)
      },
      app => {
        DB.collection(AppsCollectionName) {
          c =>
            val appName = app.name + "_" + app.lang
            c.findOne(MongoDBObject("name" -> appName)) match {
              case Some(app) => Redirect(routes.SosMessageApps.index).flashing("actionError" -> "appAlreadyExists")
              case None => {
                val builder = MongoDBObject.newBuilder
                builder += "name" -> appName
                builder += "title" -> app.title
                builder += "createdAt" -> DateTime.now()
                builder += "modifiedAt" -> DateTime.now()
                c += builder.result
                Redirect(routes.SosMessageApps.index).flashing("actionDone" -> "appAdded")
              }
            }
        }
      }
    )
  }

  def delete(id: String) = Action { implicit request =>
    DB.collection(AppsCollectionName) {
      appsCollection =>
        appsCollection.findOne(MongoDBObject("_id" -> new ObjectId(id))).map { app =>
          DB.collection(CategoriesCollectionName) {
            categoriesCollection =>
              val key = "apps." + app.get("name")
              val o = $unset(key)
              val q = MongoDBObject(key -> MongoDBObject("$exists" -> true))
              categoriesCollection.update(q, o, false, true)
          }
          appsCollection.remove(MongoDBObject("_id" -> new ObjectId(id)))
        }
        Redirect(routes.SosMessageApps.index).flashing("actionDone" -> "appDeleted")
    }
  }

  def categories(appId: String) = Action { implicit request =>
    DB.collection(AppsCollectionName) {
      appsCollection =>
        val q = MongoDBObject("_id" -> new ObjectId(appId))
        appsCollection.findOne(q).map { app =>
          DB.collection(CategoriesCollectionName) {
            categoriesCollection =>
              val appCategoriesQuery = MongoDBObject("apps." + app.get("name").toString -> MongoDBObject("$exists" -> true))
              val categoryOrder = MongoDBObject("apps." + app.get("name").toString + ".order" -> -1)
              val appCategories = categoriesCollection.find(appCategoriesQuery).sort(categoryOrder).foldLeft(List[DBObject]())((l, a) =>
                a :: l
              ).reverse

              val nonAppCategoriesQuery = MongoDBObject("apps." + app.get("name").toString -> MongoDBObject("$exists" -> false))
              val nonAppCategories = categoriesCollection.find(nonAppCategoriesQuery).foldLeft(List[DBObject]())((l, a) =>
                a :: l
              ).reverse

              DB.collection(MessagesCollectionName) {
                messagesCollection =>
                  val messagesCountByCategory = appCategories.foldLeft(Map[String, Long]())((m, o) => {
                    val count = messagesCollection.count(MongoDBObject("categoryId" -> o.get("_id")))
                    m + (o.get("_id").toString -> count)
                  })
                  Ok(views.html.apps.categories(app, appCategories, messagesCountByCategory, nonAppCategories, addCategoryForm))
              }
          }
        }.getOrElse(NotFound)
    }
  }

  def addCategory(appId: String) = Action { implicit request =>
    addCategoryForm.bindFromRequest().fold(
      formWithErrors => {
        Redirect(routes.SosMessageApps.categories(appId))
      },
      newCategory => {
        DB.collection(AppsCollectionName) {
          appsCollection =>
            appsCollection.findOne(MongoDBObject("_id" -> new ObjectId(appId))).map { app =>
              DB.collection(CategoriesCollectionName) {
                categoriesCollection =>
                  val appCategoriesQuery = MongoDBObject("apps." + app.get("name").toString -> MongoDBObject("$exists" -> true))
                  val appCategories = categoriesCollection.find(appCategoriesQuery)

                  val q = MongoDBObject("_id" -> new ObjectId(newCategory.id))
                  val key = "apps." + app.get("name")
                  val o = $set(key -> MongoDBObject("published" -> false, "order" -> appCategories.count), "modifiedAt" -> DateTime.now())
                  categoriesCollection.update(q, o, false, false)
                  Redirect(routes.SosMessageApps.categories(appId)).flashing("actionDone" -> "categoryAdded")
              }
            }
            Redirect(routes.SosMessageApps.categories(appId))
        }
      }
    )
  }

  def removeCategory(appId: String, categoryId: String) = Action { implicit request =>
    DB.collection(AppsCollectionName) {
      appsCollection =>
        appsCollection.findOne(MongoDBObject("_id" -> new ObjectId(appId))).map { app =>
          DB.collection(CategoriesCollectionName) {
            categoriesCollection =>
              val q = MongoDBObject("_id" -> new ObjectId(categoryId))
              val key = "apps." + app.get("name")
              val o = $unset(key)
              categoriesCollection.update(q, o, false, false)
              Redirect(routes.SosMessageApps.categories(appId)).flashing("actionDone" -> "categoryRemoved")
          }
        }
        Redirect(routes.SosMessageApps.categories(appId))
    }
  }

  def publishCategory(appId: String, categoryId: String) = Action { implicit request =>
    DB.collection(AppsCollectionName) {
      appsCollection =>
        val q = MongoDBObject("_id" -> new ObjectId(appId))
        appsCollection.findOne(q).map { app =>
          DB.collection(CategoriesCollectionName) {
            categoriesCollection =>
              val key = "apps." + app.get("name") + ".published"
              val o = $set(key -> true, "modifiedAt" -> DateTime.now())
              val q = MongoDBObject("_id" -> new ObjectId(categoryId))
              categoriesCollection.update(q, o, false, false)
          }
        }
        Redirect(routes.SosMessageApps.categories(appId)).flashing("actionDone" -> "categoryUpdated")
    }
  }

  def unpublishCategory(appId: String, categoryId: String) = Action { implicit request =>
    DB.collection(AppsCollectionName) {
      appsCollection =>
        val q = MongoDBObject("_id" -> new ObjectId(appId))
        appsCollection.findOne(q).map { app =>
          DB.collection(CategoriesCollectionName) {
            categoriesCollection =>
              val key = "apps." + app.get("name") + ".published"
              val o = $set(key -> false, "modifiedAt" -> DateTime.now())
              val q = MongoDBObject("_id" -> new ObjectId(categoryId))
              categoriesCollection.update(q, o, false, false)
          }
        }
        Redirect(routes.SosMessageApps.categories(appId)).flashing("actionDone" -> "categoryUpdated")
    }
  }

  def moveCategoryUp(appId: String, categoryId: String) = Action { implicit request =>
    DB.collection(AppsCollectionName) {
      appsCollection =>
        val q = MongoDBObject("_id" -> new ObjectId(appId))
        appsCollection.findOne(q).map { app =>
          DB.collection(CategoriesCollectionName) {
            categoriesCollection =>
              val appCategoriesQuery = MongoDBObject("apps." + app.get("name").toString -> MongoDBObject("$exists" -> true))
              val categoryOrder = MongoDBObject("apps." + app.get("name").toString + ".order" -> -1)
              val appCategories = categoriesCollection.find(appCategoriesQuery).sort(categoryOrder).foldLeft(List[DBObject]())((l, a) =>
                a :: l
              ).reverse

              appCategories.find(o => categoryId == o.get("_id").toString).map { selectedCategory =>
                val index = appCategories.indexOf(selectedCategory)
                if (index > 0) {
                  val key = "apps." + app.get("name").toString + ".order"
                  var q = MongoDBObject("_id" -> selectedCategory.get("_id"))
                  var o = $inc(key -> 1)
                  categoriesCollection.update(q, o, false, false)

                  var categoryBefore = appCategories(index - 1)
                  q = MongoDBObject("_id" -> categoryBefore.get("_id"))
                  o = $inc(key -> -1)
                  categoriesCollection.update(q, o, false, false)
                }
              }
          }
        }
        Redirect(routes.SosMessageApps.categories(appId)).flashing("actionDone" -> "categoryUpdated")
    }
  }

  def moveCategoryDown(appId: String, categoryId: String) = Action { implicit request =>
    DB.collection(AppsCollectionName) {
      appsCollection =>
        val q = MongoDBObject("_id" -> new ObjectId(appId))
        appsCollection.findOne(q).map { app =>
          DB.collection(CategoriesCollectionName) {
            categoriesCollection =>
              val appCategoriesQuery = MongoDBObject("apps." + app.get("name").toString -> MongoDBObject("$exists" -> true))
              val categoryOrder = MongoDBObject("apps." + app.get("name").toString + ".order" -> -1)
              val appCategories = categoriesCollection.find(appCategoriesQuery).sort(categoryOrder).foldLeft(List[DBObject]())((l, a) =>
                a :: l
              ).reverse

              appCategories.find(o => categoryId == o.get("_id").toString).map { selectedCategory =>
                val index = appCategories.indexOf(selectedCategory)
                if (index < appCategories.size - 1) {
                  val key = "apps." + app.get("name").toString + ".order"
                  var q = MongoDBObject("_id" -> selectedCategory.get("_id"))
                  var o = $inc(key -> -1)
                  categoriesCollection.update(q, o, false, false)

                  var categoryAfter = appCategories(index + 1)
                  q = MongoDBObject("_id" -> categoryAfter.get("_id"))
                  o = $inc(key -> 1)
                  categoriesCollection.update(q, o, false, false)
                }
              }
          }
        }
        Redirect(routes.SosMessageApps.categories(appId)).flashing("actionDone" -> "categoryUpdated")
    }
  }

  def announcements(appId: String) = Action { implicit request =>
    DB.collection(AppsCollectionName) {
      appsCollection =>
        val q = MongoDBObject("_id" -> new ObjectId(appId))
        appsCollection.findOne(q).map { app =>
          DB.collection(AnnouncementsCollectionName) {
            announcementsCollection =>
              val appAnnouncementsQuery = MongoDBObject("apps." + app.get("name").toString -> MongoDBObject("$exists" -> true))
              val announcementOrder = MongoDBObject("apps." + app.get("name").toString + ".order" -> -1)
              val appAnnouncements = announcementsCollection.find(appAnnouncementsQuery).sort(announcementOrder).foldLeft(List[DBObject]())((l, a) =>
                a :: l
              ).reverse

              val nonAppAnnouncementsQuery = MongoDBObject("apps." + app.get("name").toString -> MongoDBObject("$exists" -> false))
              val nonAppAnnouncements = announcementsCollection.find(nonAppAnnouncementsQuery).foldLeft(List[DBObject]())((l, a) =>
                a :: l
              ).reverse

              Ok(views.html.apps.announcements(app, appAnnouncements, nonAppAnnouncements, addAnnouncementForm))
          }
        }.getOrElse(NotFound)
    }
  }

  def addAnnouncement(appId: String) = Action { implicit request =>
    addAnnouncementForm.bindFromRequest().fold(
      formWithErrors => {
        Redirect(routes.SosMessageApps.announcements(appId))
      },
      newAnnouncement => {
        DB.collection(AppsCollectionName) {
          appsCollection =>
            appsCollection.findOne(MongoDBObject("_id" -> new ObjectId(appId))).map { app =>
              DB.collection(AnnouncementsCollectionName) {
                announcementsCollection =>
                  val q = MongoDBObject("_id" -> new ObjectId(newAnnouncement.id))
                  val key = "apps." + app.get("name")
                  val o = $set(key -> MongoDBObject("published" -> false), "modifiedAt" -> DateTime.now())
                  announcementsCollection.update(q, o, false, false)
                  Redirect(routes.SosMessageApps.announcements(appId)).flashing("actionDone" -> "announcementAdded")
              }
            }
            Redirect(routes.SosMessageApps.announcements(appId))
        }
      }
    )
  }

  def removeAnnouncement(appId: String, announcementId: String) = Action { implicit request =>
    DB.collection(AppsCollectionName) {
      appsCollection =>
        appsCollection.findOne(MongoDBObject("_id" -> new ObjectId(appId))).map { app =>
          DB.collection(AnnouncementsCollectionName) {
            announcementsCollection =>
              val q = MongoDBObject("_id" -> new ObjectId(announcementId))
              val key = "apps." + app.get("name")
              val o = $unset(key)
              announcementsCollection.update(q, o, false, false)
              Redirect(routes.SosMessageApps.announcements(appId)).flashing("actionDone" -> "announcementRemoved")
          }
        }
        Redirect(routes.SosMessageApps.announcements(appId))
    }
  }

  def publishAnnouncement(appId: String, announcementId: String) = Action { implicit request =>
    DB.collection(AppsCollectionName) {
      appsCollection =>
        val q = MongoDBObject("_id" -> new ObjectId(appId))
        appsCollection.findOne(q).map { app =>
          DB.collection(AnnouncementsCollectionName) {
            announcementsCollection =>
              val key = "apps." + app.get("name") + ".published"
              val o = $set(key -> true, "modifiedAt" -> DateTime.now())
              val q = MongoDBObject("_id" -> new ObjectId(announcementId))
              announcementsCollection.update(q, o, false, false)
          }
        }
        Redirect(routes.SosMessageApps.announcements(appId)).flashing("actionDone" -> "announcementUpdated")
    }
  }

  def unpublishAnnouncement(appId: String, announcementId: String) = Action { implicit request =>
    DB.collection(AppsCollectionName) {
      appsCollection =>
        val q = MongoDBObject("_id" -> new ObjectId(appId))
        appsCollection.findOne(q).map { app =>
          DB.collection(AnnouncementsCollectionName) {
            announcementsCollection =>
              val key = "apps." + app.get("name") + ".published"
              val o = $set(key -> false, "modifiedAt" -> DateTime.now())
              val q = MongoDBObject("_id" -> new ObjectId(announcementId))
              announcementsCollection.update(q, o, false, false)
          }
        }
        Redirect(routes.SosMessageApps.announcements(appId)).flashing("actionDone" -> "announcementUpdated")
    }
  }
}
