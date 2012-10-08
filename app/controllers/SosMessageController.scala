package controllers

import play.api.http._
import play.api.mvc._
import http._

trait SosMessageController extends Controller with Secured {

  protected def Open(f: Context => Result): Action[AnyContent] = {
    Action(req => f(reqToCtx(req)))
  }

  protected def Auth(f: Context => String => Result): Action[AnyContent] = {
    Action(req => {
      val ctx = reqToCtx(req)
      ctx.username.map(username => f(ctx)(username)).getOrElse(onUnauthorized(ctx.req))
    })
  }

  protected def reqToCtx(implicit req: Request[_]): Context = {
    Context(req, username)
  }

}

/**
 * Provide security features
 */
trait Secured {

  /**
   * Retrieve the connected user email.
   */
  protected def username(implicit req: RequestHeader) = req.session.get("email")

  /**
   * Redirect to login if the user in not authorized.
   */
  protected def onUnauthorized(implicit req: RequestHeader): PlainResult = Results.Redirect(routes.Application.login)

}