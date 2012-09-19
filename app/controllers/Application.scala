package controllers

import play.api.mvc._
import play.api.Routes
import com.mongodb.casbah.commons.conversions.scala._

object Application extends Controller {

  RegisterJodaTimeConversionHelpers()

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def javascriptRoutes = Action { implicit request =>
    import routes.javascript._
    Ok(Routes.javascriptRouter("jsRoutes")
      (Statistics.requestsStats, Statistics.usersStats, Statistics.appsStats)
    ).as("text/javascript")
  }


}
