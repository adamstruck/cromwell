package cromwell.backend

import akka.actor.{ActorRef, Props}
import com.typesafe.config.Config
import cromwell.backend.io.WorkflowPathsWithDocker
import cromwell.core.CallOutputs
import cromwell.core.JobExecutionToken.JobExecutionTokenType
import cromwell.core.path.Path
import wdl4s.wdl.WdlTaskCall
import wdl4s.wdl.expression.{PureStandardLibraryFunctions, WdlStandardLibraryFunctions}
import net.ceedubs.ficus.Ficus._

trait BackendLifecycleActorFactory {

  /**
    * Name of the backend.
    *
    * This is the first parameter passed into each factory during creation.
    *
    * @return The configuration-defined name for this instance of the backend.
    */
  def name: String

  /**
    * Config values for the backend, and a pointer to the global config.
    *
    * This is the second parameter passed into each factory during creation.
    *
    * @return The backend configuration.
    */
  def configurationDescriptor: BackendConfigurationDescriptor

  /* ****************************** */
  /*     Workflow Initialization    */
  /* ****************************** */

  def workflowInitializationActorProps(workflowDescriptor: BackendWorkflowDescriptor,
                                       ioActor: ActorRef,
                                       calls: Set[WdlTaskCall],
                                       serviceRegistryActor: ActorRef,
                                       restarting: Boolean): Option[Props] = None

  /* ****************************** */
  /*          Job Execution         */
  /* ****************************** */

  def jobExecutionActorProps(jobDescriptor: BackendJobDescriptor,
                             initializationData: Option[BackendInitializationData],
                             serviceRegistryActor: ActorRef,
                             ioActor: ActorRef,
                             backendSingletonActor: Option[ActorRef]): Props

  lazy val jobExecutionTokenType: JobExecutionTokenType = {
    val concurrentJobLimit = configurationDescriptor.backendConfig.as[Option[Int]]("concurrent-job-limit")
    JobExecutionTokenType(name, concurrentJobLimit)
  }

  /* ****************************** */
  /*      Workflow Finalization     */
  /* ****************************** */

  def workflowFinalizationActorProps(workflowDescriptor: BackendWorkflowDescriptor,
                                     ioActor: ActorRef,
                                     calls: Set[WdlTaskCall],
                                     jobExecutionMap: JobExecutionMap,
                                     workflowOutputs: CallOutputs,
                                     initializationData: Option[BackendInitializationData]): Option[Props] = None

  /* ****************************** */
  /*           Call Caching         */
  /* ****************************** */

  def fileHashingActorProps: Option[(BackendJobDescriptor, Option[BackendInitializationData], ActorRef, ActorRef) => Props] = None

  /**
    * Providing this method to generate Props for a cache hit copying actor is optional.
    * To implement it, add a function:
    * def cacheHitCopyingActorInner(jobDescriptor: BackendJobDescriptor,initializationData: Option[BackendInitializationData], serviceRegistryActor: ActorRef, ioActor: ActorRef): Props
    * And then override this method to point to it:
    * override def cacheHitCopyingActorProps = Option(cacheHitCopyingActorInner _)
    *
    * Simples!
    */
  def cacheHitCopyingActorProps: Option[(BackendJobDescriptor, Option[BackendInitializationData], ActorRef, ActorRef) => Props] = None

  /* ****************************** */
  /*              Misc.             */
  /* ****************************** */

  def backendSingletonActorProps: Option[Props] = None

  def expressionLanguageFunctions(workflowDescriptor: BackendWorkflowDescriptor,
                                  jobKey: BackendJobDescriptorKey,
                                  initializationData: Option[BackendInitializationData]): WdlStandardLibraryFunctions = PureStandardLibraryFunctions

  def getExecutionRootPath(workflowDescriptor: BackendWorkflowDescriptor, backendConfig: Config, initializationData: Option[BackendInitializationData]): Path = {
    new WorkflowPathsWithDocker(workflowDescriptor, backendConfig).executionRoot
  }

  def getWorkflowExecutionRootPath(workflowDescriptor: BackendWorkflowDescriptor, backendConfig: Config, initializationData: Option[BackendInitializationData]): Path = {
    new WorkflowPathsWithDocker(workflowDescriptor, backendConfig).workflowRoot
  }

  def runtimeAttributeDefinitions(initializationDataOption: Option[BackendInitializationData]): Set[RuntimeAttributeDefinition] = Set.empty

  /**
    * A set of KV store keys that this backend requests that the engine lookup before running each job.
    */
  def requestedKeyValueStoreKeys: Seq[String] = Seq.empty

  /*
   * Returns credentials that can be used to authenticate to a docker registry server
   * in order to obtain a docker hash.
   */
  def dockerHashCredentials(initializationDataOption: Option[BackendInitializationData]): List[Any] = List.empty
}
