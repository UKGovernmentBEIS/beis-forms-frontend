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

import controllers.{FieldCheck, FieldChecks, JsonHelpers}
import forms.validation.{FieldError, FieldHint}
import models._
import play.api.libs.json.JsObject

case class AddressField(label: Option[String], name: String, isEnabled: Boolean, isMandatory: Boolean, maxWords: Int) extends Field {

  override val check: FieldCheck = isMandatory match {
    case true => FieldChecks.mandatoryText(maxWords)
    case false => FieldChecks.noCheck
  }

  override def previewCheck: FieldCheck = FieldChecks.mandatoryCheck

  override def renderPreview(questions: Map[String, Question], answers: JsObject) =
    views.html.renderers.preview.addressField(this, JsonHelpers.flatten(answers))

  override def renderFormInput(questions: Map[String, Question], answers: JsObject, errs: Seq[FieldError], hints: Seq[FieldHint]) = {
    views.html.renderers.addressField(this, questions, JsonHelpers.flatten(answers), errs, hints)
  }
}
