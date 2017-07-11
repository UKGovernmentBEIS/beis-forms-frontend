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

import java.io.File
import javax.inject.Inject

import actions.{AppDetailAction, AppSectionAction}
import config.Config
import eu.timepit.refined.auto._
import forms.validation._
import forms.{FileList, FileUploadItem, TextField}
import models.{LongId, _}
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.mvc.{Action, Controller, MultipartFormData, Result}
import services.{ApplicationFormOps, ApplicationOps, MessageBoardOps, OpportunityOps}

import scala.concurrent
import scala.concurrent.{ExecutionContext, Future}

class DashBoardController @Inject()(   applications: ApplicationOps,
                                       opps: OpportunityOps,
                                       appforms: ApplicationFormOps,
                                       msgs: MessageBoardOps
                                     )(implicit ec: ExecutionContext)
  extends Controller with ApplicationResults {

  def applicantDashBoard = Action.async { implicit request =>
    val userId = request.session.get("username").getOrElse("Unauthorised User")
    for(
        appsSeq <- applications.getApplicationsByUserId(UserId(userId)).map{
        case apps: Seq[Application] => apps
        case _ => Seq()
        };
        oppsSeq <- opps.getOpenOpportunitySummaries.map {
        case ops: Seq[Opportunity] => ops
        case _ => Seq()
        };
        msgSeq <- msgs.byUserId(UserId(userId)).map {
        case msgs: Seq[Message] => msgs
        case _ => Seq()
        }
    )yield(
      Ok(views.html.showApplicantDashBoard(appsSeq, oppsSeq, msgSeq)))
  }
  def staffDashBoard = Action.async { implicit request =>
    val userId = request.session.get("username").getOrElse("Unauthorised User")

    for(

      oppsSeq <- opps.getOpenOpportunitySummaries.map {
          case ops: Seq[Opportunity] => //ops
        {

          ops.filter( op =>
            appforms.byOpportunityId(op.id).map(  s=> s.getOrElse(ApplicationForm(null,null,null)).sections.head.sectionType.name)
              == "simpleform")
          ops
        }

          /*oppsSeq <- opps.getOpenOpportunitySummaries.map {
          case ops: Seq[Opportunity] => //ops
          {

            ops.filter( op =>
              appforms.byOpportunityId(op.id).map(  s=> s.getOrElse(ApplicationForm(null,null,null)).sections.head.sectionType.name)
                == "simpleform")

          }*/
        case _ => Seq()
      }/*;
      msgSeq <- msgs.byUserId(UserId(userId)).map {
        case msgs: Seq[Message] => msgs
        case _ => Seq()
      }*/
    )yield(
      Ok(views.html.showStaffDashBoard(/*appsSeq, */oppsSeq/*, msgSeq*/)))
  }
}
