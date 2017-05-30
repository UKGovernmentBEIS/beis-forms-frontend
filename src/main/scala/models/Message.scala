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

package models

import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.JsObject

/**
  * Created by venkatamutyala on 25/05/2017.
  */
case class Message(id: MessageId, userId: Option[UserId], applicationId: Option[ApplicationId], sectionNumber: Option[Int], sentBy: UserId, sentAt: DateTime, message : Option[String])

case class MessageId(id: LongId)
