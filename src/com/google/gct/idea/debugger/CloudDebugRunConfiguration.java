/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.google.gct.idea.debugger;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The CloudDebugRunConfiguration stores settings to use when attaching to a target. It also creates and possibly caches
 * {@link CloudDebugProcessState}.
 * <p/>
 * When the IDE is shut down, we store this state using {@link CloudDebugProcessStateSerializer} to ensure that it is
 * stored on a per-user basis in workspace.xml.
 * <p/>
 * RunConfigurations can either be stored in workspace.xml or in a shared location depending on whether the user has
 * selected "Shared".
 */
public class CloudDebugRunConfiguration extends LocatableConfigurationBase
  implements ModuleRunConfiguration, RunConfigurationWithSuppressedDefaultDebugAction,
             RunConfigurationWithSuppressedDefaultRunAction, RemoteRunProfile {
  private static final String NAME = "Cloud Debug Configuration";
  private static final String PROJECT_NAME_TAG = "CloudProjectName";
  private static final String SHOW_NOTIFICATIONS = "CloudShowNotify";
  private String myCloudProjectName;
  private volatile CloudDebugProcessState myProcessState = null;
  private boolean myShowNotifications = true;

  protected CloudDebugRunConfiguration(Project project, @NotNull ConfigurationFactory factory) {
    super(project, factory, NAME);
    CloudDebugProcessWatcher.getInstance().ensureWatcher();
  }

  @Override
  public final RunConfiguration clone() {
    final CloudDebugRunConfiguration configuration = (CloudDebugRunConfiguration)super.clone();
    configuration.setCloudProjectName(getCloudProjectName());
    configuration.setShowNotifications(isShowNotifications());
    return configuration;
  }

  /**
   * Returns the GCP project name chosen by the user for this {@link CloudDebugRunConfiguration}.
   *
   * @return the String name of the GCP project
   */
  @Nullable
  public String getCloudProjectName() {
    return myCloudProjectName;
  }

  /**
   * Sets the GCP project name for this {@link CloudDebugRunConfiguration}.
   *
   * @param cloudProjectName the name of the GCP project that owns the target debuggee
   */
  public void setCloudProjectName(@Nullable String cloudProjectName) {
    myCloudProjectName = cloudProjectName;
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new CloudDebugSettingsEditor();
  }

  @NotNull
  @Override
  public Module[] getModules() {
    return Module.EMPTY_ARRAY;
  }

  /**
   * Returns the current debuggee state associated with this {@link CloudDebugRunConfiguration}.
   *
   * @return a CloudDebugProcessState that may be deserialized from workspace.xml or updated live from the server
   */
  @Nullable
  public CloudDebugProcessState getProcessState() {
    return myProcessState;
  }

  /**
   * Sets the debuggee state associated with this {@link CloudDebugRunConfiguration}.
   *
   * @param processState the state to associate with this {@link CloudDebugRunConfiguration}
   */
  public void setProcessState(@Nullable CloudDebugProcessState processState) {
    myProcessState = processState;
  }

  /**
   * Returns either cached state (if we were previously watching this state in this or the last IDE session) Or it
   * returns a partially valid state, which will later be filled in by the {@link com.google.gct.idea.debugger
   * .CloudDebuggerRunner}
   *
   * @param executor    the execution mode selected by the user (run, debug, profile etc.)
   * @param environment the environment object containing additional settings for executing the configuration.
   */
  @Nullable
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment)
    throws ExecutionException {
    if (myProcessState == null) {
      return new CloudDebugProcessState(null, null, myCloudProjectName, null, getProject());
    }
    return myProcessState;
  }

  public boolean isShowNotifications() {
    return myShowNotifications;
  }

  public void setShowNotifications(boolean showNotifications) {
    myShowNotifications = showNotifications;
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    super.readExternal(element);
    Attribute projectNameAttribute = element.getAttribute(PROJECT_NAME_TAG);
    if (projectNameAttribute != null) {
      myCloudProjectName = projectNameAttribute.getValue();
    }
    Attribute showNotificationsAttribute = element.getAttribute(SHOW_NOTIFICATIONS);
    if (showNotificationsAttribute != null) {
      myShowNotifications = Boolean.valueOf(showNotificationsAttribute.getValue());
    }
    // Call out to the state serializer to get process state out of workspace.xml.
    if (!Strings.isNullOrEmpty(myCloudProjectName) && !Strings.isNullOrEmpty(getName())) {
      myProcessState =
        CloudDebugProcessStateSerializer.getInstance(getProject()).getState(getName(), myCloudProjectName);
    }
    CloudDebugProcessWatcher.getInstance().ensureWatcher();
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    super.writeExternal(element);
    element.setAttribute(PROJECT_NAME_TAG, myCloudProjectName == null ? "" : myCloudProjectName);
    element.setAttribute(SHOW_NOTIFICATIONS, Boolean.toString(myShowNotifications));
    // IJ handles serialization of the the state serializer since its a project service.
  }
}
