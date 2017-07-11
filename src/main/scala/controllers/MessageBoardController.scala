/*
 * Copyright (C) 2016  Department for Business, Energy and Industrial Strategy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package controllers

import javax.inject.Inject

import models._
import play.api.libs.json.JsArray
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, Controller}
import services.{ApplicationOps, MessageBoardOps, OpportunityOps}

import scala.concurrent.{ExecutionContext, Future}

class MessageBoardController @Inject()(messages: MessageBoardOps)(implicit ec: ExecutionContext)
  extends Controller with ApplicationResults {

  def byId(id:MessageId) = Action.async { implicit request =>
    messages.byId(id).flatMap{
      case Some(msg) => Future(Ok(views.html.message(msg)))
      case None => Future.successful(NotFound)
    }
  }

  def delete(id:MessageId) = Action.async {
    messages.delete(id).flatMap { _ =>
      Future.successful(Redirect(controllers.routes.DashBoardController.applicantDashBoard()))
    }
  }

}
