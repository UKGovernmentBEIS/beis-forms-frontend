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

package services

import com.google.inject.Inject
import config.Config
import controllers.FieldCheckHelpers.FieldErrors
import controllers.{FieldCheck, FieldCheckHelpers, FieldChecks}
import forms.validation.{CostSectionValidator, FieldError}
import models._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import com.google.inject.Inject

import scala.concurrent.{ExecutionContext, Future}



class MessageBoardURLs(baseUrl: String) {

  def messages : String = s"$baseUrl/messageboard"
  def messagesById(id: MessageId): String = s"$baseUrl/message/${id.id}/messageboard"
  def delete(id: MessageId): String = s"$baseUrl/message/${id.id}/messageboard"
}

class MessageBoardService @Inject()(val ws: WSClient)(implicit val ec: ExecutionContext)
  extends MessageBoardOps with RestService {

  val baseUrl = Config.config.business.baseUrl
  val urls = new MessageBoardURLs(baseUrl)

  override def byId(id: MessageId) : Future[Option[Message]] =
    getOpt[Message](urls.messagesById(id))

  override def delete(id: MessageId): Future[Unit] =
    delete(urls.messagesById(id))

  override def byUserId(userId: UserId): Future[Seq[Message]] = {
    getWithHeaderUpdate[Seq[Message], String](urls.messages, userId.id).flatMap(msgs =>
      Future.successful(msgs.getOrElse(Seq())))
  }
}