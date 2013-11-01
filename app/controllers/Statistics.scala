package controllers

import play.api.mvc._
import db.DB
import org.joda.time.DateTime
import com.mongodb.casbah.query.Imports._
import com.mongodb.casbah.commons.conversions.scala._

object Statistics extends SosMessageController {

  val EventLogsCollectionName = "eventlogs"
  val CategoriesCollectionName = "categories"
  val MessagesCollectionName = "messages"

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

  def iosRequestsStats = Auth { implicit ctx => _ =>
      DB.collection(EventLogsCollectionName) {
        c =>
          val now = DateTime.now()
          val oneHourBefore = ("createdAt" $gte now.minusHours(1) $lte now) ++
            ("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""), "appOs" -> "ios")
          val oneDayBefore = ("createdAt" $gte now.minusDays(1) $lte now) ++
            ("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""), "appOs" -> "ios")
          val oneWeekBefore = ("createdAt" $gte now.minusWeeks(1) $lte now) ++
            ("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""), "appOs" -> "ios")
          val oneMonthBefore = ("createdAt" $gte now.minusMonths(1) $lte now) ++
            ("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""), "appOs" -> "ios")
          val oneYearBefore = ("createdAt" $gte now.minusYears(1) $lte now) ++
            ("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""), "appOs" -> "ios")

          val lastHourRequestsCount = c.count(oneHourBefore)
          val lastDayRequestsCount = c.count(oneDayBefore)
          val lastWeekRequestsCount = c.count(oneWeekBefore)
          val lastMonthRequestsCount = c.count(oneMonthBefore)
          val lastYearRequestsCount = c.count(oneYearBefore)

          Ok(views.html.stats.requests(lastHourRequestsCount, lastDayRequestsCount, lastWeekRequestsCount,
            lastMonthRequestsCount, lastYearRequestsCount))
      }
  }

  def androidRequestsStats = Auth { implicit ctx => _ =>
      DB.collection(EventLogsCollectionName) {
        c =>
          val now = DateTime.now()
          val oneHourBefore = ("createdAt" $gte now.minusHours(1) $lte now) ++
            ("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""), "appOs" -> "android")
          val oneDayBefore = ("createdAt" $gte now.minusDays(1) $lte now) ++
            ("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""), "appOs" -> "android")
          val oneWeekBefore = ("createdAt" $gte now.minusWeeks(1) $lte now) ++
            ("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""), "appOs" -> "android")
          val oneMonthBefore = ("createdAt" $gte now.minusMonths(1) $lte now) ++
            ("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""), "appOs" -> "android")
          val oneYearBefore = ("createdAt" $gte now.minusYears(1) $lte now) ++
            ("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""), "appOs" -> "android")

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

          val reduce = """function(obj, prev) {
            if (!prev.uids) { prev.uids = []; }
            if (prev.uids.indexOf(obj.uid) == -1) {
              prev.uids.push(obj.uid);
              prev.csum += 1;
            }
          }"""
          val usersCount = c.group(MongoDBObject("appOs" -> true),
            MongoDBObject("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""),
            "appOs" -> MongoDBObject("$exists" -> true, "$ne" -> "")),
             MongoDBObject( "csum" -> 0 ), reduce).toSeq
            .sortBy(_.get("csum").asInstanceOf[Double]).reverse

          Ok(views.html.stats.users(uniqueUsers, usersCount))
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

  def iosAppsStats = Auth { implicit ctx => _ =>
      DB.collection(EventLogsCollectionName) {
        c =>
          val totalCount = c.count(MongoDBObject("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""),
            "appOs" -> "ios",
            "appName" -> MongoDBObject("$exists" -> true, "$ne" -> "")))
          val reduce = """function(obj, prev) { prev.csum += 1; }"""
          val appsCount = c.group(MongoDBObject("appName" -> true),
            MongoDBObject("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""),
            "appOs" -> "ios",
            "appName" -> MongoDBObject("$exists" -> true, "$ne" -> "")),
            MongoDBObject( "csum" -> 0 ), reduce).toSeq
            .sortBy(_.get("csum").asInstanceOf[Double]).reverse
          Ok(views.html.stats.apps(totalCount, appsCount))
      }
  }

  def androidAppsStats = Auth { implicit ctx => _ =>
      DB.collection(EventLogsCollectionName) {
        c =>
          val totalCount = c.count(MongoDBObject("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""),
            "appOs" -> "android",
            "appName" -> MongoDBObject("$exists" -> true, "$ne" -> "")))
          val reduce = """function(obj, prev) { prev.csum += 1; }"""
          val appsCount = c.group(MongoDBObject("appName" -> true),
            MongoDBObject("uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""),
            "appOs" -> "android",
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
    DB.collection(MessagesCollectionName) {
      messagesCollection =>
        DB.collection(CategoriesCollectionName) {
          categoriesCollection =>
            DB.collection(EventLogsCollectionName) {
              c =>
                val totalCount = c.count(MongoDBObject("action" -> "voteMessage",
                  "uid" -> MongoDBObject("$exists" -> true, "$ne" -> "")))

                var countPerCategory = Map[String, Double]()
                c.find(MongoDBObject("action" -> "voteMessage", "uid" -> MongoDBObject("$exists" -> true, "$ne" -> ""))).toSeq.foreach(
                  o => {
                    val categoryId = messagesCollection.findOne(MongoDBObject("_id" -> new ObjectId(o.get("targetObject").toString)))
                      .get("categoryId").toString
                    val count = countPerCategory.getOrElse(categoryId, 0.0).asInstanceOf[Long]
                    countPerCategory += (categoryId -> (count + 1))
                  })

                val votedMessagesPerCategory = countPerCategory.toSeq.map(o => {
                  val q = MongoDBObject("_id" -> new ObjectId(o._1))
                  categoriesCollection.findOne(q) map {
                    category =>
                      val builder = MongoDBObject.newBuilder
                      builder += ("name" -> category.get("name").toString)
                      builder += "csum" -> o._2
                      builder.result
                  }
                })

                Ok(views.html.stats.messages(totalCount,
                  votedMessagesPerCategory.map(o => o.get)
                    .sortWith(_.get("csum").asInstanceOf[Double] > _.get("csum").asInstanceOf[Double])))
            }
        }
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
