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

import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import com.google.cloud.tools.appengine.api.deploy.DefaultDeployConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkAppEngineDeployment;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Deploys an application to App Engine.
 */
public class AppEngineDeploy {

  private static final Logger logger = Logger.getInstance(AppEngineDeploy.class);

  private AppEngineHelper helper;
  private LoggingHandler loggingHandler;
  private AppEngineDeploymentConfiguration deploymentConfiguration;
  private AppEngineEnvironment environment;
  private DeploymentOperationCallback callback;

  /**
   * Initialize the deployment dependencies.
   */
  public AppEngineDeploy(
      @NotNull AppEngineHelper helper,
      @NotNull LoggingHandler loggingHandler,
      @NotNull AppEngineDeploymentConfiguration deploymentConfiguration,
      @NotNull AppEngineEnvironment environment,
      @NotNull DeploymentOperationCallback callback) {
    this.helper = helper;
    this.loggingHandler = loggingHandler;
    this.deploymentConfiguration = deploymentConfiguration;
    this.environment = environment;
    this.callback = callback;
  }

  /**
   * Given a staging directory, deploy the application to Google App Engine.
   */
  public void deploy(
      @NotNull Path stagingDirectory,
      @NotNull ProcessStartListener deployStartListener) {
    final StringBuilder rawDeployOutput = new StringBuilder();

    DefaultDeployConfiguration configuration = new DefaultDeployConfiguration();
    configuration.setDeployables(
        Collections.singletonList(stagingDirectory.resolve("app.yaml").toFile()));
    configuration.setProject(deploymentConfiguration.getCloudProjectName());

    configuration.setPromote(deploymentConfiguration.isPromote());

    // Only send stopPreviousVersion if the environment is AE flexible (since standard does not
    // support stop), and if promote is true (since its invalid to stop the previous version without
    // promoting).
    if (environment.isFlexible() && deploymentConfiguration.isPromote()) {
      configuration.setStopPreviousVersion(deploymentConfiguration.isStopPreviousVersion());
    }

    if (!StringUtil.isEmpty(deploymentConfiguration.getVersion())) {
      configuration.setVersion(deploymentConfiguration.getVersion());
    }

    ProcessOutputLineListener deployLogListener = new ProcessOutputLineListener() {
      @Override
      public void onOutputLine(String line) {
        loggingHandler.print(line + "\n");
      }
    };
    ProcessOutputLineListener deployOutputListener = new ProcessOutputLineListener() {
      @Override
      public void onOutputLine(String output) {
        rawDeployOutput.append(output);
      }
    };
    ProcessExitListener deployExitListener = new DeployExitListener(rawDeployOutput);

    CloudSdk sdk = helper.createSdk(
        loggingHandler,
        deployStartListener,
        deployLogListener,
        deployOutputListener,
        deployExitListener);
    CloudSdkAppEngineDeployment deployment = new CloudSdkAppEngineDeployment(sdk);
    deployment.deploy(configuration);
  }

  AppEngineHelper getHelper() {
    return helper;
  }

  LoggingHandler getLoggingHandler() {
    return loggingHandler;
  }

  AppEngineDeploymentConfiguration getDeploymentConfiguration() {
    return deploymentConfiguration;
  }

  DeploymentOperationCallback getCallback() {
    return callback;
  }

  private class DeployExitListener implements ProcessExitListener {
    final StringBuilder rawDeployOutput;

    DeployExitListener(StringBuilder rawDeployOutput) {
      this.rawDeployOutput = rawDeployOutput;
    }

    @Override
    public void onExit(int exitCode) {
      try {
        if (exitCode == 0) {
          DeployOutput deployOutput = null;

          try {
            deployOutput = parseDeployOutput(rawDeployOutput.toString());
          } catch (JsonParseException ex) {
            logger.error("Could not retrieve service/version info of deployed application", ex);
          }

          if (deployOutput == null
              || deployOutput.getService() == null || deployOutput.getVersion() == null) {
            loggingHandler.print(
                GctBundle.message("appengine.deployment.version.extract.failure") + "\n"
                    + GctBundle.message("appengine.action.error.update.message") + "\n");
          }

          callback.succeeded(
              new AppEngineDeploymentRuntime(
                  loggingHandler, helper, deploymentConfiguration, environment,
                  deployOutput != null ? deployOutput.getService() : null,
                  deployOutput != null ? deployOutput.getVersion() : null));

        } else {
          logger.warn("Deployment process exited with an error. Exit Code:" + exitCode);
          callback.errorOccurred(
              GctBundle.message("appengine.deployment.error.with.code", exitCode) + "\n"
                  + GctBundle.message("appengine.action.error.update.message"));
        }
      } finally {
        helper.deleteCredentials();
      }
    }
  }

  /**
   * Parse the raw json output of the deployment.
   *
   * @return an object modeling the output of a deploy command
   * @throws JsonParseException if unable to extract the deploy output information needed
   */
  @VisibleForTesting
  static DeployOutput parseDeployOutput(String jsonOutput) throws JsonParseException {
    Type deployOutputType = new TypeToken<DeployOutput>() {}.getType();
    DeployOutput deployOutput = new Gson().fromJson(jsonOutput, deployOutputType);
    if (deployOutput == null
        || deployOutput.versions == null || deployOutput.versions.size() != 1) {
      throw new JsonParseException("Cannot get app version: unexpected gcloud JSON output format");
    }
    return deployOutput;
  }

  /**
   * Holds de-serialized JSON output of gcloud app deploy. Don't change the field names
   * because Gson uses it for automatic de-serialization.
   */
  @SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD", justification = "Initialized by Gson")
  static class DeployOutput {
    private static class Version {
      String id;
      String service;
    }

    List<Version> versions;

    @Nullable
    public String getVersion() {
      if (versions == null || versions.size() != 1) {
        return null;
      }
      return versions.get(0).id;
    }

    @Nullable
    public String getService() {
      if (versions == null || versions.size() != 1) {
        return null;
      }
      return versions.get(0).service;
    }
  }
}
