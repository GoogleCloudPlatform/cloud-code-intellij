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

import org.jdom.Element;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Converter to migrate legacy run configurations created by the old App Engine plugin to work with
 * this plugin.
 */
public class AppEngineRunConfigurationConverter
    extends ConversionProcessor<RunManagerSettings> {

  private static ImmutableMap<String, String> legacyDeployTypeToNewType
      = ImmutableMap.<String, String>builder()
      .put("google-app-engine-deploy", "gcp-app-engine-deploy")
      .build();

  private static ImmutableMap<String, String> legacyLocalRunTypeToNewType
      = ImmutableMap.<String, String>builder()
      .put("GoogleAppEngineDevServer", "gcp-app-engine-local-run")
      .build();

  private Predicate<Element> isLegacyDeployConfiguration
      = element -> legacyDeployTypeToNewType.containsKey(element.getAttributeValue("type"));
  private Predicate<Element> isLegacyLocalRunConfiguration
      = element -> legacyLocalRunTypeToNewType.containsKey(element.getAttributeValue("type"));

  @Override
  public boolean isConversionNeeded(RunManagerSettings runManagerSettings) {
    return runManagerSettings.getRunConfigurations()
        .stream()
        .anyMatch(isLegacyDeployConfiguration.or(isLegacyLocalRunConfiguration));
  }

  @Override
  public void process(RunManagerSettings runManagerSettings) throws CannotConvertException {
    Collection<? extends Element> runConfigs = runManagerSettings.getRunConfigurations();

    processDeployConfigurations(runConfigs.stream().filter(isLegacyDeployConfiguration));
    processLocalRunConfigurations(runConfigs.stream().filter(isLegacyLocalRunConfiguration));
  }

  private void processDeployConfigurations(Stream<? extends Element> deployConfigurations) {
    deployConfigurations
        .forEach(element -> {
          element.setAttribute("type",
              legacyDeployTypeToNewType.get(element.getAttributeValue("type")));
          updateName(element);
        });
  }

  private void processLocalRunConfigurations(Stream<? extends Element> localRunConfigurations) {
    localRunConfigurations
        .forEach(element -> {
          element.setAttribute("type",
              legacyLocalRunTypeToNewType.get(element.getAttributeValue("type")));
          updateName(element);
        });
  }

  private void updateName(Element element) {
    element.setAttribute("name", element.getAttributeValue("name") + " (migrated)");
  }
}
