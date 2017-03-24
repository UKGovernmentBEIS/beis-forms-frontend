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
import forms.validation.FieldValidator.Normalised

case class ContactValues(telephone: Option[String], email: Option[String], web: Option[String] = None, twitter: Option[String] = None)

case class Contact(telephone: String, email: String, web: Option[String] = None, twitter: Option[String] = None)

case object ContactValidator extends FieldValidator[ContactValues, Contact] {
  val telephoneValidator = MandatoryValidator(Some("telephone")).andThen(WordCountValidator(200))
  //val emailValidator = CurrencyValidator.anyValue
  val emailValidator = MandatoryValidator(Some("email")).andThen(WordCountValidator(200))
  val webValidator = MandatoryValidator(Some("web")).andThen(WordCountValidator(200))
  //val twitterValidator = MandatoryValidator(Some("twitter")).andThen(WordCountValidator(200))

  override def doValidation(path: String, contactValues: Normalised[ContactValues]): ValidatedNel[FieldError, Contact] = {
    val telephoneV = telephoneValidator.validate(s"$path.telephone", contactValues.telephone)
    val emailV = emailValidator.validate(s"$path.email", contactValues.email)

    (telephoneV |@| emailV).map(Contact.apply(_, _, None, None))
  }

  override def doHinting(path: String, contactValues: Normalised[ContactValues]): List[FieldHint] = {
    emailValidator.hintText(s"$path.email", contactValues.email)
  }
}

case class ContactSectionValidator(maxValue: BigDecimal) extends FieldValidator[CostList, List[CostItem]] {
  val nonEmptyV = new FieldValidator[List[CostItem], List[CostItem]] {
    override def doValidation(path: String, items: Normalised[List[CostItem]]): ValidatedNel[FieldError, List[CostItem]] =
      if (items.isEmpty) FieldError(path, s"Must provide at least one item.").invalidNel
      else items.validNel
  }

  val notTooCostlyV = new FieldValidator[List[CostItem], List[CostItem]] {
    override def doValidation(path: String, items: Normalised[List[CostItem]]): ValidatedNel[FieldError, List[CostItem]] =
      if (items.map(_.cost).sum > maxValue) FieldError(path, s"Total requested exceeds limit. Please check costs of items.").invalidNel
      else items.validNel
  }

  override def doValidation(path: String, cvs: Normalised[CostList]): ValidatedNel[FieldError, List[CostItem]] = {
    nonEmptyV.andThen(notTooCostlyV).validate("", cvs.items)
  }
}