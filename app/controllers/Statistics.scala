package controllers

import play.api.mvc._
import db.DB
import org.joda.time.DateTime
import com.mongodb.casbah.query.Imports._
import com.mongodb.casbah.commons.conversions.scala._

object Statistics extends SosMessageController {

  val EventLogsCollectionName = "eventlogs"
  val CategoriesCollectionName = "categories"

  def index = Auth { implicit ctx => _ =>
      Ok(views.html.stats.index())
  }

  def requestsStats = Auth { implicit ctx => _ =>
      DB.collection(EventLogsCollectionName) {
        c =>
          val now = DateTime.now()
          val oneHourBefore = ("createdAt" $gte now.minusHours(1) $lte now) ++
            ("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""))
          val oneDayBefore = ("createdAt" $gte now.minusDays(1) $lte now) ++
            ("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""))
          val oneWeekBefore = ("createdAt" $gte now.minusWeeks(1) $lte now) ++
            ("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""))
          val oneMonthBefore = ("createdAt" $gte now.minusMonths(1) $lte now) ++
            ("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""))
          val oneYearBefore = ("createdAt" $gte now.minusYears(1) $lte now) ++
            ("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""))

          val lastHourRequestsCount = c.count(oneHourBefore)
          val lastDayRequestsCount = c.count(oneDayBefore)
          val lastWeekRequestsCount = c.count(oneWeekBefore)
          val lastMonthRequestsCount = c.count(oneMonthBefore)
          val lastYearRequestsCount = c.count(oneYearBefore)

          Ok(views.html.stats.requests(lastHourRequestsCount, lastDayRequestsCount, lastWeekRequestsCount,
            lastMonthRequestsCount, lastYearRequestsCount))
      }
  }

  def usersStats = Auth { implicit ctx => _ =>
      DB.collection(EventLogsCollectionName) {
        c =>
          val uniqueUsers = c.distinct("uid",
            MongoDBObject("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""))).size
          Ok(views.html.stats.users(uniqueUsers))
      }
  }

  def appsStats = Auth { implicit ctx => _ =>
      DB.collection(EventLogsCollectionName) {
        c =>
          val totalCount = c.count(MongoDBObject("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""),
            "appName" -> MongoDBObject("$exists" -> true, "$ne" -> "")))
          val reduce = """function(obj, prev) { prev.csum += 1; }"""
          val appsCount = c.group(MongoDBObject("appName" -> true),
            MongoDBObject("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""),
            "appName" -> MongoDBObject("$exists" -> true, "$ne" -> "")),
            MongoDBObject( "csum" -> 0 ), reduce).toSeq
            .sortBy(_.get("csum").asInstanceOf[Double]).reverse
          Ok(views.html.stats.apps(totalCount, appsCount))
      }
  }

  def randomMessagesStats = Auth { implicit ctx => _ =>
      DB.collection(CategoriesCollectionName) {
        categoriesCollection =>
          DB.collection(EventLogsCollectionName) {
            c =>
              val totalCount = c.count(MongoDBObject("action" -> "getRandomMessage",
                "uid" -> MongoDBObject("$exists" -> true, "$ne" -> "")))
              val reduce = """function(obj, prev) { prev.csum += 1; }"""
              val messagesPerCategory = c.group(MongoDBObject("targetObject" -> true),
                MongoDBObject("action" -> "getRandomMessage",
                  "uid" -> MongoDBObject("$exists" -> true, "$ne" -> "")),
                MongoDBObject( "csum" -> 0 ), reduce).toSeq
                .sortBy(_.get("csum").asInstanceOf[Double]).reverse.map(o => {
                    val q = MongoDBObject("_id" -> new ObjectId(o.get("targetObject").toString))
                    categoriesCollection.findOne(q) map {
                      category =>
                        val builder = MongoDBObject.newBuilder
                        builder += ("name" -> category.get("name").toString)
                        o.putAll(builder.result)
                    }
                    o
                  })
                Ok(views.html.stats.messages(totalCount, messagesPerCategory))
          }
      }
  }

  def bestMessagesStats = Auth { implicit ctx => _ =>
      DB.collection(CategoriesCollectionName) {
        categoriesCollection =>
          DB.collection(EventLogsCollectionName) {
            c =>
              val totalCount = c.count(MongoDBObject("action" -> "getBestMessages",
                "uid" -> MongoDBObject("$exists" -> true, "$ne" -> "")))
              val reduce = """function(obj, prev) { prev.csum += 1; }"""
              val bestMessagesPerCategory = c.group(MongoDBObject("targetObject" -> true),
                MongoDBObject("action" -> "getBestMessages",
                  "uid" -> MongoDBObject("$exists" -> true, "$ne" -> "")),
                MongoDBObject( "csum" -> 0 ), reduce).toSeq
                .sortBy(_.get("csum").asInstanceOf[Double]).reverse.map(o => {
                    val q = MongoDBObject("_id" -> new ObjectId(o.get("targetObject").toString))
                    categoriesCollection.findOne(q) map {
                      category =>
                        val builder = MongoDBObject.newBuilder
                        builder += ("name" -> category.get("name").toString)
                        o.putAll(builder.result)
                    }
                    o
                  })
                Ok(views.html.stats.messages(totalCount, bestMessagesPerCategory))
          }
      }
  }

  def votedMessagesStats = Auth { implicit ctx => _ =>
      DB.collection(EventLogsCollectionName) {
        c =>
          val totalCount = c.count(MongoDBObject("action" -> "voteMessage",
            "uid" -> MongoDBObject("$exists" -> true, "$ne" -> "")))
          Ok(views.html.stats.messages(totalCount, Seq()))
      }
  }

  def contributedMessagesStats = Auth { implicit ctx => _ =>
      DB.collection(CategoriesCollectionName) {
        categoriesCollection =>
          DB.collection(EventLogsCollectionName) {
            c =>
              val q = MongoDBObject("action" -> "postMessage",
                "uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""))
              val totalCount = c.count(q)
              val reduce = """function(obj, prev) { prev.csum += 1; }"""
              val contributedMessagesPerCategory = c.group(MongoDBObject("targetObject" -> true),
                q,
                MongoDBObject( "csum" -> 0 ), reduce).toSeq
                .sortBy(_.get("csum").asInstanceOf[Double]).reverse.map(o => {
                    val q = MongoDBObject("_id" -> new ObjectId(o.get("targetObject").toString))
                    categoriesCollection.findOne(q) map {
                      category =>
                        val builder = MongoDBObject.newBuilder
                        builder += ("name" -> category.get("name").toString)
                        o.putAll(builder.result)
                    }
                    o
                  })
                Ok(views.html.stats.messages(totalCount, contributedMessagesPerCategory))
          }
      }
  }

}
