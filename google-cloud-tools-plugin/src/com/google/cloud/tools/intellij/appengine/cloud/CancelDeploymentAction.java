/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.appengine.cloud;

import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.icons.AllIcons.Actions;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfiguration;
import com.intellij.remoteServer.impl.runtime.ui.tree.DeploymentNode;
import com.intellij.remoteServer.impl.runtime.ui.tree.actions.ServersTreeAction;
import com.intellij.remoteServer.runtime.Deployment;
import com.intellij.remoteServer.runtime.deployment.DeploymentStatus;

/**
 * The action behind the "Cancel Deployment" toolbar button. It's only visible when a deployment to
 * App Engine is in progress.
 */
public class CancelDeploymentAction extends ServersTreeAction<DeploymentNode> {

  public CancelDeploymentAction() {
    super(GctBundle.message("appengine.deployment.cancel.button.text"),
        GctBundle.message("appengine.deployment.cancel.button.description"), Actions.Cancel);
  }

  @Override
  protected Class<DeploymentNode> getTargetNodeClass() {
    return DeploymentNode.class;
  }

  @Override
  protected boolean isVisible4(DeploymentNode node) {
    return getDeployment(node).getConnection().getServer().getType() instanceof AppEngineCloudType;
  }

  @Override
  protected boolean isEnabled4(DeploymentNode node) {
    return getDeployment(node).getStatus() == DeploymentStatus.DEPLOYING;
  }

  @Override
  protected void doActionPerformed(DeploymentNode node) {
    DeploymentConfiguration config = getDeployment(node).getDeploymentTask().getConfiguration();
    if (config instanceof AppEngineDeploymentConfiguration) {
      ((AppEngineDeploymentConfiguration)config).cancelCurrentAction();
    }
  }

  private Deployment getDeployment(DeploymentNode node) {
    return (Deployment) node.getValue();
  }

}
