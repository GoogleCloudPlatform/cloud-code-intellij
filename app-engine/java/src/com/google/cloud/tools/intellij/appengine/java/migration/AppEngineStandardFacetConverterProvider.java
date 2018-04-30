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

package com.google.cloud.tools.intellij.appengine.java.migration;

import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacetType;
import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.ConversionContext;
import com.intellij.conversion.ConversionProcessor;
import com.intellij.conversion.ConverterProvider;
import com.intellij.conversion.ModuleSettings;
import com.intellij.conversion.ProjectConverter;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.containers.ContainerUtil;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.facet.JpsFacetSerializer;

/** @author nik */
public class AppEngineStandardFacetConverterProvider extends ConverterProvider {

  public AppEngineStandardFacetConverterProvider() {
    super("google-app-engine-facet");
  }

  @NotNull
  @Override
  public ProjectConverter createConverter(@NotNull ConversionContext context) {
    return new ProjectConverter() {
      @Nullable
      @Override
      public ConversionProcessor<ModuleSettings> createModuleFileConverter() {
        return new GoogleAppEngineFacetConversionProcessor();
      }
    };
  }

  @NotNull
  @Override
  public String getConversionDescription() {
    return "Google App Engine facets will be decoupled from Web facets";
  }

  private static class GoogleAppEngineFacetConversionProcessor
      extends ConversionProcessor<ModuleSettings> {

    @Override
    public boolean isConversionNeeded(ModuleSettings settings) {
      return !getAppEngineFacetTags(settings).isEmpty();
    }

    @Override
    public void process(ModuleSettings settings) throws CannotConvertException {
      List<Element> facetTags = getAppEngineFacetTags(settings);
      for (Element tag : facetTags) {
        tag.detach();
      }
      Element facetTag = ContainerUtil.getFirstItem(facetTags);
      if (facetTag != null) {
        String facetName = facetTag.getAttributeValue(JpsFacetSerializer.NAME_ATTRIBUTE);
        Element configuration = facetTag.getChild(JpsFacetSerializer.CONFIGURATION_TAG);
        settings.addFacetElement(
            AppEngineStandardFacetType.STRING_ID, facetName, (Element) configuration.clone());
      }
    }

    @NotNull
    private static List<Element> getAppEngineFacetTags(@NotNull ModuleSettings settings) {
      List<Element> appEngineFacetTags = new ArrayList<Element>();
      for (Element webFacetTag : settings.getFacetElements("web")) {
        for (Element childFacetTag :
            JDOMUtil.getChildren(webFacetTag, JpsFacetSerializer.FACET_TAG)) {
          if (AppEngineStandardFacetType.STRING_ID.equals(
              childFacetTag.getAttributeValue(JpsFacetSerializer.TYPE_ATTRIBUTE))) {
            appEngineFacetTags.add(childFacetTag);
          }
        }
      }
      return appEngineFacetTags;
    }
  }
}
