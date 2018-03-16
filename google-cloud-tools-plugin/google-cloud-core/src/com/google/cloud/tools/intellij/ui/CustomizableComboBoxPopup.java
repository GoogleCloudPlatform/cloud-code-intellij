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

package com.google.cloud.tools.intellij.ui;

import com.intellij.ui.awt.RelativePoint;

/**
 * The interface implemented when creating a customized combobox. The implementor needs to define
 * the contents of the popup as well as implements methods for setting and getting the currently
 * selected item's text.
 */
public interface CustomizableComboBoxPopup {

  // Shows a PopUp at the given point.
  void showPopup(RelativePoint showTarget);

  // Hides a PopUp at the given point.
  void hidePopup();

  // Returns true if the PopUp is visible on screen.
  boolean isPopupVisible();
}
