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

package com.google.cloud.tools.intellij.gcs;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

/** Tests for {@link GcsBreadcrumbsTextPane}. */
public class GcsBreadcrumbsTextPaneTest {

  private static final String BUCKET_NAME = "my-bucket";

  private GcsBreadcrumbsTextPane breadcrumbs;

  @Before
  public void setUp() {
    breadcrumbs = new GcsBreadcrumbsTextPane();
  }

  @Test
  public void testRootDirectoryRendering() {
    breadcrumbs.render(BUCKET_NAME);
    assertThat(breadcrumbs.getText()).isEqualTo(withHtmlWrap(BUCKET_NAME));
  }

  @Test
  public void testSubdirectoryRendering() {
    breadcrumbs.render(BUCKET_NAME, "dir-1/");

    String expectedBreadcrumbs = concatBreadcrumbs(withHyperlink(BUCKET_NAME, ""), "dir-1");
    assertThat(breadcrumbs.getText()).isEqualTo(withHtmlWrap(expectedBreadcrumbs));
  }

  @Test
  public void testMultipleNestedSubdirectoryRendering() {
    breadcrumbs.render(BUCKET_NAME, "dir-1/dir-2/");

    String expectedBreadcrumbs =
        concatBreadcrumbs(
            withHyperlink(BUCKET_NAME, ""), withHyperlink("dir-1", "dir-1/"), "dir-2");
    assertThat(breadcrumbs.getText()).isEqualTo(withHtmlWrap(expectedBreadcrumbs));
  }

  private static String concatBreadcrumbs(String... elems) {
    return Arrays.stream(elems).collect(Collectors.joining(" / "));
  }

  private static String withHtmlWrap(String text) {
    return "<html><font face='sans' size='-1'>" + text + "</font></html>";
  }

  private static String withHyperlink(String name, String link) {
    return "<a href=\"" + link + "\">" + name + "</a>";
  }
}
