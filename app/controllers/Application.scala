package controllers

import play.api.mvc._
import play.api.Routes

object Application extends Controller {

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
