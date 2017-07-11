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
import controllers.FieldCheckHelpers.FieldErrors
import forms.{FileList, FileUploadItem}
import forms.validation.FieldError
import models.{AppSectionNumber, ApplicationId, ApplicationSectionDetail, ResourceKey}
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.{Action, Controller, MultipartFormData, Result}
import services.{AWSOps, ApplicationFormOps, ApplicationOps, OpportunityOps}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by venkatamutyala on 02/07/2017.
  */
class AWSHandler @Inject()(
                            actionHandler: ActionHandler,
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


  def showFileItemForm(app: ApplicationSectionDetail, doc: JsObject, errs: FieldErrors, itemNumber: Option[Int] = None): Result = {
    import ApplicationData._
    import FieldCheckHelpers._

    val fields = itemFieldsFor(app.sectionNumber).getOrElse(List.empty)
    val checks = itemChecksFor(app.sectionNumber)
    val hints = hinting(doc, checks)
    val answers = app.section.map { s => s.answers }.getOrElse(JsObject(List.empty))
    Ok(views.html.fileUploadForm(app, answers, errs, hints))
  }

  def uploadFileAWSS3(id: ApplicationId,  sectionNumber: AppSectionNumber, appSection: ApplicationSectionDetail , fieldValues: JsObject,
                      f: File, userId : String) :Future[Result] = {

    val filename = fieldValues.fields.head._2.toString().replaceAll("^\"|\"$", "")
    StringUtils.isEmpty(filename) match {
      case true => Future.successful(showFileItemForm(appSection, null, List(FieldError("supportingDocuments", "File name should not be empty"))))
      case false => {
        val extension = FilenameUtils.getExtension(filename)
        /* File Upload */
        val fileUploadItem:FileUploadItem = FileUploadItem(filename)
        /** Save file metadata in Database and Physical file in AWS S3  **/
        applications.saveFileItem(id, sectionNumber, JsObject(Seq("item" -> Json.toJson(fileUploadItem)))).flatMap {
          case itemnumber => {
            /** AWS S3 call to store files on AWS S3 **/
            awsS3.upload(ResourceKey( itemnumber + "." + extension), f).flatMap{
              case Nil =>  Future.successful(redirectToSectionForm(id, sectionNumber))
              case errs => Future.successful(showFileItemForm(appSection, null, errs))
            }
          }
        }
      }
    }

  }
//---------------------
  def uploadFileAWSS3Simple(id: ApplicationId,  sectionNumber: AppSectionNumber, appSection: ApplicationSectionDetail , fieldValues: JsObject,
                      f: File, userId : String) :Future[Result] = {

    val filename = fieldValues.fields.head._2.toString().replaceAll("^\"|\"$", "")
    StringUtils.isEmpty(filename) match {
      case true => Future.successful(showSimpleFileItemForm(appSection, null, List(FieldError("supportingDocuments", "File name should not be empty"))))
      case false => {
        val extension = FilenameUtils.getExtension(filename)
        /* File Upload */
        val fileUploadItem:FileUploadItem = FileUploadItem(filename)
        /** Save file metadata in Database and Physical file in AWS S3  **/
        applications.saveFileItem(id, sectionNumber, JsObject(Seq("item" -> Json.toJson(fileUploadItem)))).flatMap {
          case itemnumber => {
            /** AWS S3 call to store files on AWS S3 **/
            awsS3.upload(ResourceKey( itemnumber + "." + extension), f).flatMap{
              case Nil =>  Future.successful(redirectToSimpleSectionForm(id, sectionNumber))
              case errs => Future.successful(showSimpleFileItemForm(appSection, null, errs))
            }
          }
        }
      }
    }

  }


  def showSimpleFileItemForm(app: ApplicationSectionDetail, doc: JsObject, errs: FieldErrors, itemNumber: Option[Int] = None): Result = {
    import ApplicationData._
    import FieldCheckHelpers._

    val fields = itemFieldsFor(app.sectionNumber).getOrElse(List.empty)
    val checks = itemChecksFor(app.sectionNumber)
    val hints = hinting(doc, checks)
    val answers = app.section.map { s => s.answers }.getOrElse(JsObject(List.empty))
    Ok(views.html.fileUploadSimpleForm(app, answers, errs, hints))
  }
  //------------
  /** TODO:- Not Used ** To Be Deleted**/
  def uploadFileToLocalDirectory(id: ApplicationId,  sectionNumber: AppSectionNumber, appSection: ApplicationSectionDetail , fieldValues: JsObject,
                                 mf: MultipartFormData.FilePart[TemporaryFile], userId : String) :Future[Result] = {
    import java.io.File
    val filename = mf.filename
    val contentType = mf.contentType
    /* File Upload */
    val fileUploadItem:FileUploadItem = FileUploadItem(filename)
    val fileuploaddirectory = Config.config.file.fileuploaddirectory

    /** Save file metadata in Database and Physical file in AWS S3 call **/
    applications.saveFileItem(id, sectionNumber, JsObject(Seq("item" -> Json.toJson(fileUploadItem)))).flatMap {
      case itemnumber => {
        val file:File = new File(s"$fileuploaddirectory/$itemnumber")
        mf.ref.moveTo(file)
        Future.successful(redirectToSectionForm(id, sectionNumber))
      }
    }
  }



  def deleteFileFromAWSS3(itemNumber: String) ={
    val file = awsS3.delete(ResourceKey(itemNumber))
  }

  implicit class FileMonads(f: java.io.File) {
    def check = if (f.exists) Some(f) else None
    def remove = if (f.delete()) Some(f) else None
  }

  def deleteFileFromLocalFolder(itemNumber: Int) ={
    val filepath = Config.config.file.fileuploaddirectory + "/"  + itemNumber
    println("Deleting File ........" + filepath)
    for {
      foundFile <- new File(filepath).check
      deletedFile <- foundFile.remove
    } yield deletedFile
  }
}
