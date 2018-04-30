/*
 * Copyright 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.stackdriver.debugger;

import com.google.api.services.clouddebugger.v2.model.Breakpoint;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ImmutableList;
import com.intellij.util.xmlb.annotations.Transient;
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * CloudDebugProcessState is serializable state that stores the current set of snapshots for a given
 * GCP application. It is used by a {@link CloudDebugProcess}, but its lifetime can span beyond a
 * debug session and be used by the background process watcher.
 */
public class CloudDebugProcessState extends UserDataHolderBase implements RunProfileState {

  // The current state is simply an array of breakpoints.  It's volatile because it's updated and
  // retrieved on different threads with otherwise no synchronization.
  private volatile ImmutableList<Breakpoint> currentServerBreakpointList =
      ContainerUtil.immutableList(new ArrayList<Breakpoint>());
  // DebuggeeId is defined by the cloud debugger service to represent a single target service
  // that we can debug.
  private String debuggeeId;
  // The local IDE project that contains the source code running in the debuggee.
  private Project project;
  // The project name for the target debuggee
  private String projectName;
  // The project number for the target debuggee
  private String projectNumber;
  // The email of the user associated with the debugee of this state.
  private String userEmail;
  // A WaitToken is defined by the cloud debugger service to represent the entirety of the
  // service state.
  // The state is defined by any combination of snapshots and their values.
  private String waitToken;
  // Whether this state listens in background or not
  private boolean listenInBackground;

  /**
   * This constructor is used by deserialization of the {@link CloudDebugProcessStateSerializer}. We
   * use a separate serializer so we can ensure that the config is written to workspace.xml which is
   * not shared between users and is never checked in.
   */
  public CloudDebugProcessState() {}

  /**
   * CloudDebugProcessState can be initialized with partial state to indicate preferences on the
   * {@link CloudDebugRunConfiguration}. When the process state is partial, the attach dialog is
   * used to make it complete. isValidDebuggee must be true for the attach to continue.
   *
   * @param userEmail the user's email which corresponds to a login {@link CredentialedUser}
   * @param debuggeeId a String Id that represents a target application to debug (debuggee)
   * @param projectName a alpha-numeric String name identifying a GCP project that owns the target
   *     debuggee
   * @param projectNumber a numeric String identifying the same GCP project that owns the target
   *     debuggee
   * @param project the intelliJ IDE project
   */
  public CloudDebugProcessState(
      @Nullable String userEmail,
      @Nullable String debuggeeId,
      @Nullable String projectName,
      @Nullable String projectNumber,
      @Nullable Project project) {
    setUserEmail(userEmail);
    setDebuggeeId(debuggeeId);
    setProjectName(projectName);
    setProjectNumber(projectNumber);
    setProject(project);
    setListenInBackground(false);
  }

  @Nullable
  @Override
  public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner)
      throws ExecutionException {
    // There is nothing to execute.  We set up all of our state in CloudDebugRunner.
    return null;
  }

  /**
   * Returns a cached set of {@link Breakpoint} objects. The list is periodically updated from a
   * background timer.
   *
   * @return the current list of breakpoints and their state
   */
  @NotNull
  @Transient
  public ImmutableList<Breakpoint> getCurrentServerBreakpointList() {
    return currentServerBreakpointList;
  }

  /** Updates the state (breakpoint list). */
  public void setCurrentServerBreakpointList(ImmutableList<Breakpoint> newBreakpointList) {
    currentServerBreakpointList = newBreakpointList;
  }

  /**
   * Called during serialization to store the id in workspace.xml.
   *
   * @return the target debuggee Id the state is attached to
   */
  @Nullable
  public String getDebuggeeId() {
    return debuggeeId;
  }

  /** Called during deserialization from {@link CloudDebugProcessStateSerializer}. */
  public void setDebuggeeId(@Nullable String debuggeeId) {
    this.debuggeeId = debuggeeId;
  }

  /**
   * Returns the local IDE project hosting source for the target debuggee.
   *
   * @return the intelliJ {@link Project} used to debug
   */
  @Transient
  public Project getProject() {
    return project;
  }

  /**
   * Called after deserialization, it initializes this state with the current project.
   *
   * @param project the intelliJ IDE {@link Project}
   */
  public void setProject(@Nullable Project project) {
    this.project = project;
  }

  /**
   * Used by serialization and de-serialization to store the project name.
   *
   * @return the name of the GCP project
   */
  public String getProjectName() {
    return projectName;
  }

  /**
   * Sets the project name associated with the debuggee.
   *
   * @param projectName the GCP project name that owns the debuggee
   */
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  /**
   * The project number corresponds to the project name, but is not user friendly. However its
   * required in calls to the debugger apiary.
   *
   * @return the numeric Id associated with the owning GCP project
   */
  @Nullable
  public String getProjectNumber() {
    return projectNumber;
  }

  /**
   * Sets the project number that identifies the debugee target. It's similar to project name. We
   * store both because locally we resolve project via name, but the server takes a project number.
   * We don't want to have to call elysium to resolve between the two if we don't have to.
   *
   * @param projectNumber the numeric Id associated with the owning GCP project
   */
  public void setProjectNumber(String projectNumber) {
    this.projectNumber = projectNumber;
  }

  /**
   * The email user is stored in workspace.xml and is used during deserialization to recreate the
   * debugger client.
   *
   * @return the string identifying the user that has access to the GCP Project
   */
  @Nullable
  public String getUserEmail() {
    return userEmail;
  }

  /**
   * Sets the email (and corresponding credential to use) when attaching to the server.
   *
   * @param userEmail the string identifying the user that has access to the GCP Project
   */
  public void setUserEmail(@Nullable String userEmail) {
    this.userEmail = userEmail;
  }

  /**
   * Returns an identifier that represents debuggee state. The wait token is guaranteed to be unique
   * between two different debuggee states.
   *
   * @return the waitToken for use during serialization
   */
  @Nullable
  public String getWaitToken() {
    return waitToken;
  }

  /**
   * Sets the wait token (called during serialization).
   *
   * @param waitToken an identifier that represents debuggee state
   */
  public void setWaitToken(@Nullable String waitToken) {
    this.waitToken = waitToken;
  }

  /** Returns whether this state is configured to allow background listening. */
  public boolean isListenInBackground() {
    return listenInBackground;
  }

  /** Set whether a service should look for events in the background for this state. */
  public void setListenInBackground(boolean listenInBackground) {
    this.listenInBackground = listenInBackground;
  }
}
