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

package services

import io.findify.s3mock.S3Mock
import org.scalatest._
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client, AmazonS3ClientBuilder}
import scala.language.reflectiveCalls

/**
  * Created by venkatamutyala on 09/05/2017.
  */

class AWSS3Test extends WordSpecLike with Matchers with BeforeAndAfterAll{
  override def beforeAll() {
    val api = S3Mock(port = 8001, dir = "/tmp/s3")
    api.start
  }

  def fixture =
    new {
      val credentials = new AnonymousAWSCredentials()
      implicit val client:AmazonS3 = AmazonS3ClientBuilder.standard()
        .withEndpointConfiguration(new EndpointConfiguration("http://127.0.0.1:8001", ""))
        .build()
   }

  override def afterAll() {
    fixture.client.deleteBucket("testbucket")
  }

  "AWS S3" should {
    "upload files with basic credentials" in {
      fixture.client.createBucket("testbucket")
      fixture.client.putObject("testbucket", "testfile", "Test Text")
    }

    "download file of given key (file name)" in {
      fixture.client.getObject("testbucket", "testfile").getKey shouldBe "testfile"
    }

    "delete file of given key (file name)" in {
      fixture.client.deleteObject("testbucket", "testfile")
      an [AmazonS3Exception] should be thrownBy fixture.client.getObject("testbucket", "testfile").getKey
    }
  }
}


