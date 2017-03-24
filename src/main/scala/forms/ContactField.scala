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
import forms.validation._
import models._
import play.api.libs.json.{JsObject, Json}

case class ContactField(name: String) extends Field {
  implicit val contactReads = Json.reads[ContactValues]

  val telephoneField = TextField(Some("Telephone"), s"$name.telephone", isNumeric = false, 20)
  val emailField = TextField(Some("Email Id"), s"$name.email", isNumeric = false, 20)
  val webField = TextField(Some("Web Address"), s"$name.web", isNumeric = false, 20)
  val twitterField = TextField(Some("Twitter"), s"$name.twitter", isNumeric = false, 20)

  override def previewCheck: FieldCheck = FieldChecks.mandatoryCheck
  override def check: FieldCheck = FieldChecks.fromValidator(ContactValidator)

  override def renderPreview(questions: Map[String, Question], answers: JsObject) =
    views.html.renderers.preview.contactField(this, JsonHelpers.flatten(answers))

  override def renderFormInput(questions: Map[String, Question], answers: JsObject, errs: Seq[FieldError], hints: Seq[FieldHint]) = {
    views.html.renderers.contactField(this, questions, JsonHelpers.flatten(answers), errs, hints)
  }
}
