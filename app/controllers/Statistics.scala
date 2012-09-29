package controllers

import play.api.mvc._
import db.DB
import org.joda.time.DateTime
import com.mongodb.casbah.query.Imports._
import com.mongodb.casbah.commons.conversions.scala._

object Statistics extends Controller with Secured {

  val EventLogsCollectionName = "eventlogs"

  def index = IsAuthenticated { _ =>
    implicit request =>
      Ok(views.html.stats.index())
  }

  def requestsStats = IsAuthenticated { _ =>
    implicit request =>
      DB.collection(EventLogsCollectionName) {
        c =>
          val now = DateTime.now()
          val oneHourBefore = "createdAt" $gte now.minusHours(1) $lte now
          val oneDayBefore = "createdAt" $gte now.minusDays(1) $lte now
          val oneWeekBefore = "createdAt" $gte now.minusWeeks(1) $lte now
          val oneMonthBefore = "createdAt" $gte now.minusMonths(1) $lte now
          val oneYearBefore = "createdAt" $gte now.minusYears(1) $lte now

          val lastHourRequestsCount = c.count(oneHourBefore)
          val lastDayRequestsCount = c.count(oneDayBefore)
          val lastWeekRequestsCount = c.count(oneWeekBefore)
          val lastMonthRequestsCount = c.count(oneMonthBefore)
          val lastYearRequestsCount = c.count(oneYearBefore)

          Ok(views.html.stats.requests(lastHourRequestsCount, lastDayRequestsCount, lastWeekRequestsCount,
            lastMonthRequestsCount, lastYearRequestsCount))
      }
  }

  def usersStats = IsAuthenticated { _ =>
    implicit request =>
      DB.collection(EventLogsCollectionName) {
        c =>
          val uniqueUsers = c.distinct("uid").size
          Ok(views.html.stats.users(uniqueUsers))
      }
  }

  def appsStats = IsAuthenticated { _ =>
    implicit request =>
      DB.collection(EventLogsCollectionName) {
        c =>
          val totalCount = c.count(MongoDBObject())
          val reduce = """function(obj, prev) { prev.csum += 1; }"""
          val appsCount = c.group(MongoDBObject("appName" -> true), MongoDBObject(),
            MongoDBObject( "csum" -> 0 ), reduce).toSeq
            .sortBy(_.get("csum").asInstanceOf[Double]).reverse
          Ok(views.html.stats.apps(totalCount, appsCount))
      }
  }

}
