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

class ContactValidatorSpec extends WordSpecLike with Matchers {
  "Contact field validator" should {
    "reject a missing value of Email" in {
      val contactValues = ContactValues(Some("02080000000"), None, None, None)
      //ContactValidator.validate("contact", contactValues) shouldBe  an[Invalid[_]]
      ContactValidator.validate("contact", contactValues).map{ e =>
      }.leftMap{err =>
        err.head.err shouldBe "'email' cannot be empty"
      } shouldBe  an[Invalid[_]]
    }

    "reject a length of Email exdeeds Specified number of characters" in {
      val email = "abcdefijklmnopqrstuvwxyzabcdefijklmnopqrstuvwxyzabcdefijklmnopqrstuvwxyzabcdefijklmnopqrstuvwxyzabcdefijklmnopqrstuvwxyz" +
        "@" +
        "abcdefijklmnopqrstuvwxyzabcdefijklmnopqrstuvwxyzabcdefijklmnopqrstuvwxyzabcdefijklmnopqrstuvwxyzabcdefijklmnopqrstuvwxyz" +
        ".com"
      val contactValues = ContactValues(Some("02080000000"), Some(email), None, None)
      ContactValidator.validate("contact", contactValues).map{ e =>
      }.leftMap{err =>
        err.head.err shouldBe "Character limit exceeded"
      } shouldBe  an[Invalid[_]]
    }

    "approve an existing of Email" in {
      val email = "admin@beis.gov.uk"
      val contactValues = ContactValues(Some("02080000000"), Some(email), None, None)
      ContactValidator.validate("contact", contactValues).leftMap{ e =>
        fail("Character limit exceeded")
      }
    }

    "approve a length of Email that is under Specified number of characters" in {
      val email = "admin@beis.gov.uk"
      val contactValues = ContactValues(Some("02080000000"), Some(email), None, None)
      ContactValidator.validate("contact", contactValues) shouldBe an[Valid[_]]
    }

    "reject a missing value of Telephone" in {
      val email = "admin@beis.gov.uk"
      val contactValues = ContactValues(None, Some(email), None, None)
      ContactValidator.validate("contact", contactValues).map{ e =>
      }.leftMap{err =>
        err.head.err shouldBe "'telephone' cannot be empty"
      } shouldBe  an[Invalid[_]]
    }

    "reject a length of Telephone exdeeds Specified number of characters" in {
      val email = "admin@beis.gov.uk"
      val telephone = "123456789012345678901234567890"
      val contactValues = ContactValues(Some(telephone), Some(email), None, None)
      ContactValidator.validate("contact", contactValues).map{ e =>
      }.leftMap{err =>
        err.head.err shouldBe "Character limit exceeded"
      } shouldBe  an[Invalid[_]]
    }

    "approve an existing of Telephone" in {
      val email = "admin@beis.gov.uk"
      val telephone = "123456789012345"
      val contactValues = ContactValues(Some(telephone), Some(email), None, None)
      ContactValidator.validate("contact", contactValues).leftMap{ e =>
        fail("Character limit exceeded")
      }
    }

    "approve a length of Telephone that is inside Specified number of characters range" in {
      val email = "admin@beis.gov.uk"
      val telephone = "123456789012345"
      val contactValues = ContactValues(Some(telephone), Some(email), None, None)
      ContactValidator.validate("contact", contactValues) shouldBe an[Valid[_]]
    }

  }
}
