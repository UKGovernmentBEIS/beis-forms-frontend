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
import scala.concurrent.{ExecutionContext, Future}

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
    (username == "applicant1" && password == "1234") ||
    (username == "applicant2" && password == "1234") ||
    (username == "applicant3" && password == "1234") ||
    (username == "applicant4" && password == "1234") ||
    (username == "manager" && password == "1234") ||
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

        //TODO:- Here the Roles come into place and Users belong to Group or Role
        implicit val userIdInSession = user.name
        if(user.name.equals("applicant1") || user.name.equals("applicant2") || user.name.equals("applicant3") || user.name.equals("applicant4"))
        Redirect(routes.DashBoardController.applicantDashBoard()).withSession(Security.username -> user.name)
        else if(user.name.equals("manager") || user.name.equals("portfoliomanager"))
          Redirect(routes.DashBoardController.staffDashBoard()).withSession(Security.username -> user.name)
          //Redirect(manage.routes.OpportunityController.showNewOpportunityForm()).withSession(Security.username -> user.name)
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
