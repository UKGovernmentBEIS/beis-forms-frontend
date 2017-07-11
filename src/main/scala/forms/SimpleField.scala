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

import controllers.manage._
import controllers.{FieldCheck, FieldChecks, JsonHelpers}
import forms.validation._
import models.Question
import play.api.libs.json.JsObject
import play.api.libs.json.Json
case class SimpleField(label: Option[String], name: String, isEnabled: Boolean, isMandatory: Boolean, isNumeric: Boolean, maxWords: Int, fieldType: String) extends Field {
  val validator = MandatoryValidator(label).andThen(CharacterCountValidator(maxWords))

  implicit val simpleFieldReads = Json.format[SimpleField]
  implicit val textFieldReads = Json.format[TextField]
  implicit val textAreaFieldReads = Json.format[TextAreaField]


  override val check: FieldCheck = isMandatory match {
    case true => FieldChecks.fromValidator(validator)
    case false => FieldChecks.noCheck
  }

  override val previewCheck: FieldCheck = FieldChecks.mandatoryCheck

  override def renderFormInput(questions: Map[String, Question], answers: JsObject, errs: Seq[FieldError], hints: Seq[FieldHint]) =

   fieldType match {
      case "text" =>
        views.html.renderers.textField(Json.toJson(this).validate[TextField].get, questions, JsonHelpers.flatten(answers), errs, hints)

      case "textArea" =>
        views.html.renderers.textAreaField(Json.toJson(this).validate[TextAreaField].get, questions, JsonHelpers.flatten(answers), errs, hints)
   }

    override def renderPreview(questions: Map[String, Question], answers: JsObject) =
      fieldType match {
        case "text" =>
          views.html.renderers.preview.textField(Json.toJson(this).validate[TextField].get, JsonHelpers.flatten(answers))
        case "textArea" =>
          views.html.renderers.preview.textAreaField(Json.toJson(this).validate[TextAreaField].get, JsonHelpers.flatten(answers))
      }

}
