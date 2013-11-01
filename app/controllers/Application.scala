package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.Routes
import com.mongodb.casbah.commons.conversions.scala._
import com.mongodb.casbah.query.Imports._
import db.DB
import com.mongodb.casbah.commons.conversions.scala._
import java.security.MessageDigest
import http._

object Application extends SosMessageController with Secured {

  RegisterJodaTimeConversionHelpers()

  def index = Open { implicit ctx =>
    Ok(views.html.index())
  }

  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    ) verifying ("Invalid email or password", result => result match {
      case (email, password) => {
        DB.collection("users") {
          c =>
            val q = MongoDBObject("email" -> email)
            c.findOne(q).map { user =>
              val md5Password = user.get("password").toString
              val salt = user.get("salt").toString
              md5Password == computeMD5Password(password, salt)
            }.getOrElse(false)
        }
      }
    })
  )

  private def computeMD5Password(password: String, salt: String) = {
    val md = MessageDigest.getInstance("MD5")
    val s = password + salt
    md.digest(s.getBytes("UTF-8")).map(0xFF & _).map { "%02x".format(_) }.mkString("")
  }

  /**
   * Login page.
   */
  def login = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  /**
   * Handle login form submission.
   */
  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors)),
      user => Redirect(routes.Application.index).withSession("email" -> user._1)
    )
  }

  /**
   * Logout and clean the session.
   */
  def logout = Action {
    Redirect(routes.Application.index).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

  def javascriptRoutes = Action { implicit request =>
    import routes.javascript._
    Ok(Routes.javascriptRouter("jsRoutes")
      (Statistics.requestsStats, Statistics.iosRequestsStats, Statistics.androidRequestsStats, Statistics.usersStats,
        Statistics.appsStats, Statistics.iosAppsStats, Statistics.androidAppsStats,
        Statistics.randomMessagesStats, Statistics.bestMessagesStats, Statistics.votedMessagesStats, Statistics.contributedMessagesStats)
    ).as("text/javascript")
  }

  def ping = Open { implicit ctx =>
    Ok("pong")
  }

}
