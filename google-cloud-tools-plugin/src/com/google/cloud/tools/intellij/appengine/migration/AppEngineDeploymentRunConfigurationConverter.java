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

package com.google.cloud.tools.intellij.appengine.migration;

import com.google.common.collect.ImmutableMap;
import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.ConversionProcessor;
import com.intellij.conversion.RunManagerSettings;
import java.util.function.Predicate;
import org.jdom.Element;

/**
 * Converter to migrate legacy deployment run configurations created by the old App Engine plugin to
 * work with this plugin.
 */
public class AppEngineDeploymentRunConfigurationConverter
    extends ConversionProcessor<RunManagerSettings> {

  private static ImmutableMap<String, String> legacyToNewType =
      ImmutableMap.<String, String>builder()
          .put("google-app-engine-deploy", "gcp-app-engine-deploy")
          .build();

  private Predicate<Element> isLegacyConfiguration =
      element -> legacyToNewType.containsKey(element.getAttributeValue("type"));

  @Override
  public boolean isConversionNeeded(RunManagerSettings runManagerSettings) {
    return runManagerSettings.getRunConfigurations().stream().anyMatch(isLegacyConfiguration);
  }

  @Override
  public void process(RunManagerSettings runManagerSettings) throws CannotConvertException {
    runManagerSettings
        .getRunConfigurations()
        .stream()
        .filter(isLegacyConfiguration)
        .forEach(
            element ->
                element.setAttribute(
                    "type", legacyToNewType.get(element.getAttributeValue("type"))));
  }
}
