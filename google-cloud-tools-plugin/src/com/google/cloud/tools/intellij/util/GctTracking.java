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

package com.google.cloud.tools.intellij.util;

/**
 * Class defining client-side event actions we want to track.
 */
public class GctTracking {

  // Event actions
  public static final String APP_ENGINE_DEPLOY = "appengine.deploy";
  public static final String APP_ENGINE_DEPLOY_SUCCESS = "appengine.deploy.success";
  public static final String APP_ENGINE_DEPLOY_FAIL = "appengine.deploy.fail";
  public static final String APP_ENGINE_DEPLOY_CANCEL = "appengine.deploy.cancel";
  public static final String APP_ENGINE_RUN = "appengine.run";
  public static final String APP_ENGINE_STOP = "appengine.stop";
  public static final String APP_ENGINE_ADD_SUPPORT = "appengine.support.add";
  public static final String APP_ENGINE_ADD_LIBRARY = "appengine.library.add";
  public static final String APP_ENGINE_APPLICATION_CREATE = "appengine.application.create";
  public static final String APP_ENGINE_APPLICATION_CREATE_SUCCESS
      = "appengine.application.create.success";
  public static final String APP_ENGINE_APPLICATION_CREATE_FAIL
      = "appengine.application.create.fail";

  public static final String APP_ENGINE_ADD_STANDARD_FACET = "appengine.standard.facet.add";

  public static final String APP_ENGINE_ADD_FLEX_FACET = "appengine.flex.facet.add";
  public static final String APP_ENGINE_FLEX_APP_YAML_CREATE = "appengine.flex.app.yaml.create";
  public static final String APP_ENGINE_FLEX_APP_YAML_CREATE_SUCCESS =
      "appengine.flex.app.yaml.create.success";
  public static final String APP_ENGINE_FLEX_APP_YAML_CREATE_FAIL =
      "appengine.flex.app.yaml.create.fail";
  public static final String APP_ENGINE_FLEX_DOCKERFILE_CREATE = "appengine.flex.dockerfile.create";
  public static final String APP_ENGINE_FLEX_DOCKERFILE_CREATE_SUCCESS =
      "appengine.flex.dockerfile.create.success";
  public static final String APP_ENGINE_FLEX_DOCKERFILE_CREATE_FAIL =
      "appengine.flex.dockerfile.create.fail";

  public static final String PROJECT_SELECTION_CREATE_NEW_PROJECT =
      "project.selection.create.new.project";

  public static final String VCS_CHECKOUT = "vcs.checkout";
  public static final String VCS_UPLOAD = "vcs.upload";

  public static final String CLOUD_DEBUGGER_NEW_RUN_CONFIG = "cloud.debugger.new.run.config";
  public static final String CLOUD_DEBUGGER_CREATE_BREAKPOINT = "cloud.debugger.create.breakpoint";
  public static final String CLOUD_DEBUGGER_CLONE_BREAKPOINTS = "cloud.debugger.clone.breakpoints";
  public static final String CLOUD_DEBUGGER_SNAPSHOT_RECEIVED = "cloud.debugger.snapshot.received";
  public static final String CLOUD_DEBUGGER_START_SESSION = "cloud.debugger.start.session";
  public static final String CLOUD_DEBUGGER_CLOSE_STOP_LISTEN = "cloud.debugger.close.stop.listen";
  public static final String CLOUD_DEBUGGER_CLOSE_CONTINUE_LISTEN =
      "cloud.debugger.close.continue.listen";
  public static final String CLOUD_DEBUGGER_NOTIFY_BREAKPOINT_LIST_CHANGE =
      "cloud.debugger.notify.breakpoint.list.change";

  public static final String APP_ENGINE_OLD_PLUGIN_NOTIFICATION =
      "appengine.oldplugin.notification";
  public static final String APP_ENGINE_OLD_PLUGIN_NOTIFICATION_CLICK =
      "appengine.oldplugin.notification.link.click";
  public static final String APP_ENGINE_OLD_PLUGIN_DEACTIVATED =
      "appengine.oldplugin.deactivated";

  public static final String CLOUD_SDK_MALFORMED_PATH = "cloudsdk.malformedpath";

  public static final String METADATA_LABEL_KEY = "label";
  public static final String METADATA_MESSAGE_KEY = "message";
}
