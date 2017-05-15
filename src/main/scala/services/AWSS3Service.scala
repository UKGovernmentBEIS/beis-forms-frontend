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

import java.io.File
import java.net.URL

import akka.stream.javadsl.StreamConverters
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.{GetObjectRequest, S3ObjectInputStream, S3ObjectSummary}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.google.inject.Inject
import config.Config
import controllers.FieldCheckHelpers.FieldErrors
import forms.validation.FieldError
import models._
import org.apache.commons.io.{FileUtils, IOUtils}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConversions._

class AWSS3Service @Inject()(implicit val ec: ExecutionContext)
  extends AWSOps {

  val baseUrl = Config.config.business.baseUrl
  val urls = new ApplicationURLs(baseUrl)
  val appFormUrls = new ApplicationFormURLs(baseUrl)

  val accesskey = Config.config.aws.accesskey
  val secretkey = Config.config.aws.secretkey
  val region = Config.config.aws.region
  val bucket = Config.config.aws.bucket

  val creds:BasicAWSCredentials = new BasicAWSCredentials(accesskey, secretkey)
  val s3Client:AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(new AWSStaticCredentialsProvider(creds)).build()


  //TODO:- need to change the FieldErrors to General Errors
  override def upload(key: ResourceKey, file : File):  Future[FieldErrors] = {

    Try(s3Client.putObject(bucket, key.key, file)) match{
      case Success(result) => Future.successful( List())
      case Failure(e) => {
        /*This will out put complete error stack
        val flderrs = e.getStackTrace.toList.map( er => FieldError("item", er.toString))*/
        val flderrs = List(FieldError("item1", "File Upload Error: "),
                           FieldError("item2", e.getMessage))
        Future.successful(flderrs)
      }
    }
  }

  override def download(key: ResourceKey):  Future[FieldErrors] = {

    Try(s3Client.getObject(bucket, key.key)) match{
      case Success(result) => {
        val stream:S3ObjectInputStream = result.getObjectContent()
        val filedownloaddirectory = Config.config.file.filedownloaddirectory
        val tmpFile: File = new File(filedownloaddirectory + key.key)
        val byteArray = IOUtils.toByteArray(stream)
        FileUtils.writeByteArrayToFile(tmpFile, byteArray)
        Future.successful( List())
      }
      case Failure(e) => {
        val flderrs = List(FieldError("item1", "File Download Error: "),
          FieldError("item2", e.getMessage))
        Future.successful(flderrs)
      }
    }
  }

  override def downloadDirect(key: ResourceKey):  Future[URL] = {

    Try(s3Client.generatePresignedUrl(bucket, key.key, null)) match{
      case Success(result) => Future.successful( result)
      case Failure(e) => Future.successful(null)
    }
  }

  override def delete(key: ResourceKey):  Future[FieldErrors] = {
    Try(s3Client.deleteObject(bucket, key.key)) match{
      case Success(result) => Future.successful( List())
      case Failure(e) => {
         val flderrs = List(FieldError("item1", "File Delete Error: "),
                           FieldError("item2", e.getMessage))
        Future.successful(flderrs)
      }
    }
  }

  /** This functionality is not implmented yet **/
  override def listBuckets():Unit= {
     val result1 = s3Client.listObjects(bucket)
     val objectSummaries:Seq[S3ObjectSummary] = result1.getObjectSummaries
     objectSummaries.map(a=> println("File:-" + a.getKey))
     }

}
