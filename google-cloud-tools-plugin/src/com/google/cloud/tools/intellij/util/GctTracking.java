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

  public static final String CATEGORY = "com.google.cloud.tools";

  // Event actions
  public static final String APP_ENGINE_DEPLOY = "appengine.deploy";
  public static final String PROJECT_SELECTION = "project.selection";
  public static final String VCS_CHECKOUT = "vcs.checkout";
  public static final String VCS_UPLOAD = "vcs.uplaod";

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
}
