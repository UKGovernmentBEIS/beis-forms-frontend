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

package forms

import cats.data.ValidatedNel
import controllers.{FieldCheck, FieldChecks, JsonHelpers}
import forms.validation.FieldValidator.Normalised
import forms.validation.{FieldError, FieldHint, FieldValidator}
import models._
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import cats.data.ValidatedNel
import cats.syntax.cartesian._
import cats.syntax.validated._
import forms.validation.FieldValidator.Normalised


case class FileUploadField(label: Option[String], name: String, isEnabled: Boolean, isMandatory: Boolean, maxWords: Int) extends Field {

  implicit val fileuploadReads = Json.reads[FileUploadItem]

  override val check: FieldCheck = isMandatory match {
    //case true => FieldChecks.mandatoryCheck //Need to enable this incase atleast one file is mandatory
    case true => FieldChecks.noCheck
    case false => FieldChecks.noCheck
  }

  override def previewCheck: FieldCheck = FieldChecks.mandatoryCheck

  override def renderPreview(questions: Map[String, Question], answers: JsObject) = {

    val itemValues: Seq[JsValue] = (answers \ "items").validate[JsArray].asOpt.map(_.value).getOrElse(Seq())
    val fileUploadItems = itemValues.flatMap(_.validate[FileUploadItem].asOpt)
    views.html.renderers.preview.fileUpload(fileUploadItems)
  }

  override def renderFormInput(questions: Map[String, Question], answers: JsObject, errs: Seq[FieldError], hints: Seq[FieldHint]) = {

    val itemValues: Seq[JsValue] = (answers \ "items").validate[JsArray].asOpt.map(_.value).getOrElse(Seq())
    val fileUploadItems = itemValues.flatMap(_.validate[FileUploadItem].asOpt)
    views.html.renderers.fileUpload(this, questions, fileUploadItems, errs, hints)
  }
}

case class FileSectionValidator() extends FieldValidator[FileList, List[FileUploadItem]] {

    val nonEmptyV = new FieldValidator[List[FileUploadItem], List[FileUploadItem]] {

    override def doValidation(path: String, items: Normalised[List[FileUploadItem]]): ValidatedNel[FieldError, List[FileUploadItem]] = {

      if (items.isEmpty) FieldError(path, s"Must provide at least one item.").invalidNel
      else items.validNel
    }
  }

  override def doValidation(path: String, cvs: Normalised[FileList]): ValidatedNel[FieldError, List[FileUploadItem]] = {
      nonEmptyV.validate("", cvs.items)
  }
}

case class FileUploadItem(supportingDocuments: String, itemNumber: Option[Int] = None)
case class FileList(items: List[FileUploadItem])
