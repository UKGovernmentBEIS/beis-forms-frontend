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

case class CompanyInfoValues(companyname: Option[String], companynumber: Option[String] = None)

case class CompanyInfo(companyname: String, companynumber: Option[String] = None)

case object CompanyInfoValidator extends FieldValidator[CompanyInfoValues, CompanyInfo] {
  val companynamelength = Config.config.fieldvalidation.email
  val companynameValidator = MandatoryValidator(Some("companyname")).andThen(CharacterCountValidator(companynamelength))
  val companynumberValidator = MandatoryValidator(Some("companynumber")).andThen(CharacterCountValidator(200)) //not  used

  override def doValidation(path: String, companyInfoValues: Normalised[CompanyInfoValues]): ValidatedNel[FieldError, CompanyInfo] = {
    val companynameV = companynameValidator.validate(s"$path.companyname", companyInfoValues.companyname)
    val companynumberV = companynumberValidator.validate(s"$path.companynumber", companyInfoValues.companynumber)

    (companynameV).map(CompanyInfo.apply(_))
  }

}
