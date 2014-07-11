package com.netflix.spinnaker.orca.kato.tasks

import spock.lang.Specification
import spock.lang.Subject
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.netflix.spinnaker.orca.SimpleTaskContext
import com.netflix.spinnaker.orca.TaskResult
import com.netflix.spinnaker.orca.kato.api.EnableOrDisableAsgOperation
import com.netflix.spinnaker.orca.kato.api.KatoService
import com.netflix.spinnaker.orca.kato.api.ResizeAsgOperation
import com.netflix.spinnaker.orca.kato.api.TaskId

/**
 * Created by aglover on 7/10/14.
 */
class ResizeAsgTaskSpec extends Specification {
  @Subject task = new ResizeAsgTask()
  def context = new SimpleTaskContext()
  def mapper = new ObjectMapper()
  def taskId = new TaskId(UUID.randomUUID().toString())

  def resizeASGConfig = [
      asgName    : "test-asg",
      regions    : ["us-west-1", "us-east-1"],
      credentials: "fzlem",
      capacity   : [
          min: 1,
          max: 10
      ]
  ]

  def setup() {
    mapper.registerModule(new GuavaModule())

    task.mapper = mapper

    resizeASGConfig.each {
      context."resizeAsg.$it.key" = it.value
    }
  }

  def "creates a disable ASG task based on job parameters"() {
    given:
    def operations
    task.kato = Mock(KatoService) {
      1 * requestOperations(*_) >> {
        operations = it[0]
        rx.Observable.from(taskId)
      }
    }

    when:
    task.execute(context)

    then:
    operations.size() == 1
    with(operations[0].resizeAsgDescription) {
      it instanceof ResizeAsgOperation
      asgName == resizeASGConfig.asgName
      region == resizeASGConfig.region
      credentials == resizeASGConfig.credentials
    }
  }

  def "returns a success status with the kato task id"() {
    given:
    task.kato = Stub(KatoService) {
      requestOperations(*_) >> rx.Observable.from(taskId)
    }

    when:
    def result = task.execute(context)

    then:
    result.status == TaskResult.Status.SUCCEEDED
    result.outputs."kato.task.id" == taskId
    result.outputs."deploy.account.name" == resizeASGConfig.credentials
  }
}
