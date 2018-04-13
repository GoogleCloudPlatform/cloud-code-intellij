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

package com.google.cloud.tools.intellij.analytics;

/** Class defining client-side event actions we want to track. */
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
  public static final String APP_ENGINE_APPLICATION_CREATE_SUCCESS =
      "appengine.application.create.success";
  public static final String APP_ENGINE_APPLICATION_CREATE_FAIL =
      "appengine.application.create.fail";
  public static final String APP_ENGINE_FACET_ADD = "appengine.facet.add";
  public static final String APP_ENGINE_GENERATE_FILE_APPYAML =
      "appengine.generate.file.appyaml.click";
  public static final String APP_ENGINE_GENERATE_FILE_DOCKERFILE =
      "appengine.generate.file.dockerfile.click";

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
  public static final String APP_ENGINE_OLD_PLUGIN_DEACTIVATED = "appengine.oldplugin.deactivated";

  public static final String CLOUD_SDK_MALFORMED_PATH = "cloudsdk.malformedpath";

  public static final String GCS_BUCKET_LIST = "cloud.storage.bucket.list";
  public static final String GCS_BUCKET_LIST_EXCEPTION = "cloud.storage.bucket.list.exception";
  public static final String GCS_BUCKET_LIST_ACTION_COPY_BUCKET_NAME =
      "cloud.storage.bucket.list.action.copy.bucket.name";
  public static final String GCS_BLOB_BROWSE = "cloud.storage.blob.browse";
  public static final String GCS_BLOB_BROWSE_ACTION_COPY_BUCKET_NAME =
      "cloud.storage.blob.browse.action.copy.bucket.name";
  public static final String GCS_BLOB_BROWSE_ACTION_COPY_BLOB_NAME =
      "cloud.storage.blob.browse.action.copy.blob.name";
  public static final String GCS_BLOB_BROWSE_EXCEPTION = "cloud.storage.blob.browse.exception";

  public static final String METADATA_LABEL_KEY = "label";
  public static final String METADATA_MESSAGE_KEY = "message";
  public static final String METADATA_BUILD_SYSTEM_KEY = "build";
  public static final String METADATA_SDK_KEY = "sdk";
  public static final String METADATA_BUILD_SYSTEM_MAVEN = "maven";

  public static final String ACCOUNT_PLUGIN_DETECTED = "account.plugin.detected";
  public static final String ACCOUNT_PLUGIN_UNINSTALLED = "account.plugin.uninstalled";
  public static final String ACCOUNT_PLUGIN_RESTART_DIALOG_YES_ACTION =
      "account.plugin.restart.dialog.yes.action";
  public static final String ACCOUNT_PLUGIN_RESTART_DIALOG_NO_ACTION =
      "account.plugin.restart.dialog.no.action";

  public static final String CLIENT_LIBRARY_ADD_LIBRARY = "client.library.add.library";
  public static final String CLIENT_LIBRARY_ENABLE_API = "client.library.enable.api";

  public static final String MANAGED_SDK_SUCCESSFUL_INSTALL = "managed.sdk.successful.install";
  public static final String MANAGED_SDK_FAILED_INSTALL = "managed.sdk.failed.install";
  public static final String MANAGED_SDK_SUCCESSFUL_UPDATE = "managed.sdk.successful.update";
  public static final String MANAGED_SDK_FAILED_UPDATE = "managed.sdk.failed.update";
}
