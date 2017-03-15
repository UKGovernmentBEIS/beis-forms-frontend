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

package controllers

import play.api.libs.Files.TemporaryFile
import play.api.libs.json.JsObject
import play.api.mvc.{BodyParser, BodyParsers, MultipartFormData}

case class JsonFormSubmit(mf: MultipartFormData[TemporaryFile], action: ButtonAction, values: JsObject)

object JsonForm {

  import JsonHelpers._

  /**
    * Note if more than one button action name is present in the keys then it is indeterminate as to
    * which one will be returned. This shouldn't occur if the form is properly submitted from a
    * browser, though.
    */
  def decodeButton(keys: Set[String]): Option[ButtonAction] = keys.flatMap(ButtonAction.unapply).headOption.map {
    case Save => if (keys.contains("_complete_checkbox")) Complete else Save
    case Preview => if (keys.contains("_complete_checkbox")) completeAndPreview else Preview
    case b => b
  }

  import play.api.libs.iteratee.Execution.Implicits.trampoline

  def parser: BodyParser[JsonFormSubmit] = BodyParsers.parse.urlFormEncoded.map { params =>
    // Drop keys that start with '_' as these are "system" keys like the button name
    val jsonFormValues = formToJson(params.filterKeys(k => !k.startsWith("_")))
    val button: Option[ButtonAction] = decodeButton(params.keySet)
    button.map(b => JsonFormSubmit(null,b, jsonFormValues)).getOrElse(sys.error("Could not find an action button on the form post"))
  }

  def fileuploadparser: BodyParser[JsonFormSubmit] = BodyParsers.parse.multipartFormData.map { params =>
    // Drop keys that start with '_' as these are "system" keys like the button name
    val pm = params.dataParts
    //request.body.dataParts.map
    val jsonFormValues = formToJson(pm.filterKeys(k => !k.startsWith("_")))
    val button: Option[ButtonAction] = decodeButton(pm.keySet)
    button.map(b => JsonFormSubmit(params, b, jsonFormValues)).getOrElse(sys.error("Could not find an action button on the form post"))
  }
}
