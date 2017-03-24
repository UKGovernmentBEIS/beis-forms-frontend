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

import javax.sql.DataSource

import org.activiti.engine.ProcessEngineConfiguration
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl

import scala.xml.Elem

/**
 * Minimal Activiti engine wrapper for deploying a process.
 */
class ProcessEngineWrapper(implicit ds: DataSource) {

  val engine = ProcessEngineConfiguration.
    createStandaloneProcessEngineConfiguration.asInstanceOf[ProcessEngineConfigurationImpl].
    setDataSource(ds).asInstanceOf[ProcessEngineConfigurationImpl].
    setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE).
    buildProcessEngine()

  // Register the event listener that logs process activity.
  engine.getRuntimeService().addEventListener(new LoggingEventListener)

  /**
   * Deploy the given BPMN process definition XML in this engine.
   */
  def deploy(processXml: Elem): Int = {
    val definitions = <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:activiti="http://activiti.org/bpmn" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC" xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI" typeLanguage="http://www.w3.org/2001/XMLSchema" expressionLanguage="http://www.w3.org/1999/XPath" targetNamespace="http://www.activiti.org/test">
      { processXml }
    </definitions>

    val repository = engine.getRepositoryService()
    val deployment = repository.createDeployment()
    deployment.addString("test.bpmn", definitions.toString).name("Test deployment")
    deployment.deploy().getId.toInt
  }

  /**
   * Executes the deployed process.
   */
  def execute(processKey: String, variables: Map[String, AnyRef] = Map.empty[String, AnyRef]): Unit = {
    val runtimeService = engine.getRuntimeService()
    import scala.collection.JavaConverters._
    runtimeService.startProcessInstanceByKey(processKey, variables.asJava)
  }
}