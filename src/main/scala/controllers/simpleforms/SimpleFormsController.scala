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

package controllers.simpleforms

import java.io.File
import java.net.URL
import javax.inject.Inject

import actions.{AppDetailAction, AppSectionAction, OppSectionAction, OpportunityAction}
import config.Config
import controllers._
import controllers.FieldCheckHelpers.{hinting, noErrors}
import eu.timepit.refined.auto._
import forms.{FileList, FileUploadItem, TextField}
import forms.validation.{FieldError, SectionError}
import models.{AppSectionNumber, ApplicationFormId, ApplicationFormSection, ApplicationId, ApplicationSection, ApplicationSectionDetail, OppSectionNumber, OpportunityId, Question, ResourceKey, UserId}
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.{Action, Controller, Result}
import services.{AWSOps, ApplicationFormOps, ApplicationOps, OpportunityOps}

import scala.concurrent.{ExecutionContext, Future}

class SimpleFormsController @Inject()(
                                       actionHandler: ActionHandler,
                                       awsHandler: AWSHandler,
                                       applications: ApplicationOps,
                                       opportunities: OpportunityOps,
                                       forms: ApplicationFormOps,
                                       opps: OpportunityOps,
                                       awsS3: AWSOps,
                                       AppDetailAction: AppDetailAction,
                                       OpportunityAction: OpportunityAction,
                                       AppSectionAction: AppSectionAction
                                     )(implicit ec: ExecutionContext)
  extends Controller with ApplicationResults {

  implicit val fileuploadReads = Json.reads[FileUploadItem]
  implicit val fileuploadItemF = Json.format[FileUploadItem]
  implicit val fileListReads = Json.reads[FileList]

  def show(id: ApplicationId) = AppDetailAction(id) { request =>
    Ok(views.html.showSimpleApplicationForm(request.appDetail, List.empty))
  }

  def showForms = Action.async {
    opportunities.getOpenOpportunitySummaries.map { os => Ok(views.html.showOpportunities(os)) }
  }

  def showForm(id: OpportunityId, sectionNumber: Option[OppSectionNumber]) = OpportunityAction(id) { request =>
    Redirect(controllers.routes.OpportunityController.showOpportunitySection(id, sectionNumber.getOrElse(OppSectionNumber(1))))
  }

//  def showSicknessFormHome() = Action.async { request =>
//    val userId = request.session.get("username").getOrElse("Unauthorised User")
//    val oppId = OpportunityId(2)
//    val oppId = OpportunityId(2)
//    val opp = opportunities.byId(oppId).map{
//      case Some(s) => s
//      case None => None
//    }
//
//    appForms.byOpportunityId(oppId).flatMap {
//       case Some(appform) => apps.byFormId(appform.id, UserId(userId)).flatMap{
//            case app: Option[Application] => Future.successful(Ok(views.html.showSicknessForm(appform, app, opp , request.section)))
//            // None => Future.successful(NotFound)
//       }
//       case None => Future.successful(NotFound)
//    }
//  }

  def createSimpleForm(id: ApplicationFormId) = Action.async { request =>
    val userId = request.session.get("username").getOrElse("Unauthorised User")


    for(
      app <- applications.createForSimpleForm(id, UserId(userId)).map {
        case Some(a) =>  a
        //case None => None
      };

      appd <- applications.detail(app.id).map {
        case Some(ad) =>  ad
        //case None => Future.successful(NotFound(s"No application with id ${id.id} exists"))
      }
    )yield(
      Ok(views.html.showSimpleApplicationForm(appd, List.empty)))



//    val a =  applications.createForSimpleForm(id, UserId(userId)).map {
//          case Some(ad) =>  ad
//          //case None => None
//        }
//
//
//
//val h = applications.detail(app.id).flatMap {
//    case Some(ad) =>  ad
//    //case None => Future.successful(NotFound(s"No application with id ${id.id} exists"))
//  }

//    applications.createForSimpleForm(id, UserId(userId)).map {
//      case Some(app) => {
//        applications.detail(app.id).flatMap {
//          case Some(ad) =>  Ok(views.html.showSimpleApplicationForm(ad, List.empty))
//          //case None => Future.successful(NotFound(s"No application with id ${id.id} exists"))
//        }
//       // Ok(controllers.routes.ApplicationController.simpleAppshow(app.id))
//
//      }
//      case None => NotFound
//    }
  }

  def showSectionSimpleForm(id: ApplicationId, sectionNumber: AppSectionNumber) = AppSectionAction(id, sectionNumber) { request =>
    request.appSection.section match {
      case None =>
        val hints = hinting(JsObject(List.empty), checksFor(request.appSection.formSection))
        actionHandler.renderSectionSimpleForm(request.appSection, noErrors, hints)

      case Some(s) =>
        val hints = hinting(s.answers, checksFor(request.appSection.formSection))
        actionHandler.renderSectionSimpleForm(request.appSection, noErrors, hints)

      /*if (s.isComplete) actionHandler.redirectToPreview(id, sectionNumber)
      else {
        val hints = hinting(s.answers, checksFor(request.appSection.formSection))
        actionHandler.renderSectionSimpleForm(request.appSection, noErrors, hints)
      }*/
    }
  }

  def checksFor(formSection: ApplicationFormSection): Map[String, FieldCheck] =
    formSection.fields.map(f => f.name -> f.check).toMap

  val APP_REF_FIELD_NAME = "application-ref"
  val appRefField = TextField(label = Some(APP_REF_FIELD_NAME), name = APP_REF_FIELD_NAME, isEnabled = true, isMandatory = false, isNumeric = false, maxWords = 200)
  val appRefQuestion = Map(APP_REF_FIELD_NAME -> Question("My application reference"))


  def postSection(id: ApplicationId, sectionNumber: AppSectionNumber) = AppSectionAction(id, sectionNumber).async(JsonForm.fileuploadparser) {
    implicit request =>
      implicit val userId = request.session.get("username").getOrElse("Unauthorised User")

      request.body.action match {

        case Complete => {
          println("Complete------")
          actionHandler.doCompleteSimple(request.appSection, request.body.values)
      }
        case Save => {
          actionHandler.doSaveSimple(request.appSection, request.body.values)
        }
        case FileUpload => {
          request.body.mf match {
            case Some(file) =>{
               awsHandler.uploadFileAWSS3Simple(id, sectionNumber, request.appSection, request.body.values, file, userId)
            }
            case None =>
              Future.successful(redirectToSimpleFormOverview(id))
          }
        }
        case SaveItem => actionHandler.doSaveItemSimple(request.appSection, request.body.values)
        case Preview => actionHandler.doPreviewSimple(request.appSection, request.body.values)
       // case completeAndPreview => actionHandler.completeAndPreview(request.appSection, request.body.values)
      }
  }

  def submit(id: ApplicationId) = AppDetailAction(id).async { request =>
    val userId = request.session.get("username").getOrElse("Unauthorised User")
    val sectionErrors: Seq[SectionError] = request.appDetail.applicationForm.sections.sortBy(_.sectionNumber).flatMap { fs =>
      request.appDetail.sections.find(_.sectionNumber == fs.sectionNumber) match {
        case None => Some(SectionError(fs, "Not started"))
        case Some(s) => checkSection(fs, s)
      }
    }
    if (sectionErrors.isEmpty) {
      val emailto = Config.config.business.emailto
      val dtf = DateTimeFormat.forPattern("HH:mm:ss")
      val appsubmittime = dtf.print(LocalDateTime.now()) //returns TimeZone Europe/London
      //actionHandler.doSubmit(id).map {
      actionHandler.doSubmitSimple(id, request.appDetail, UserId(userId)).map {
        case Some(e) =>
          Ok(views.html.submitSimpleApplicationForm(e.applicationRef, emailto, appsubmittime))
        case None => NotFound
      }
    } else Future.successful(Ok(views.html.showSimpleApplicationForm(request.appDetail, sectionErrors)))
  }


  def addFileItem(applicationId: ApplicationId, sectionNumber: AppSectionNumber) = AppSectionAction(applicationId, sectionNumber) { implicit request =>
    awsHandler.showSimpleFileItemForm(request.appSection, JsObject(List.empty), List.empty)
  }

  def deleteFileItem(applicationId: ApplicationId, sectionNumber: AppSectionNumber, itemNumber: Int, ext: String) = Action.async {
    applications.deleteItem(applicationId, sectionNumber, itemNumber).flatMap { _ =>
      awsHandler.deleteFileFromAWSS3(itemNumber.toString + ext)
      // Check if we deleted the last item in the list and, if so, delete the section so
      // it will go back to the Not Started state.
      applications.getSection(applicationId, sectionNumber).flatMap {
        case Some(s) if (s.answers \ "items").validate[JsArray].asOpt.getOrElse(JsArray(List.empty)).value.isEmpty =>
          applications.deleteSection(applicationId, sectionNumber).map { _ =>
            redirectToSimpleSectionForm(applicationId, sectionNumber)
          }
        case _ => Future.successful(redirectToSimpleSectionForm(applicationId, sectionNumber))
      }
    }
  }

  /** This method
    * 1. Checks the user authorisation for ASW S3 access
    * 2. Create a preSigned URL to be accessed by thers (for public consumption)
    * 3. Out put to User Browser to download
    **/

  def downloadFileDirect(id: ApplicationId,  sectionNumber: AppSectionNumber, key: ResourceKey) = AppSectionAction(id, sectionNumber).async { request =>
    val preSignedURL = awsS3.downloadDirect(key)
    preSignedURL.flatMap {
      case url: URL => Future.successful(Redirect(url.toString))
      //TODO:- This is error case:- need to update method to add error message 'Error in downloading document.... Please try again'
      case _ => Future.successful(redirectToSimpleSectionForm(id, sectionNumber))
    }
  }
  def checkSection(appFormSection: ApplicationFormSection, appSection: ApplicationSection): Option[SectionError] = {
    appSection.completedAt match {
      case Some(_) => None
      case None => Some(SectionError(appFormSection, "In progress"))
    }
  }



  def showGuidancePage(id: OpportunityId) = Action {
    Ok(views.html.guidance(id))
  }

}



