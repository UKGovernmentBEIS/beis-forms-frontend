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

case class SimpleFormValues(employeename: Option[String]=None, department: Option[String], natureofillness: Option[String],
                             managername: Option[String], manageremail: Option[String])
case class SimpleForm(employeename: String, department: String, natureofillness: String,
                            managername: String, manageremail: String)


case object SimpleFormValidator extends FieldValidator[SimpleFormValues, SimpleForm] {
 // val companynamelength = Config.config.fieldvalidation.email
  val employeenameValidator = MandatoryValidator(Some("employeename")).andThen(CharacterCountValidator(20))
  val departmentValidator = MandatoryValidator(Some("department")).andThen(CharacterCountValidator(20)) //not  used
  val natureofillnessValidator = MandatoryValidator(Some("natureofillness")).andThen(CharacterCountValidator(20)) //not  used

  val managernameValidator = MandatoryValidator(Some("managername")).andThen(CharacterCountValidator(20))
  val manageremailValidator = MandatoryValidator(Some("manageremail")).andThen(CharacterCountValidator(200)) //not  used

  override def doValidation(path: String, simpleFormValues: Normalised[SimpleFormValues]): ValidatedNel[FieldError, SimpleForm] = {
    val employeenameValidatorV = employeenameValidator.validate(s"$path.employeename", simpleFormValues.employeename)
    val departmentValidatorV = departmentValidator.validate(s"$path.department", simpleFormValues.department)
    val natureofillnessValidatorV = natureofillnessValidator.validate(s"$path.natureofillness", simpleFormValues.natureofillness)
    val managernameV = managernameValidator.validate(s"$path.managername", simpleFormValues.managername)
    val manageremailV = manageremailValidator.validate(s"$path.manageremail", simpleFormValues.manageremail)

    (employeenameValidatorV |@| departmentValidatorV |@| natureofillnessValidatorV |@|  managernameV |@| manageremailV).map(SimpleForm(_,_,_,_,_))
  }
}


/*class SimpleFormValidator(textfields : Seq[TextField]) extends FieldValidator[ContactValues, Contact]{

  override def doValidation(path: String, contactValues: Normalised[ContactValues]): ValidatedNel[FieldError, Contact] = {
    println("== SimpleFormValidator doValidation textfields=======:-"+ textfields)
    println("== SimpleFormValidator doValidation contactValues=======:-"+ contactValues)

    def createValidator(f : String) = {
      val textfield = textfields.filter(t => t.name == s"$path.$f")
      textfield.head.isMandatory match {
        case true => {
          println("=============== SimpleFormValidator createValidator 000=======:-"+ f)
          println("=============== SimpleFormValidator createValidator 000=======:-"+ f)

          MandatoryValidator(Some(f)).andThen(CharacterCountValidator(textfield.head.maxWords))
        }
        case false => NonMandatoryValidator(None)
      }
    }

    val telephoneValidator = {
      println("== SimpleFormValidator in telephoneValidator=======")
      createValidator("telephone").validate(s"$path.telephone", contactValues.telephone)
    }
    val emailValidator = {
      println("== SimpleFormValidator in emailValidator=======")
      createValidator("email").validate(s"$path.email", contactValues.email)
    }
    val webValidator = createValidator("web").validate(s"$path.web", contactValues.web)
    val twitterValidator = createValidator("twitter").validate(s"$path.twitter", contactValues.twitter)

    println("== SimpleFormValidator in before telephoneValidator |@| emailValidator =======:-")

    (telephoneValidator |@| emailValidator |@| webValidator |@| twitterValidator).map(Contact.apply(_, _, _, _))
  }

}*/