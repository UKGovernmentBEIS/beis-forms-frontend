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

import cats.data.Validated.{Invalid, Valid}
import org.scalatest.{Matchers, WordSpecLike}

class CompanyInfoValidatorSpec extends WordSpecLike with Matchers {
  "Contact field validator" should {
    "reject a missing value of Email" in {
      val companyInfoValues = CompanyInfoValues(None, None)
      CompanyInfoValidator.validate("companyname", companyInfoValues).map{ e =>
      }.leftMap{err =>
        err.head.err shouldBe "'companyname' cannot be empty"
      } shouldBe  an[Invalid[_]]
    }

    "reject a length of Email exdeeds Specified number of characters" in {
      val companyname ="abcdefijklmnopqrstuvwxyzabcdefijklmnopqrstuvwxyzabcdefijklmnopqrstuvwxyzabcdefijklmnopqrstuvwxyzabcdefijklmnopqrstuvwxyz" +
        "abcdefijklmnopqrstuvwxyzabcdefijklmnopqrstuvwxyzabcdefijklmnopqrstuvwxyzabcdefijklmnopqrstuvwxyzabcdefijklmnopqrstuvwxyz" +
        "LLC"
      val companyInfoValues = CompanyInfoValues(Some(companyname), None)
      CompanyInfoValidator.validate("companyname", companyInfoValues).map{ e =>
      }.leftMap{err =>
        err.head.err shouldBe "Character limit exceeded"
      } shouldBe  an[Invalid[_]]
    }

    "approve an existing of Email" in {
      val companyname = "Department for Business, Energy & Industrial Strategy"
      val companyInfoValues = CompanyInfoValues(Some(companyname), None)
      CompanyInfoValidator.validate("companyname", companyInfoValues).leftMap{ e =>
        fail("Character limit exceeded")
      }
    }

    "approve a length of Email that is inside Specified number of characters range" in {
      val companyname = "Department for Business, Energy & Industrial Strategy"
      val companyInfoValues = CompanyInfoValues(Some(companyname), None)
      CompanyInfoValidator.validate("companyname", companyInfoValues) shouldBe an[Valid[_]]
    }

  }
}
