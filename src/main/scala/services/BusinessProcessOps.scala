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

import com.google.inject.ImplementedBy
import controllers.FieldCheckHelpers.FieldErrors
import models._
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsObject, Reads}
import play.api.mvc.MultipartFormData

import scala.concurrent.Future

@ImplementedBy(classOf[BusinessProcessService])
trait BusinessProcessOps {

  //def byId(id: ProcessId): Future[Option[Process]]
  def activateProcess(id: ProcessDefinitionId, doc: ProcessDefinition): Future[Option[ProcessInstanceId]]
  def getExecution(id: ProcessDefinitionId, activityId: ActivityId): Future[Option[ExecutionId]]
  def sendSignal(id: ExecutionId, action: Action): Future[Option[ExecutionId]]
}
