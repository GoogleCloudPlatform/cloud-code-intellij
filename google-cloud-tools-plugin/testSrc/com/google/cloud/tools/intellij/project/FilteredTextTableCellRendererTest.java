/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.project;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;

public class FilteredTextTableCellRendererTest {
  private static final String SAMPLE_TEXT = "your project name and PROJECT id.";

  private FilteredTextTableCellRenderer renderer;

  @Before
  public void setUp() {
    renderer = new FilteredTextTableCellRenderer();
  }

  @Test
  public void accepts_nullInputs() {
    String result = renderer.highlightFilterText(null, null);

    assertThat(result).isNull();
  }

  @Test
  public void accepts_emptyInputs() {
    String result = renderer.highlightFilterText("", "");

    assertThat(result).isEmpty();
  }

  @Test
  public void emptyFilter_doesNotChangeText() {
    String result = renderer.highlightFilterText("", SAMPLE_TEXT);

    assertThat(result).isEqualTo(SAMPLE_TEXT);
  }

  @Test
  public void highlights_matchingText() {
    String result = renderer.highlightFilterText("project", SAMPLE_TEXT);
    String expected = "<html>your <b>project</b> name and <b>PROJECT</b> id.";

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void highlights_matchingText_atStart() {
    String result = renderer.highlightFilterText("you", SAMPLE_TEXT);
    String expected = "<html><b>you</b>r project name and PROJECT id.";

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void highlights_matchingText_atEnd() {
    String result = renderer.highlightFilterText("id.", SAMPLE_TEXT);
    String expected = "<html>your project name and PROJECT <b>id.</b>";

    assertThat(result).isEqualTo(expected);
  }
}
