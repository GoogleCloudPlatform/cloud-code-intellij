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
import com.intellij.conversion.ConversionContext;
import com.intellij.conversion.ConversionProcessor;
import com.intellij.conversion.ConverterProvider;
import com.intellij.conversion.ModuleSettings;
import com.intellij.conversion.ProjectConverter;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.containers.ContainerUtil;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.facet.JpsFacetSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class AppEngineFacetMigrationConverterProvider extends ConverterProvider {

  private final static String DEPRECATED_PLUGIN_ID = "com.intellij.appengine";
  private final static String DEPRECATED_APP_ENGINE_FACET_ID = "google-app-engine";

  public AppEngineFacetMigrationConverterProvider() {
    super("google-app-engine-facet-migration");
  }

  @NotNull
  @Override
  public ProjectConverter createConverter(@NotNull ConversionContext context) {
    return new ProjectConverter() {
      @Override
      public ConversionProcessor<ModuleSettings> createModuleFileConverter() {
        return new AppEngineFacetMigrationConversionProcessor();
      }
    };
  }

  @NotNull
  @Override
  public String getConversionDescription() {
    return "Deprecated Google App Engine facets will be replaced with the latest version";
  }

  private static class AppEngineFacetMigrationConversionProcessor extends
      ConversionProcessor<ModuleSettings> {

    @Override
    public boolean isConversionNeeded(ModuleSettings settings) {
      boolean deprecatedPluginIsNotInstalled = !Plugins.isPluginInstalled(DEPRECATED_PLUGIN_ID);
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
}
