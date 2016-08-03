/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.converter;

import com.google.cloud.tools.intellij.appengine.facet.AppEngineFacetType;
import com.google.cloud.tools.intellij.util.Plugins;

import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.ConversionProcessor;
import com.intellij.conversion.ModuleSettings;

import org.jdom.Element;
import org.jetbrains.jps.model.serialization.facet.JpsFacetSerializer;

public class AppEngineFacetMigrationConversionProcessor extends
    ConversionProcessor<ModuleSettings> {

  final static String DEPRECATED_PLUGIN_ID = "com.intellij.appengine";
  final static String DEPRECATED_APP_ENGINE_FACET_ID = "google-app-engine";

  private Plugins plugins;

  public AppEngineFacetMigrationConversionProcessor(Plugins plugins) {
    this.plugins = plugins;
  }

  @Override
  public boolean isConversionNeeded(ModuleSettings settings) {
    boolean deprecatedPluginIsNotInstalled = !plugins.isPluginInstalled(DEPRECATED_PLUGIN_ID);
    boolean deprecatedFacetsArePresent =
        !settings.getFacetElements(DEPRECATED_APP_ENGINE_FACET_ID).isEmpty();
    return deprecatedPluginIsNotInstalled && deprecatedFacetsArePresent;
  }

  @Override
  public void process(ModuleSettings settings) throws CannotConvertException {
    for (Element tag : settings.getFacetElements(DEPRECATED_APP_ENGINE_FACET_ID)) {
      // remove the old tag
      tag.detach();

      Element configuration = tag.getChild(JpsFacetSerializer.CONFIGURATION_TAG);
      String facetName = tag.getAttributeValue(JpsFacetSerializer.NAME_ATTRIBUTE);

      // add a new tag with all the same settings as the old one, but the new facet type id
      settings.addFacetElement(AppEngineFacetType.STRING_ID, facetName, configuration.clone());
    }
  }
}
