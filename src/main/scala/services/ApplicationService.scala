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

import akka.stream.scaladsl.FileIO
import com.google.inject.Inject
import config.Config
import controllers.FieldCheckHelpers.FieldErrors
import controllers.{FieldCheck, FieldCheckHelpers, FieldChecks}
import forms.{FileList, FileSectionValidator, FileUploadItem}
import forms.validation.{CostSectionValidator, FieldError}
import models._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
//import play.mvc.BodyParser.MultipartFormData

import scala.concurrent.{ExecutionContext, Future}
import java.io.{ByteArrayOutputStream, File, FileInputStream}

import play.api.libs.Files.TemporaryFile
//import play.api.mvc.MultipartFormData

//import scala.io.Source

//import com.ning.http.client.FluentCaseInsensitiveStringsMap
//import com.ning.http.multipart.{FilePart, MultipartRequestEntity, StringPart}
import play.api.http.HeaderNames._
import play.api.http.{ContentTypeOf, Writeable}
import play.api.mvc.{Codec, MultipartFormData}
import java.io.{ByteArrayOutputStream, File}


import akka.stream.scaladsl.{FileIO, Source}

//import scala.io.Source


class ApplicationURLs(baseUrl: String) {
  def application(id: ApplicationId): String =
    s"$baseUrl/application/${id.id}"

  def detail(id: ApplicationId): String =
    s"$baseUrl/application/${id.id}/detail"

  def submit(id: ApplicationId) =
    s"$baseUrl/application/${id.id}/submit"

  def personalRef(id: ApplicationId) =
    s"$baseUrl/application/${id.id}/personal-ref"

  def markNotCompleted(id: ApplicationId, sectionNumber: AppSectionNumber) =
    s"$baseUrl/application/${id.id}/section/${sectionNumber.num}/markNotCompleted"

  def section(id: ApplicationId, sectionNumber: AppSectionNumber) =
    s"$baseUrl/application/${id.id}/section/${sectionNumber.num}"

  def sectionDetail(id: ApplicationId, sectionNumber: AppSectionNumber) =
    s"$baseUrl/application/${id.id}/section/${sectionNumber.num}/detail"

  def sections(id: ApplicationId) =
    s"$baseUrl/application/${id.id}/sections"

  def complete(id: ApplicationId, sectionNumber: AppSectionNumber) =
    s"$baseUrl/application/${id.id}/section/${sectionNumber.num}/complete"

  def item(id: ApplicationId, sectionNumber: AppSectionNumber, itemNumber: Int) =
    s"$baseUrl/application/${id.id}/section/${sectionNumber.num}/item/$itemNumber"

  def items(id: ApplicationId, sectionNumber: AppSectionNumber) =
    s"$baseUrl/application/${id.id}/section/${sectionNumber.num}/items"

  def fileitems(id: ApplicationId, sectionNumber: AppSectionNumber) =
    s"$baseUrl/application/${id.id}/section/${sectionNumber.num}/fileitems"



  def uploadFile() =
    s"$baseUrl/application/uploadfile"

  val reset = s"$baseUrl/reset"
}

class ApplicationService @Inject()(val ws: WSClient)(implicit val ec: ExecutionContext)
  extends ApplicationOps with RestService {
  //implicit val fileuploadItemF = Json.format[FileUploadItem]
  //implicit val fileListF = Json.format[FileList]
  //implicit val multipartFormDataReads = Json.format[play.api.mvc.MultipartFormData.FilePart[play.api.libs.Files.TemporaryFile]]
  //implicit val temporaryFileReads = Json.format[play.api.libs.Files.TemporaryFile]
  //implicit val fileReads = Json.reads[java.io.File]
  //implicit val fileFormat = Json.format[java.io.File]
  //mplicit val filePartReads = Json.format[play.api.mvc.MultipartFormData.FilePart[ A]

  val baseUrl = Config.config.business.baseUrl
  val urls = new ApplicationURLs(baseUrl)
  val appFormUrls = new ApplicationFormURLs(baseUrl)

  override def byId(id: ApplicationId): Future[Option[Application]] =
    getOpt[Application](urls.application(id))

  override def saveSection(id: ApplicationId, sectionNumber: AppSectionNumber, doc: JsObject): Future[Unit] =
    post(urls.section(id, sectionNumber), doc)

  override def completeSection(id: ApplicationId, sectionNumber: AppSectionNumber, doc: JsObject): Future[FieldErrors] = {
    sectionDetail(id, sectionNumber).flatMap {
      case Some(app) =>
        FieldCheckHelpers.check(doc, checksFor(app.formSection)) match {
          case Nil => post(urls.complete(id, sectionNumber), doc).map(_ => List())
          case errs => {
            Future.successful(errs)
          }
        }
      // TODO: Need better error handling here
      case None => Future.successful(List(FieldError("", s"tried to save a non-existent section number $sectionNumber in application ${id.id}")))
    }
  }

  def checksFor(formSection: ApplicationFormSection): Map[String, FieldCheck] =
    formSection.sectionType match {
      case SectionTypeForm => formSection.fields.map(f => f.name -> f.check).toMap
      case SectionTypeCostList => Map("items" -> FieldChecks.fromValidator(CostSectionValidator(2000)))
      case SectionTypeFileList => Map("items" -> FieldChecks.fromValidator(FileSectionValidator()))
    }

  override def saveItem(id: ApplicationId, sectionNumber: AppSectionNumber, doc: JsObject): Future[FieldErrors] = {
    val item = (doc \ "item").toOption.flatMap(_.validate[JsObject].asOpt).getOrElse(JsObject(Seq()))
    item \ "itemNumber" match {
      case JsDefined(JsNumber(itemNumber)) => {
         put(urls.item(id, sectionNumber, itemNumber.toInt), item).map(_ => List())
      }
      case _ => {
         post(urls.items(id, sectionNumber), item).map(_ => List())
      }
    }
  }

  override def saveFileItem(id: ApplicationId, sectionNumber: AppSectionNumber, doc: JsObject): Future[Int] = {
    val item = (doc \ "item").toOption.flatMap(_.validate[JsObject].asOpt).getOrElse(JsObject(Seq()))
    postFile(urls.fileitems(id, sectionNumber), item)
  }

  override def deleteItem(id: ApplicationId, sectionNumber: AppSectionNumber, itemNumber: Int): Future[Unit] =
    delete(urls.item(id, sectionNumber, itemNumber))

  override def getItem[T: Reads](id: ApplicationId, sectionNumber: AppSectionNumber, itemNumber: Int): Future[Option[T]] =
    getOpt[T](urls.item(id, sectionNumber, itemNumber))

  override def getSection(id: ApplicationId, sectionNumber: AppSectionNumber): Future[Option[ApplicationSection]] =
    getOpt[ApplicationSection](urls.section(id, sectionNumber))

  override def getSections(id: ApplicationId): Future[Seq[ApplicationSection]] =
    getMany[ApplicationSection](urls.sections(id))

  override def getOrCreateForForm(applicationFormId: ApplicationFormId): Future[Option[Application]] =
    getOpt[Application](appFormUrls.application(applicationFormId))

  override def overview(id: ApplicationId): Future[Option[ApplicationOverview]] =
    getOpt[ApplicationOverview](urls.application(id))

  override def detail(id: ApplicationId): Future[Option[ApplicationDetail]] =
    getOpt[ApplicationDetail](urls.detail(id))

  override def sectionDetail(id: ApplicationId, sectionNumber: AppSectionNumber): Future[Option[ApplicationSectionDetail]] =
    getOpt[ApplicationSectionDetail](urls.sectionDetail(id, sectionNumber))

  override def reset(): Future[Unit] =
    post(urls.reset, "")

  override def deleteSection(id: ApplicationId, sectionNumber: AppSectionNumber): Future[Unit] =
    delete(urls.section(id, sectionNumber))

  override def clearSectionCompletedDate(id: ApplicationId, sectionNumber: AppSectionNumber): Future[Unit] =
    put(urls.markNotCompleted(id, sectionNumber), "")

  override def submit(id: ApplicationId): Future[Option[SubmittedApplicationRef]] =
    postWithResult[SubmittedApplicationRef, String](urls.submit(id), "")

  override def updatePersonalReference(id: ApplicationId, reference: String) =
    post(urls.personalRef(id), reference)

  override def uploadFile(fileName: String, fileType: String,  mf: MultipartFormData.FilePart[TemporaryFile]): Future[Unit] = {

    //play.api.mvc.MultipartFormData.FilePart[Source[ByteString, Any]]
    /*  This is for upload a file to backend Using REST API */

    val dataParts = Map(
      "filename" -> Seq("Test"),
      "filetype" -> Seq("pdf")
    )

    //val file = new java.io.File("/Users/venkatamutyala/test/test.txt")
    //val fileParts = new play.api.mvc.MultipartFormData.FilePart("attachment", "foo.jpg", Some("image/jpeg"), file)
    //val multipartFormData = MultipartFormData(dataParts, fileParts, Seq(), Seq())

    //val multipartFormData = MultipartFormData(dataParts, fileParts, Seq(), Seq())
    //ws.url("").post(Source(fileParts :: dataParts :: List()))
    //ws.url("").post(Source(play.api.mvc.MultipartFormData.FilePart("hello", "hello.txt", Option("text/plain"), FileIO.fromFile(tmpFile)) :: DataPart("key", "value") :: List()))
    //WS.url(url).post(multipartFormData)
    // /////post(urls.uploadFile(), "")
    //post(urls.uploadFile(), Source(fileParts :: dataParts :: List()))
    //-------------
    //val source: String = Source.fromFile("/Users/venkatamutyala/test/test.txt").getLines.mkString
    //val json: JsValue = Json.parse(source)
    //------------
    //val stream = new FileInputStream(file)

    //val json = try {  Json.parse(stream) } finally { stream.close() }
    //println("JSon:-" + Json.stringify (json))

    //val model = Source fromInputStream{ getClass getResourceAsStream "/Users/venkatamutyala/test/test.txt" } getLines() toString()
    //println("Json:-" + Json.toJson(file))
    //ws.url("").post(Source(FilePart("hello", "hello.txt", Option("text/plain"), FileIO.fromFile(file)) :: DataPart("key", "value") :: List()))
    //ws.url("").post(mf)
    //ws.url("").post()
    //ws.url("").post(Source.single(mf))

    // post(urls.uploadFile(), Source.single(fileParts))
    post(urls.uploadFile(), "")
    //
  }
}
