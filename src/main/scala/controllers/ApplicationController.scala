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

import actions.{AppDetailAction, AppSectionAction}
import config.Config
import eu.timepit.refined.auto._
import forms.{FileList, FileUploadItem, TextField}
import forms.validation._
import models._
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.mvc.{Action, Controller, MultipartFormData, Result}
import services.{ApplicationFormOps, ApplicationOps, OpportunityOps}

import scala.concurrent.{ExecutionContext, Future}
import java.io.File

class ApplicationController @Inject()(
                                       actionHandler: ActionHandler,
                                       applications: ApplicationOps,
                                       forms: ApplicationFormOps,
                                       opps: OpportunityOps,
                                       AppDetailAction: AppDetailAction,
                                       AppSectionAction: AppSectionAction
                                     )(implicit ec: ExecutionContext)
  extends Controller with ApplicationResults {

  implicit val fileuploadReads = Json.reads[FileUploadItem]
  implicit val fileuploadItemF = Json.format[FileUploadItem]
  implicit val fileListReads = Json.reads[FileList]

  def showOrCreateForForm(id: ApplicationFormId) = Action.async {
    applications.getOrCreateForForm(id).map {
      case Some(app) =>
        app.personalReference.map { _ => redirectToOverview(app.id) }
          .getOrElse(Redirect(controllers.routes.ApplicationController.editPersonalRef(app.id)))
      case None => NotFound
    }
  }

  def show(id: ApplicationId) = AppDetailAction(id) { request =>
    Ok(views.html.showApplicationForm(request.appDetail, List.empty))
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

  def addFileItem(applicationId: ApplicationId, sectionNumber: AppSectionNumber) = AppSectionAction(applicationId, sectionNumber) { implicit request =>
    showFileItemForm(request.appSection, JsObject(List.empty), List.empty)
  }

  def showFileItemForm(app: ApplicationSectionDetail, doc: JsObject, errs: FieldErrors, itemNumber: Option[Int] = None): Result = {
    import ApplicationData._
    import FieldCheckHelpers._

    val fields = itemFieldsFor(app.sectionNumber).getOrElse(List.empty)
    val checks = itemChecksFor(app.sectionNumber)
    val hints = hinting(doc, checks)
    val answers = app.section.map { s => s.answers }.getOrElse(JsObject(List.empty))
    Ok(views.html.fileUploadForm(app, answers, errs, hints))
  }

  def postSection(id: ApplicationId, sectionNumber: AppSectionNumber) = AppSectionAction(id, sectionNumber).async(JsonForm.fileuploadparser) {
    implicit request =>

      request.body.action match {

        case Complete => {
          actionHandler.doComplete(request.appSection, request.body.values)
      }
        case Save => {
          actionHandler.doSave(request.appSection, request.body.values)
        }
        case FileUpload => {
          request.body.mf.file("file") match {
            case Some(file) =>
              uploadFile(id, sectionNumber, request.appSection, request.body.values, file)
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
    val sectionErrors: Seq[SectionError] = request.appDetail.applicationForm.sections.sortBy(_.sectionNumber).flatMap { fs =>
      request.appDetail.sections.find(_.sectionNumber == fs.sectionNumber) match {
        case None => Some(SectionError(fs, "Not started"))
        case Some(s) => checkSection(fs, s)
      }
    }

    if (sectionErrors.isEmpty) {
      val emailto = Config.config.business.emailto
      //val emailto = "experiencederic@university.ac.uk"
      val dtf = DateTimeFormat.forPattern("HH:mm:ss")
      val appsubmittime = dtf.print(LocalDateTime.now()) //returns TimeZOne Europe/London
      actionHandler.doSubmit(id).map {
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
  val appRefField = TextField(label = Some(APP_REF_FIELD_NAME), name = APP_REF_FIELD_NAME, isNumeric = false, maxWords = 20)
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

  def uploadFile(id: ApplicationId,  sectionNumber: AppSectionNumber, appSection: ApplicationSectionDetail , fieldValues: JsObject,
                 mf: MultipartFormData.FilePart[TemporaryFile]) :Future[Result] = {
      import java.io.File
      val filename = mf.filename
      val contentType = mf.contentType

      /* File Upload */
      val fileUploadItem:FileUploadItem = FileUploadItem(filename)
      val fileuploaddirectory = Config.config.file.fileuploaddirectory

      applications.saveFileItem(id, sectionNumber, JsObject(Seq("item" -> Json.toJson(fileUploadItem)))).flatMap {
      case itemnumber => {
        mf.ref.moveTo(new File(s"$fileuploaddirectory/$itemnumber"))
        Future.successful(redirectToSectionForm(id, sectionNumber))
      }
     }
   }

  def deleteFileItem(applicationId: ApplicationId, sectionNumber: AppSectionNumber, itemNumber: Int) = Action.async {
    applications.deleteItem(applicationId, sectionNumber, itemNumber).flatMap { _ =>
      deleteFileFromFolder(itemNumber)
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

  implicit class FileMonads(f: java.io.File) {
    def check = if (f.exists) Some(f) else None
    def remove = if (f.delete()) Some(f) else None
  }

  def deleteFileFromFolder(itemNumber: Int) ={
    val filepath = Config.config.file.fileuploaddirectory + "/"  + itemNumber
      println("Deleting File ........" + filepath)
    for {
      foundFile <- new File(filepath).check
      deletedFile <- foundFile.remove
    } yield deletedFile

  }

  /*def uploadFile_b(id: ApplicationId) = Action(parse.multipartFormData) { request =>
    request.body.dataParts.map{ xx =>
      print("**********" + xx.toString())
    }
    request.body.file("file").map { file =>
      import java.io.File
      val filename = file.filename
      val contentType = file.contentType
      file.ref.moveTo(new File(s"/Users/venkatamutyala/test/$filename"))
      Redirect(controllers.routes.ApplicationController.show(id))
    }.getOrElse {
      Ok("File NOT uploaded")
    }
  }*/

}
