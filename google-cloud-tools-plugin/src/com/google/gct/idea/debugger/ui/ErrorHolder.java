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

package com.google.gct.idea.debugger.ui;

/**
 * Stores the error message received when querying debug targets, but provides a blank item as
 * representation in a JComboBox
 */
// The reason for this class is that the combobox must have an item to ensure that
// com.google.gct.idea.debugger.ui.CloudAttachDialog#doValidate() method correctly identifies
// the case when the result for querying debug targets has been received and validation can be
// carried out
public class ErrorHolder implements DebugTargetSelectorItem {

  private String errorMessage;

  public ErrorHolder(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  @Override
  // return empty String for the renderer
  public String toString() {
    return "";
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
