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

import com.intellij.openapi.util.Comparing;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * CloudLineBreakpointProperties holds custom properties not normally set on a java line breakpoint. Right now, this is
 * just watch expressions.  Custom conditions are supported by default as is the enabled state and other attributes such
 * as source location.
 */
public class CloudLineBreakpointProperties extends XBreakpointProperties<CloudLineBreakpointProperties> {
  private static final String[] EMPTY_ARRAY = new String[0];
  private boolean myCreatedByServer = false;
  private boolean myDisabledByServer = false;
  // Prevents adding duplicate breakpoints when a breakpoint is added, debug session closed and
  // breakpoint is hit offline.
  private boolean addedOnServer = false;
  private String[] myWatchExpressions;

  @Nullable
  @Override
  public CloudLineBreakpointProperties getState() {
    return this;
  }

  @Tag("watch-expressions")
  public final String[] getWatchExpressions() {
    return myWatchExpressions != null ? myWatchExpressions : EMPTY_ARRAY;
  }

  public boolean isCreatedByServer() {
    return myCreatedByServer;
  }

  public void setCreatedByServer(boolean createdByServer) {
    myCreatedByServer = createdByServer;
  }

  public boolean isDisabledByServer() {
    return myDisabledByServer;
  }

  public void setDisabledByServer(boolean disabledByServer) {
    myDisabledByServer = disabledByServer;
  }

  @Override
  public void loadState(CloudLineBreakpointProperties state) {
    myWatchExpressions = state.getWatchExpressions();
  }

  public final boolean setWatchExpressions(@Nullable String[] watchExpressions) {
    boolean changed = !arrayEqual(myWatchExpressions, watchExpressions);
    if (changed) {
      myWatchExpressions = watchExpressions == null ? null : Arrays.copyOf(watchExpressions, watchExpressions.length);
    }
    return changed;
  }

  private static boolean arrayEqual(@Nullable Object[] a, @Nullable Object[] b) {
    if ((a == null || a.length == 0) && (b == null || b.length == 0)) {
      return true;
    }
    return Comparing.equal(a, b);
  }

  public boolean isAddedOnServer() {
    return addedOnServer;
  }

  public void setAddedOnServer(boolean addedOnServer) {
    this.addedOnServer = addedOnServer;
  }
}
