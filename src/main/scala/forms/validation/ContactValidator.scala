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
import forms.validation.FieldValidator.Normalised

case class ContactValues(telephone: Option[String], email: Option[String], web: Option[String] = None, twitter: Option[String] = None)

case class Contact(telephone: String, email: String, web: Option[String] = None, twitter: Option[String] = None)

case object ContactValidator extends FieldValidator[ContactValues, Contact] {
  val telephonelength = Config.config.fieldvalidation.telephone
  val emaillength = Config.config.fieldvalidation.email

  val telephoneValidator = MandatoryValidator(Some("telephone")).andThen(CharacterCountValidator(telephonelength))
  //val emailValidator = CurrencyValidator.anyValue
  val emailValidator = MandatoryValidator(Some("email")).andThen(CharacterCountValidator(emaillength))
  val webValidator = MandatoryValidator(Some("web")).andThen(CharacterCountValidator(200))
  //val twitterValidator = MandatoryValidator(Some("twitter")).andThen(CharacterCountValidator(200))

  override def doValidation(path: String, contactValues: Normalised[ContactValues]): ValidatedNel[FieldError, Contact] = {
    val telephoneV = telephoneValidator.validate(s"$path.telephone", contactValues.telephone)
    val emailV = emailValidator.validate(s"$path.email", contactValues.email)

    (telephoneV |@| emailV).map(Contact.apply(_, _, None, None))
  }

  override def doHinting(path: String, contactValues: Normalised[ContactValues]): List[FieldHint] = {
    emailValidator.hintText(s"$path.email", contactValues.email)
  }
}