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
import java.net.URL
import javax.inject.Inject

import actions.{AppDetailAction, AppSectionAction}
import config.Config
import eu.timepit.refined.auto._
import forms.validation._
import forms.{FileList, FileUploadItem, TextField}
import models._
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.mvc._
import services.{AWSOps, ApplicationFormOps, ApplicationOps, OpportunityOps}
import play.api.mvc.{Action, Controller, MultipartFormData, Result}

import scala.concurrent.{ExecutionContext, Future}

class ApplicationController @Inject()(
                                       actionHandler: ActionHandler,
                                       awsHandler: AWSHandler,
                                       applications: ApplicationOps,
                                       forms: ApplicationFormOps,
                                       opps: OpportunityOps,
                                       awsS3: AWSOps,
                                       AppDetailAction: AppDetailAction,
                                       AppSectionAction: AppSectionAction
                                     )(implicit ec: ExecutionContext)
  extends Controller with ApplicationResults {

  implicit val fileuploadReads = Json.reads[FileUploadItem]
  implicit val fileuploadItemF = Json.format[FileUploadItem]
  implicit val fileListReads = Json.reads[FileList]


  //TODO:- Need to check user is Authenticated and Authorised before access the methds - to be done using Shibboleth??
  def showForForm(id: ApplicationFormId) = Action.async { request =>
    implicit val userId = request.session.get("username").getOrElse("Unauthorised User")
    applications.byFormId(id, UserId(userId)).map {
      case Some(app) =>
        app.personalReference.map { _ => redirectToOverview(app.id) }
          .getOrElse(Redirect(controllers.routes.ApplicationController.editPersonalRef(app.id)))
      case None => NotFound
    }
  }

  def createForForm(id: ApplicationFormId) = Action.async { request =>
    val userId = request.session.get("username").getOrElse("Unauthorised User")
    applications.createForForm(id, UserId(userId)).map {
      case Some(app) =>
        app.personalReference.map { _ => redirectToOverview(app.id) }
          .getOrElse(Redirect(controllers.routes.ApplicationController.editPersonalRef(app.id)))
      case None => NotFound
    }
  }

  def show(id: ApplicationId) = AppDetailAction(id) { request =>
    Ok(views.html.showApplicationForm(request.appDetail, List.empty))
  }

   def simpleAppshow(id: ApplicationId) = AppDetailAction(id) { request =>
    Ok(views.html.showSimpleApplicationForm(request.appDetail, List.empty))
  }

  def reset = Action.async {
    applications.reset().map(_ => Redirect(controllers.routes.StartPageController.startPage()))
  }

  import FieldCheckHelpers._

  def editSectionForm(id: ApplicationId, sectionNumber: AppSectionNumber) = AppSectionAction(id, sectionNumber) { request =>
    val hints = request.appSection.section.map(s => hinting(s.answers, checksFor(request.appSection.formSection))).getOrElse(List.empty)
    actionHandler.renderSectionForm(request.appSection, noErrors, hints)
  }

  def resetAndEditSection(id: ApplicationId, sectionNumber: AppSectionNumber) = Action.async { request =>
    applications.clearSectionCompletedDate(id, sectionNumber).map { _ =>
      Redirect(controllers.routes.ApplicationController.editSectionForm(id, sectionNumber))
    }
  }



  def addFileItem(applicationId: ApplicationId, sectionNumber: AppSectionNumber) = AppSectionAction(applicationId, sectionNumber) { implicit request =>
    awsHandler.showFileItemForm(request.appSection, JsObject(List.empty), List.empty)
  }

  def deleteFileItem(applicationId: ApplicationId, sectionNumber: AppSectionNumber, itemNumber: Int, ext: String) = Action.async {
    applications.deleteItem(applicationId, sectionNumber, itemNumber).flatMap { _ =>
      awsHandler.deleteFileFromAWSS3(itemNumber.toString + ext)
      // Check if we deleted the last item in the list and, if so, delete the section so
      // it will go back to the Not Started state.
      applications.getSection(applicationId, sectionNumber).flatMap {
        case Some(s) if (s.answers \ "items").validate[JsArray].asOpt.getOrElse(JsArray(List.empty)).value.isEmpty =>
          applications.deleteSection(applicationId, sectionNumber).map { _ =>
            redirectToSectionForm(applicationId, sectionNumber)
          }
        case _ => Future.successful(redirectToSectionForm(applicationId, sectionNumber))
      }
    }
  }

  /** This method
   1. Checks the user authorisation for ASW S3 access
   2. Download the AWS S3 file to a temp location on PAAS server
   3. Create a File objectwith the file.
   3. Out put the file to User Browser to download
    **/

  def downloadFile(id: ApplicationId,  sectionNumber: AppSectionNumber, key: ResourceKey) = Action { implicit request =>
    val file = awsS3.download(key)
    val fileTmp = "attachment; filename=" + key.key
    val filedownloaddirectory = Config.config.file.filedownloaddirectory
    Ok.sendFile(new File(filedownloaddirectory + key.key), inline=true).withHeaders(CACHE_CONTROL->"max-age=3600",
      CONTENT_DISPOSITION->fileTmp, CONTENT_TYPE->"application/x-download")
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
      case _ => Future.successful(redirectToSectionForm(id, sectionNumber))
    }
  }

  def showSectionForm(id: ApplicationId, sectionNumber: AppSectionNumber) = AppSectionAction(id, sectionNumber) { request =>
    request.appSection.section match {
      case None =>
        val hints = hinting(JsObject(List.empty), checksFor(request.appSection.formSection))
        actionHandler.renderSectionForm(request.appSection, noErrors, hints)

      case Some(s) =>
        if (s.isComplete) actionHandler.redirectToPreview(id, sectionNumber)
        else {
          val hints = hinting(s.answers, checksFor(request.appSection.formSection))
          actionHandler.renderSectionForm(request.appSection, noErrors, hints)
        }
    }
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

  def postSection(id: ApplicationId, sectionNumber: AppSectionNumber) = AppSectionAction(id, sectionNumber).async(JsonForm.fileuploadparser) {
    implicit request =>
      implicit val userId = request.session.get("username").getOrElse("Unauthorised User")

      request.body.action match {

        case Complete => {
          actionHandler.doComplete(request.appSection, request.body.values)
      }
        case Save => {
          actionHandler.doSave(request.appSection, request.body.values)
        }
        case FileUpload => {
          request.body.mf match {
            case Some(file) =>{
              awsHandler.uploadFileAWSS3(id, sectionNumber, request.appSection, request.body.values, file, userId)
            }
            case None =>
              Future.successful(redirectToOverview(id))
          }
        }
        case SaveItem => actionHandler.doSaveItem(request.appSection, request.body.values)
        case Preview => actionHandler.doPreview(request.appSection, request.body.values)
        case completeAndPreview => actionHandler.completeAndPreview(request.appSection, request.body.values)
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
      actionHandler.doSubmit(id, request.appDetail, UserId(userId)).map {
        case Some(e) =>
          Ok(views.html.submitApplicationForm(e.applicationRef, emailto, appsubmittime))
        case None => NotFound
      }
    } else Future.successful(Ok(views.html.showApplicationForm(request.appDetail, sectionErrors)))
  }

  def checkSection(appFormSection: ApplicationFormSection, appSection: ApplicationSection): Option[SectionError] = {
    appSection.completedAt match {
      case Some(_) => None
      case None => Some(SectionError(appFormSection, "In progress"))
    }
  }

  def checksFor(formSection: ApplicationFormSection): Map[String, FieldCheck] =
    formSection.fields.map(f => f.name -> f.check).toMap

  val APP_REF_FIELD_NAME = "application-ref"
  val appRefField = TextField(label = Some(APP_REF_FIELD_NAME), name = APP_REF_FIELD_NAME, isEnabled = true, isMandatory = false, isNumeric = false, maxWords = 200)
  val appRefQuestion = Map(APP_REF_FIELD_NAME -> Question("My application reference"))

  def editPersonalRef(id: ApplicationId) = AppDetailAction(id) { request =>
    val answers = JsObject(Seq(APP_REF_FIELD_NAME -> Json.toJson(request.appDetail.personalReference.map(_.value).getOrElse(""))))
    val hints = hinting(answers, Map(appRefField.name -> appRefField.check))
    Ok(views.html.personalReferenceForm(appRefField, request.appDetail, appRefQuestion, answers, Nil, hints))
  }

  def savePersonalRef(id: ApplicationId) = AppDetailAction(id).async(JsonForm.parser) { request =>
    request.body.action match {
      case Save => appRefField.check(appRefField.name, Json.toJson(JsonHelpers.flatten(request.body.values).getOrElse(APP_REF_FIELD_NAME, ""))) match {
        case Nil =>
          applications.updatePersonalReference(request.appDetail.id, JsonHelpers.flatten(request.body.values).getOrElse(APP_REF_FIELD_NAME, "")).map { _ =>
            Redirect(controllers.routes.ApplicationController.show(request.appDetail.id))
          }
        case errs =>
          val hints = hinting(request.body.values, Map(appRefField.name -> appRefField.check))
          Future.successful(
            Ok(views.html.personalReferenceForm(appRefField, request.appDetail, appRefQuestion, request.body.values, errs, hints))
          )
      }
      case _ =>
        Future.successful(Redirect(controllers.routes.ApplicationController.show(request.appDetail.id)))
    }

  }



}
