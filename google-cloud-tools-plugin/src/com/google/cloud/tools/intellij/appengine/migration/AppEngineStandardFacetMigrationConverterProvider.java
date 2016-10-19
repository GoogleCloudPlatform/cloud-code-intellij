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

package com.google.cloud.tools.intellij.appengine.migration;

import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.conversion.ConversionContext;
import com.intellij.conversion.ConversionProcessor;
import com.intellij.conversion.ConverterProvider;
import com.intellij.conversion.ModuleSettings;
import com.intellij.conversion.ProjectConverter;

import org.jetbrains.annotations.NotNull;

/**
 * Provides converters for an App Engine Facet Migration. This migration migrates facets that use
 * a deprecated facet ID to a new facet ID.
 */
public class AppEngineStandardFacetMigrationConverterProvider extends ConverterProvider {

  public AppEngineStandardFacetMigrationConverterProvider() {
    super("google-app-engine-facet-migration");
  }

  @NotNull
  @Override
  public ProjectConverter createConverter(@NotNull ConversionContext context) {
    return new ProjectConverter() {
      @Override
      public ConversionProcessor<ModuleSettings> createModuleFileConverter() {
        return new AppEngineStandardFacetMigrationConversionProcessor();
      }
    };
  }

  @NotNull
  @Override
  public String getConversionDescription() {
    return GctBundle.message("appengine.facet.converter.description");
  }

}
