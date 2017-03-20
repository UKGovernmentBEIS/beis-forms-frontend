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

import play.api.mvc.{Action, Controller}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller, MultipartFormData, Result}
import play.api.mvc.Security
import org.activiti.engine.{ProcessEngine, ProcessEngines}
import org.h2.jdbcx.JdbcDataSource


class UserController /* @Inject()(pe: ProcessEngine) */ extends Controller {

  implicit val postWrites = Json.writes[LoginForm]

 /* val loginform = Form(
    tuple(
      "name" -> text,
      "password" -> text
    ) verifying ("Invalid email or password", result => result match {
      case (name, password) => check(name, password)
    })
  )
*/
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
    //Ok(views.html.startPage())
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

        //logger.info("Deploy business process")
        // (start) --flow1--> [example-task] --flow2--> (end)
        processEngine deploy {
          <process id="logging-test" name="Logging Test" isExecutable="true">
            <startEvent id="start" name="Start"></startEvent>
            <sequenceFlow id="flow1" sourceRef="start" targetRef="example-task"></sequenceFlow>
            <serviceTask id="example-task" name="Service Task" activiti:class={classOf[ExampleServiceTask].getName}></serviceTask>
            <sequenceFlow id="flow2" sourceRef="example-task" targetRef="end"></sequenceFlow>
            <endEvent id="end" name="End"></endEvent>
          </process>
        }

         /*val pdid = engine.getRepositoryService().
          createProcessDefinitionQuery().
          processDefinitionId(pid).
          singleResult().
          getId()*/

        //Redirect(routes.Application.index).withSession(Security.username -> user._1)
         //val pdl = pe.getRepositoryService().createProcessDefinitionQuery().list();
        if(user.name.equals("applicant"))
        Redirect(routes.OpportunityController.showOpportunities()).withSession(Security.username -> user.name)
        else if(user.name.equals("portfoliomanager"))
          Redirect(manage.routes.OpportunityController.showNewOpportunityForm()).withSession(Security.username -> user.name)
        else
          Redirect(routes.OpportunityController.showOpportunities()).withSession(Security.username -> user.name)
      }
    )
  }
/*  def start( pid:String){
    val pdid = pe.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(pid).
      singleResult().
      getId()
     pe.getRuntimeService().startProcessInstanceById(pdid)
       index()
  }

def index() {
  val user = Security.username
  val pdl = pe.getRepositoryService().
    createProcessDefinitionQuery().list();

  val utl = pe.getTaskService().createTaskQuery().
    taskUnnassigned().list();

 val atl = pe.getTaskService().createTaskQuery().taskAssignee("").list()
  println("---------------------" + user)
      //render(user, pdl, utl, atl)
  Redirect(routes.OpportunityController.showOpportunities()).withSession(Security.username -> user)

}
  */


}

case class LoginForm(name: String, password: String)
