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
import cats.syntax.validated._
import forms.validation.FieldValidator.Normalised

case class CharacterCountValidator(maxChars: Int) extends FieldValidator[String, String] {

  import CharacterCountValidator._

  override def normalise(s: String): String = s.trim()

  override def doValidation(path: String, s: Normalised[String]): ValidatedNel[FieldError, String] = {
  s match {
      case n if n.length > maxChars => FieldError(path, s"'${path.substring(path.indexOf('.')+1,path.length)}' Character limit exceeded").invalidNel
      case n => n.validNel
    }
  }

  override def doHinting(path: String, s: Normalised[String]): List[FieldHint] = {
    val charCount:Int = s.length
    val text = charCount match {
      case 0 => noChars(maxChars)
      case _ if charCount > maxChars => overLimit(charCount - maxChars)
      case _  => ""
    }

    List(FieldHint(path, text, Some("CharCount"), Some(s"""{\"maxWords\": $maxChars}""")))
  }
}

object CharacterCountValidator {
  def overLimit(over: Int) = s"$over Characters over limit"
  def noChars(max: Int) = s"$max Characters maximum"
}
