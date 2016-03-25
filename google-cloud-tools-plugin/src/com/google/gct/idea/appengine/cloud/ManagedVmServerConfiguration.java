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

package com.google.gct.idea.appengine.cloud;

import com.intellij.remoteServer.configuration.ServerConfigurationBase;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Transient;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Model for the IntelliJ application scoped 'Cloud' configurations.  This is a base configuration
 * used by Managed VM deployment runtime configurations. It's primarily the bits that can be
 * re-used across deployments, such as auth and project.
 */
public class ManagedVmServerConfiguration extends
    ServerConfigurationBase<ManagedVmServerConfiguration> {

  private String cloudSdkHomePath;
  private String cloudProjectName;
  private String googleUserName;

  @Transient
  private PropertyChangeListener projectNameListener;

  @Attribute("cloudSdkHomePath")
  public String getCloudSdkHomePath() {
    return cloudSdkHomePath;
  }

  @Attribute("cloudProjectName")
  public String getCloudProjectName() {
    return cloudProjectName;
  }

  public void setCloudSdkHomePath(String cloudSdkHomePath) {
    this.cloudSdkHomePath = cloudSdkHomePath;
  }

  public void setCloudProjectName(String cloudProjectName) {
    fireNameChangeEvent(this.cloudProjectName, cloudProjectName);
    this.cloudProjectName = cloudProjectName;
  }


  @Attribute("googleUserName")
  public String getGoogleUserName() {
    return googleUserName;
  }

  public void setGoogleUserName(String googleUserName) {
    this.googleUserName = googleUserName;
  }

  protected void setProjectNameListener(PropertyChangeListener listener) {
    this.projectNameListener = listener;
  }

  protected void fireNameChangeEvent(String oldName, String newName) {
    if (projectNameListener != null) {
      projectNameListener.propertyChange(new PropertyChangeEvent(
          this,
          "cloudProjectName",
          oldName,
          newName
      ));
    }
  }
}
