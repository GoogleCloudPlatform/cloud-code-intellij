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

package com.google.cloud.tools.intellij.ui;

import java.awt.Font;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/** Tools to work with fonts when standard settings are not sufficient. */
public class FontUtils {

  /**
   * Converts font for all characters in {@link StyledDocument} to system default font. Does not
   * change or affect newly added characters.
   *
   * @param styledDocument {@link StyledDocument} to update, see {@link javax.swing.JTextPane}
   */
  public static void convertStyledDocumentFontToDefault(StyledDocument styledDocument) {
    for (int ch = 0; ch < styledDocument.getLength(); ch++) {
      AttributeSet defaultStyle = styledDocument.getCharacterElement(ch).getAttributes();
      MutableAttributeSet updatedFontAttrbutes = new SimpleAttributeSet(defaultStyle);
      Font defaultFont = UIManager.getFont("Label.font");
      StyleConstants.setFontFamily(updatedFontAttrbutes, defaultFont.getFamily());
      StyleConstants.setFontSize(updatedFontAttrbutes, defaultFont.getSize());

      styledDocument.setCharacterAttributes(ch, 1, updatedFontAttrbutes, true);
    }
  }
}
