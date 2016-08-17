/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.appengine.sdk.impl;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;

import com.intellij.facet.ui.FacetConfigurationQuickFix;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.BrowserUtil;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import javax.swing.JComponent;

/**
 * @author nik
 */
// TODO get rid of this class
public class AppEngineSdkUtil {

  @NonNls
  public static final String APP_ENGINE_DOWNLOAD_URL
      = "http://code.google.com/appengine/downloads.html#Google_App_Engine_SDK_for_Java";
  private static final FacetConfigurationQuickFix DOWNLOAD_SDK_QUICK_FIX
      = new FacetConfigurationQuickFix("Download...") {
          @Override
          public void run(JComponent place) {
            BrowserUtil.browse(APP_ENGINE_DOWNLOAD_URL);
          }
        };

  private AppEngineSdkUtil() {
  }

  @NotNull
  public static ValidationResult checkPath(String path) {
    final File toolsApiJarFile = CloudSdkService.getInstance().getToolsApiJarFile();
    if (toolsApiJarFile == null || !toolsApiJarFile.exists()) {
      return createNotFoundMessage(path, toolsApiJarFile);
    }

    return ValidationResult.OK;
  }

  private static ValidationResult createNotFoundMessage(@NotNull String path, @NotNull File file) {
    return new ValidationResult(
        "'" + path + "' is not valid App Engine SDK installation: " + "'" + file
            + "' file not found",
        DOWNLOAD_SDK_QUICK_FIX);
  }
}
