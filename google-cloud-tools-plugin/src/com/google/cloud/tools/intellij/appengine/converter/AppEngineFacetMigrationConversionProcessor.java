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

import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.ConversionProcessor;
import com.intellij.conversion.ModuleSettings;

import org.jdom.Element;
import org.jetbrains.jps.model.serialization.facet.JpsFacetSerializer;

/**
 * Performs conversions from deprecated App Engine Facets to their new version.
 */
public class AppEngineFacetMigrationConversionProcessor extends
    ConversionProcessor<ModuleSettings> {

  static final String DEPRECATED_APP_ENGINE_FACET_ID = "google-app-engine";

  @Override
  public boolean isConversionNeeded(ModuleSettings settings) {
    return !settings.getFacetElements(DEPRECATED_APP_ENGINE_FACET_ID).isEmpty();
  }

  @Override
  public void process(ModuleSettings settings) throws CannotConvertException {
    for (Element deprecatedTag : settings.getFacetElements(DEPRECATED_APP_ENGINE_FACET_ID)) {
      // remove the old tag
      deprecatedTag.detach();

      Element configuration = deprecatedTag.getChild(JpsFacetSerializer.CONFIGURATION_TAG);
      String facetName = deprecatedTag.getAttributeValue(JpsFacetSerializer.NAME_ATTRIBUTE);

      // add a new tag with all the same settings as the old one, but the new facet type id
      settings.addFacetElement(AppEngineFacetType.STRING_ID, facetName, configuration.clone());
    }
  }
}
