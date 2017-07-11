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
import forms.validation.{ContactValidator, _}
import models._
import play.api.libs.json._

case class SimpleFormField(name: String, simpleform : Seq[SimpleField]) extends Field {
  implicit val simpleFormReads = Json.reads[SimpleFormValues]
  //println("===contactitems==="+ contactitems)
  //  val telephoneField = sicknessform.filter(s => (s.name == "sicknessAbsence.telephone")).head
  //  val emailField = sicknessform.filter(s => (s.name == "sicknessAbsence.email")).head
  //  val webField = sicknessform.filter(s => (s.name == "sicknessAbsence.web")).head
  //  val twitterField = sicknessform.filter(s => (s.name == "sicknessAbsence.twitter")).head

  //  println("===telephoneField==="+ telephoneField)


  //override def check: FieldCheck = FieldChecks.fromValidator(ContactValidator)
  //override def check: FieldCheck = FieldChecks.fromValidator(new ContactValidator(Seq(telephoneField, emailField, webField, twitterField)))
  //override def check: FieldCheck = FieldChecks.noCheck

  override def check: FieldCheck = {
    //println("== ContactField case true "+name + " ======= " +  contactitems)

    //FieldChecks.fromValidator(new SimpleFormValidator(Seq(telephoneField, emailField, webField, twitterField)))
    FieldChecks.fromValidator(SimpleFormValidator)
  }

  override def previewCheck: FieldCheck = FieldChecks.mandatoryCheck

  override def renderPreview(questions: Map[String, Question], answers: JsObject) = {
    views.html.renderers.preview.simpleFormField(this, JsonHelpers.flatten(answers))
  }

  override def renderFormInput(questions: Map[String, Question], answers: JsObject, errs: Seq[FieldError], hints: Seq[FieldHint]) = {

    views.html.renderers.simpleFormField(this, questions, JsonHelpers.flatten(answers), errs, hints)
  }
}
