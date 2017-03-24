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

import java.util
import javax.inject.Inject

import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity
import org.activiti.engine.repository.ProcessDefinition
import org.activiti.engine.task.Task
import play.api.mvc.{Action, Controller}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller, MultipartFormData, Result}
import play.api.mvc.Security
import org.activiti.engine.{ProcessEngine, ProcessEngines}
import org.h2.jdbcx.JdbcDataSource

/********************************************************************************
  This file is for temporary Login till any Security component is deployed.
  This file also for Activity samples.
  Please donot use this login file. i.e dont use http://localhost:9000/login
  Use only http://localhost:9000
 *********************************************************************************/

class UserController /* @Inject()(pe: ProcessEngine) */ extends Controller {

  implicit val postWrites = Json.writes[LoginForm]

  val loginform:Form[LoginForm] = Form(
    mapping(
      "name" -> text,
      "password" -> text
    ) (LoginForm.apply)(LoginForm.unapply) verifying ("Invalid email or password", result => result match {
      case loginForm => check(loginForm.name, loginForm.password)
    })
  )

  def check(username: String, password: String) = {
    (username == "applicant" && password == "1234") ||
    (username == "portfoliomanager" && password == "1234")
  }

  def loginForm = Action{
    Ok(views.html.loginForm("", loginform))
  }

  def loginFormSubmit = Action { implicit request =>

    loginform.bindFromRequest.fold(
      errors => {
        Ok(views.html.loginForm("error", loginform))
      },
      user=> {

        // Set-up an H2 database.
        Class.forName("org.h2.Driver")
        val jdbcUrl = "jdbc:h2:activiti"
        implicit val ds = new JdbcDataSource()
        ds.setURL(jdbcUrl)
        val processEngine = new ProcessEngineWrapper

        val processId = processEngine.deploy {
          <process id="logging-test" name="Logging Test" isExecutable="true">
            <startEvent id="start" name="Start"></startEvent>
            <sequenceFlow id="flow1" sourceRef="start" targetRef="example-task"></sequenceFlow>
            <serviceTask id="example-task" name="Service Task" activiti:class={classOf[ExampleServiceTask].getName}></serviceTask>
            <sequenceFlow id="flow2" sourceRef="example-task" targetRef="end"></sequenceFlow>
            <endEvent id="end" name="End"></endEvent>
          </process>
        }

        val pdl:util.List[ProcessDefinition]  = processEngine.engine.getRepositoryService().
          createProcessDefinitionQuery().list();

        start(2401, processEngine)

        val pdl1:util.List[ProcessDefinition] = processEngine.engine.getRepositoryService().
          createProcessDefinitionQuery().list();

        val utl:util.List[Task] = processEngine.engine.getTaskService().createTaskQuery().
          taskUnnassigned().list();

        val atl:util.List[Task] = processEngine.engine.getTaskService().createTaskQuery().
          taskAssignee(Security.username).list();

        if(user.name.equals("applicant"))
        Redirect(routes.OpportunityController.showOpportunities()).withSession(Security.username -> user.name)
        else if(user.name.equals("portfoliomanager"))
          Redirect(manage.routes.OpportunityController.showNewOpportunityForm()).withSession(Security.username -> user.name)
        else
          Redirect(routes.OpportunityController.showOpportunities()).withSession(Security.username -> user.name)
      }
    )
  }

  def start(pid:Int, processEngine: ProcessEngineWrapper){
    processEngine.engine.getRuntimeService().startProcessInstanceByKey("logging-test")
  }

}

case class LoginForm(name: String, password: String)
