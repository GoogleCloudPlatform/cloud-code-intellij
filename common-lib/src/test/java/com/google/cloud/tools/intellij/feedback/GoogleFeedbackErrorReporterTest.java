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

package com.google.cloud.tools.intellij.feedback;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.intellij.errorreport.bean.ErrorBean;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/** Test cases for {@link GoogleFeedbackErrorReporter}. */
@RunWith(MockitoJUnitRunner.class)
public class GoogleFeedbackErrorReporterTest {

  private static final String TEST_MESSAGE = "Test message";
  private static final String LAST_ACTION = "last action";
  private static final String FULL_PRODUCT_NAME = "full product name";
  private static final String PACKAGE_CODE = "package code";
  private static final String VERSION_NAME = "version name";
  private static final String MAJOR_VERSION = "major version";
  private static final String MINOR_VERSION = "minor version";
  private static final String PLUGIN_VERSION = "plugin version";

  @Mock private ApplicationNamesInfo mockAppNameInfo;

  @Mock private ApplicationInfoEx mockAppInfoEx;

  @Mock private Application mockApplication;
  private ErrorBean error;

  @Before
  public void setUp() {
    error = new ErrorBean(new Throwable(TEST_MESSAGE), LAST_ACTION);
    error.setPluginVersion(PLUGIN_VERSION);
    when(mockAppNameInfo.getFullProductName()).thenReturn(FULL_PRODUCT_NAME);
    when(mockAppInfoEx.getPackageCode()).thenReturn(PACKAGE_CODE);
    when(mockAppInfoEx.getVersionName()).thenReturn(VERSION_NAME);
    when(mockAppInfoEx.getMajorVersion()).thenReturn(MAJOR_VERSION);
    when(mockAppInfoEx.getMinorVersion()).thenReturn(MINOR_VERSION);
  }

  @Test
  public void testBuildKeyValuesMap_trueFlags() throws Exception {
    when(mockAppInfoEx.isEAP()).thenReturn(true);
    when(mockApplication.isInternal()).thenReturn(true);
    Map<String, String> result =
        GoogleFeedbackErrorReporter.buildKeyValuesMap(
            error, mockAppNameInfo, mockAppInfoEx, mockApplication);
    assertEquals(TEST_MESSAGE, result.get(GoogleFeedbackErrorReporter.ERROR_MESSAGE_KEY));
    assertEquals(LAST_ACTION, result.get(GoogleFeedbackErrorReporter.LAST_ACTION_KEY));
    assertEquals(FULL_PRODUCT_NAME, result.get(GoogleFeedbackErrorReporter.APP_NAME_KEY));
    assertEquals(PACKAGE_CODE, result.get(GoogleFeedbackErrorReporter.APP_CODE_KEY));
    assertEquals(Boolean.TRUE.toString(), result.get(GoogleFeedbackErrorReporter.APP_EAP_KEY));
    assertEquals(Boolean.TRUE.toString(), result.get(GoogleFeedbackErrorReporter.APP_INTERNAL_KEY));
    assertEquals(MAJOR_VERSION, result.get(GoogleFeedbackErrorReporter.APP_VERSION_MAJOR_KEY));
    assertEquals(MINOR_VERSION, result.get(GoogleFeedbackErrorReporter.APP_VERSION_MINOR_KEY));
    assertEquals(PLUGIN_VERSION, result.get(GoogleFeedbackErrorReporter.PLUGIN_VERSION));
  }

  @Test
  public void testBuildKeyValuesMap_falseFlags() throws Exception {
    when(mockAppInfoEx.isEAP()).thenReturn(false);
    when(mockApplication.isInternal()).thenReturn(false);
    Map<String, String> result =
        GoogleFeedbackErrorReporter.buildKeyValuesMap(
            error, mockAppNameInfo, mockAppInfoEx, mockApplication);
    assertEquals(Boolean.FALSE.toString(), result.get(GoogleFeedbackErrorReporter.APP_EAP_KEY));
    assertEquals(
        Boolean.FALSE.toString(), result.get(GoogleFeedbackErrorReporter.APP_INTERNAL_KEY));
  }

  @Test
  public void testNullToNone_nullString() throws Exception {
    Assert.assertEquals(
        GoogleFeedbackErrorReporter.NONE_STRING, GoogleFeedbackErrorReporter.nullToNone(null));
  }

  @Test
  public void testNullToNone_notNullString() throws Exception {
    String validString = "test";
    assertEquals(validString, GoogleFeedbackErrorReporter.nullToNone(validString));
  }
}
