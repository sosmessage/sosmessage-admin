package http

import play.api.mvc.{ Request, RequestHeader }

sealed class Context(val req: Request[_], val username: Option[String]) {

  def isAuthenticated = username.isDefined

  def isAnonymous = !isAuthenticated
}

object Context {

  def apply(req: Request[_], username: Option[String]): Context =
    new Context(req, username)

}
