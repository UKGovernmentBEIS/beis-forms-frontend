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
import models.{ActivityId, ExecutionId, ProcessDefinition, ProcessDefinitionId, ProcessId, ProcessInstanceId, Action}
import play.api.libs.ws.WSClient
import play.api.libs.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.{ExecutionContext, Future}
import java.util.Base64

/**
  * Created by venkatamutyala on 05/06/2017.
  */


class ProcessURLs(url: String) {

  /** BPM Activiti URLs **/
  def getProcess(id: ProcessDefinitionId) =
      s"$url/service/runtime/get-process-instances"
  def activateProcess =
    s"$url/service/runtime/process-instances"
  def execution (id: ProcessDefinitionId, activityId: ActivityId) =
    s"$url/service/runtime/executions?processDefinitionId=${id.id}&activityId=${activityId.id}"
  def signal (id: ExecutionId) =
    s"$url/service/runtime/executions/${id.id}"

}

class BusinessProcessService @Inject()(val ws: WSClient)(implicit val ec: ExecutionContext)
    extends BusinessProcessOps with RestService {

  val baseUrl = Config.config.business.bpmServerUrl
  val bpmServerContext = Config.config.business.bpmServerContext

   val urls = new ProcessURLs(baseUrl + bpmServerContext)

   private val basicAuth = {
     "Basic " + new String(Base64.getEncoder.encode((Config.config.bpm.procuser + ":" + Config.config.bpm.procpwd).getBytes))
   }

   override def activateProcess(id: ProcessDefinitionId, pd: ProcessDefinition): Future[Option[ProcessInstanceId]] =
      postOptWithAuthHeaderUpdate[ProcessInstanceId, ProcessDefinition](urls.activateProcess, basicAuth, pd)

  override def getExecution(id: ProcessDefinitionId, activityId: ActivityId): Future[Option[ExecutionId]]=
    getOptWithAuthHeaderUpdate[ExecutionId](urls.execution(id, activityId), basicAuth)

  def sendSignal(id: ExecutionId, action: Action): Future[Option[ExecutionId]]=
    putOptWithAuthHeaderUpdate[ExecutionId, Action](urls.signal(id), basicAuth, action)
}
