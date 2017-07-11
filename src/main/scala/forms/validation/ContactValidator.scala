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

package forms.validation

import cats.data.ValidatedNel
import cats.syntax.cartesian._
import cats.syntax.validated._
import config.Config
import forms.TextField
import forms.validation.FieldValidator.Normalised

case class ContactValues(telephone: Option[String], email: Option[String], web: Option[String] = None, twitter: Option[String] = None)

case class Contact(telephone: String, email: String, web: String, twitter: String)

 object ContactValidator  {
  def apply(textfields : Seq[TextField]) = {
    println("== ContactValidator in ContactValidator=======:-"+ textfields)
    new ContactValidator(textfields)
  }
}

 class ContactValidator(textfields : Seq[TextField]) extends FieldValidator[ContactValues, Contact]{

  override def doValidation(path: String, contactValues: Normalised[ContactValues]): ValidatedNel[FieldError, Contact] = {

    def createValidator(f : String) = {
    val textfield = textfields.filter(t => t.name == s"$path.$f")
    textfield.head.isMandatory match {
      case true => MandatoryValidator(Some(f)).andThen(CharacterCountValidator(textfield.head.maxWords))
      case false => NonMandatoryValidator(None)
    }
  }

  val telephoneValidator = createValidator("telephone").validate(s"$path.telephone", contactValues.telephone)

  val emailValidator = createValidator("email").validate(s"$path.email", contactValues.email)

  val webValidator = createValidator("web").validate(s"$path.web", contactValues.web)
  val twitterValidator = createValidator("twitter").validate(s"$path.twitter", contactValues.twitter)

    (telephoneValidator |@| emailValidator |@| webValidator |@| twitterValidator).map(Contact.apply(_, _, _, _))
  }

}