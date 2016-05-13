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

package com.google.cloud.tools.intellij.debugger;

import com.intellij.openapi.util.Comparing;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * CloudLineBreakpointProperties holds custom properties not normally set on a java line breakpoint.
 * Right now, this is just watch expressions.  Custom conditions are supported by default as is the
 * enabled state and other attributes such as source location.
 */
public class CloudLineBreakpointProperties extends
    XBreakpointProperties<CloudLineBreakpointProperties> {

  private static final String[] EMPTY_ARRAY = new String[0];
  private boolean createdByServer = false;
  private boolean disabledByServer = false;
  // Prevents adding duplicate breakpoints when a breakpoint is added, debug session closed and
  // breakpoint is hit offline.
  private boolean addedOnServer = false;
  private String[] watchExpressions;

  @Nullable
  @Override
  public CloudLineBreakpointProperties getState() {
    return this;
  }

  @Tag("watch-expressions")
  public final String[] getWatchExpressions() {
    return watchExpressions != null ? watchExpressions : EMPTY_ARRAY;
  }

  public boolean isCreatedByServer() {
    return createdByServer;
  }

  public void setCreatedByServer(boolean createdByServer) {
    this.createdByServer = createdByServer;
  }

  public boolean isDisabledByServer() {
    return disabledByServer;
  }

  public void setDisabledByServer(boolean disabledByServer) {
    this.disabledByServer = disabledByServer;
  }

  @Override
  public void loadState(CloudLineBreakpointProperties state) {
    watchExpressions = state.getWatchExpressions();
  }

  /**
   * Sets the watch expressions and returns if the passed in expression differ from the
   * currently set ones.
   */
  public final boolean setWatchExpressions(@Nullable String[] watchExpressions) {
    boolean changed = !arrayEqual(this.watchExpressions, watchExpressions);
    if (changed) {
      this.watchExpressions = watchExpressions == null ? null
          : Arrays.copyOf(watchExpressions, watchExpressions.length);
    }
    return changed;
  }

  private static boolean arrayEqual(@Nullable Object[] first, @Nullable Object[] second) {
    if ((first == null || first.length == 0) && (second == null || second.length == 0)) {
      return true;
    }
    return Comparing.equal(first, second);
  }

  public boolean isAddedOnServer() {
    return addedOnServer;
  }

  public void setAddedOnServer(boolean addedOnServer) {
    this.addedOnServer = addedOnServer;
  }
}
