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

import javax.inject.Inject

import config.Config
import forms.FileUploadItem
import forms.validation.CostItem
import models._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import services.{ApplicationFormOps, ApplicationOps, OpportunityOps, BusinessProcessOps}

import scala.concurrent.{ExecutionContext, Future}

class ActionHandler @Inject()(applications: ApplicationOps, applicationForms: ApplicationFormOps, opportunities: OpportunityOps,
                              processes: BusinessProcessOps)(implicit ec: ExecutionContext)
  extends ApplicationResults {

  import ApplicationData._
  import FieldCheckHelpers._

  def doSave(app: ApplicationSectionDetail, fieldValues: JsObject): Future[Result] = {
    app.formSection.sectionType match {
      case SectionTypeForm => {
        if (JsonHelpers.allFieldsEmpty(fieldValues)) applications.deleteSection(app.id, app.sectionNumber)
        else applications.saveSection(app.id, app.sectionNumber, fieldValues)
      }.map(_ => redirectToOverview(app.id))
      case SectionTypeCostList => Future.successful(redirectToOverview(app.id))
      case SectionTypeFileList => Future.successful(redirectToOverview(app.id))
    }
  }

  def doComplete(app: ApplicationSectionDetail, fieldValues: JsObject): Future[Result] = {
    val answers = app.formSection.sectionType match {
      case SectionTypeForm => fieldValues
      // Instead of using the values that were passed in from the form we'll use the values that
      // have already been saved against the item list, since these were created by the add-item
      // form.
      case SectionTypeCostList => app.section.map(_.answers).getOrElse(JsObject(Seq()))
      case SectionTypeFileList => app.section.map(_.answers).getOrElse(JsObject(Seq()))
    }

    applications.completeSection(app.id, app.sectionNumber, answers).map {
      case Nil => redirectToOverview(app.id)
      case errs => redisplaySectionForm(app, answers, errs)
    }
  }

  def doSaveItem(app: ApplicationSectionDetail, fieldValues: JsObject): Future[Result] = {
    JsonHelpers.allFieldsEmpty(fieldValues) match {
      case true => applications.deleteSection(app.id, app.sectionNumber).map(_ => redirectToOverview(app.id))
      case false => applications.saveItem(app.id, app.sectionNumber, fieldValues).flatMap {
        case Nil => Future.successful(redirectToOverview(app.id))
        case errs => Future.successful(redisplaySectionForm(app, fieldValues, errs))
      }
    }
  }

  def doSaveFileItem(app: ApplicationSectionDetail, fieldValues: JsObject): Future[Result] = {
    JsonHelpers.allFieldsEmpty(fieldValues) match {
      case true => applications.deleteSection(app.id, app.sectionNumber).map(_ => redirectToOverview(app.id))
      case false => applications.saveFileItem(app.id, app.sectionNumber, fieldValues).flatMap {
        case itemnumber => Future.successful(redirectToOverview(app.id))
        //case errs => Future.successful(redisplaySectionForm(app, fieldValues, errs))
      }
    }
  }

  def doPreview(app: ApplicationSectionDetail, fieldValues: JsObject): Future[Result] = {
    app.formSection.sectionType match {
      case SectionTypeForm =>
        val errs = check(fieldValues, previewChecksFor(app.formSection))
        if (errs.isEmpty) applications.saveSection(app.id, app.sectionNumber, fieldValues).map(_ => redirectToPreview(app.id, app.sectionNumber))
        else Future.successful(redisplaySectionForm(app, fieldValues, errs))

      case SectionTypeCostList => Future.successful(redirectToPreview(app.id, app.sectionNumber))
      case _ => Future.successful(NotFound)
    }
  }

  def doSubmit(id: ApplicationId, applicationDetail: ApplicationDetail, userId: UserId): Future[Option[SubmittedApplicationRef]] = {
    /** Create ProcessDefinition object and activate  **/
    val pdId = ProcessDefinitionId(Config.config.bpm.procdefId)
    val pd = ProcessDefinition(pdId, BusinessKey("businessKey"+ pdId), false, processVariables(applicationDetail, userId))

    /* 2 type of submits to Activiti
        1)Submit Firt time by Applicant:- Create new Process Instance AND Update the BEIS forms Applicationn status to Submit
        2)Submit for 'Request for more Info':- Update existing ProcessInstance AND Update the BEIS forms Applicationn status to Submit
    */
    applicationDetail.appStatus.appStatus match {
        case "In progress" =>  {  /* Fresh Application , so activate the BPM Process*/

          /** Save Application only if Process database is updated without errors */
          processes.activateProcess(pdId, pd).flatMap{
            case Some(procInstId) =>{
              /** Update Appplication record with Submit status **/
              applications.submit(id)
            }
            case _ => Future.successful(None)
          }
        }
        case _ => { /* Already submitted Application, and came back for 'Request for more info' */

            /* Update the existing process instance - get ExecutionID for the Task*/
            processes.getExecution(pdId, ActivityId("BEIS_Wait_Application")).flatMap{
              case Some(executionId) =>{

                    /** Update Activiti Execution to Signal the Waiting Task to release by sending ExecutionID**/
                    val s  = ActionId("signal")
                    val pv =  ProcessVariable("approvestatus", "Submitted")
                    processes.sendSignal(executionId, Action(s, Seq(pv))).flatMap {
                        case Some(executionId) => {
                          /** Update Appplication record with Submit status **/
                          applications.submit(id)
                        }
                        case _ => Future.successful(None)
                    }
              }
              case _ => Future.successful(None)
            }
        }
    }
  }

  def processVariables(applicationDetail: ApplicationDetail, userId: UserId): Seq[ProcessVariable] ={
    val pvAppId       =  ProcessVariable("ApplicationId", applicationDetail.id.id.toString)
    val pvApplicant   =  ProcessVariable("Applicant", userId.id)
    val status        =  ProcessVariable("approvestatus", "Submitted")
    val pvAppRef      =  ProcessVariable("ApplicationReference", applicationDetail.personalReference.getOrElse("Not set").toString)
    val pvOpId        =  ProcessVariable("OpportunityId", applicationDetail.opportunity.id.id.toString())
    val pvOpTitle     =  ProcessVariable("OpportunityTitle", applicationDetail.opportunity.title)
    Seq(pvAppId, pvApplicant, status, pvAppRef, pvOpId, pvOpTitle)
  }

  def completeAndPreview(app: ApplicationSectionDetail, fieldValues: JsObject): Future[Result] = {
    val answers = app.formSection.sectionType match {
      case SectionTypeForm => fieldValues
      // Instead of using the values that were passed in from the form we'll use the values that
      // have already been saved against the item list, since these were created by the add-item
      // form.
      case SectionTypeCostList => app.section.map(_.answers).getOrElse(JsObject(Seq()))
      case _ => JsObject(Seq())
    }

    val previewCheckErrs = check(answers, previewChecksFor(app.formSection))
    if (previewCheckErrs.isEmpty) {
      JsonHelpers.allFieldsEmpty(answers) match {
        case true => applications.deleteSection(app.id, app.sectionNumber).map(_ => redirectToOverview(app.id))
        case false => applications.completeSection(app.id, app.sectionNumber, answers).map {
          case Nil => redirectToPreview(app.id, app.sectionNumber)
          case errs => redisplaySectionForm(app, answers, errs)
        }
      }
    } else Future.successful(redisplaySectionForm(app, answers, previewCheckErrs))
  }

  def redirectToPreview(id: ApplicationId, sectionNumber: AppSectionNumber) =
    Redirect(routes.ApplicationPreviewController.previewSection(id, sectionNumber))

  def renderSectionForm(app: ApplicationSectionDetail,
                        errs: FieldErrors,
                        hints: FieldHints): Result = {
    val answers = app.section.map { s => s.answers }.getOrElse(JsObject(List.empty))
    selectSectionForm(app, answers, errs)
  }

  def redisplaySectionForm(app: ApplicationSectionDetail, answers: JsObject, errs: FieldErrors = noErrors): Result = {
    selectSectionForm(app, answers, errs)
  }

  def selectSectionForm(app: ApplicationSectionDetail, answers: JsObject, errs: FieldErrors): Result = {
    val checks = app.formSection.fields.map(f => f.name -> f.check).toMap
    val hints = hinting(answers, checks)

    app.formSection.sectionType match {
      case SectionTypeForm => Ok(views.html.sectionForm(app, answers, errs, hints))
      case SectionTypeCostList =>
        answers \ "items" match {
          case JsDefined(JsArray(is)) if is.nonEmpty =>
            val itemValues: Seq[JsValue] = (answers \ "items").validate[JsArray].asOpt.map(_.value).getOrElse(Seq())
            val costItems = itemValues.flatMap(_.validate[CostItem].asOpt)
            Ok(views.html.sectionList(app, costItems, answers, errs, hints))
          case _ => Redirect(controllers.routes.CostController.addItem(app.id, app.formSection.sectionNumber))
        }
      case SectionTypeFileList => {
        val itemValues: Seq[JsValue] = (answers \ "items").validate[JsArray].asOpt.map(_.value).getOrElse(Seq())
        val fileUploadItems = itemValues.flatMap(_.validate[FileUploadItem].asOpt)
        Ok(views.html.sectionFileList(app, fileUploadItems, answers, errs, hints))
      }

    }
  }

  def previewChecksFor(formSection: ApplicationFormSection): Map[String, FieldCheck] =
    //TODO:- may need to implement seperate checks for Previews
    //formSection.fields.map(f => f.name -> f.previewCheck).toMap
    formSection.fields.map(f => f.name -> f.check).toMap  //may need to implement seperate checks for Previews
}
