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

package com.google.cloud.tools.intellij.jps.model.impl;

import com.google.cloud.tools.intellij.jps.model.JpsStackdriverModuleExtension;

import com.intellij.util.xmlb.XmlSerializer;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.facet.JpsFacetConfigurationSerializer;

import java.util.Collections;
import java.util.List;

/**
 * Created by joaomartins on 11/3/16.
 */
public class JpsStackdriverModelSerializerExtension extends JpsModelSerializerExtension {

  @NotNull
  @Override
  public List<? extends JpsFacetConfigurationSerializer<?>> getFacetConfigurationSerializers() {
    return Collections.singletonList(new JpsStackdriverModuleExtensionSerializer());
  }

  private static class JpsStackdriverModuleExtensionSerializer
      extends JpsFacetConfigurationSerializer<JpsStackdriverModuleExtension> {

    public JpsStackdriverModuleExtensionSerializer() {
      super(JpsStackdriverModuleExtensionImpl.ROLE, "stackdriver", "Google Stackdriver Debugger");
    }

    @Override
    protected JpsStackdriverModuleExtension loadExtension(
        @NotNull Element facetConfigurationElement,
        String name, JpsElement parent, JpsModule module) {
      StackdriverProperties properties = XmlSerializer
          .deserialize(facetConfigurationElement, StackdriverProperties.class);
      return new JpsStackdriverModuleExtensionImpl(
          properties != null ? properties : new StackdriverProperties()
      );
    }

    @Override
    protected void saveExtension(JpsStackdriverModuleExtension extension,
        Element facetConfigurationTag, JpsModule module) {
      XmlSerializer.serializeInto(((JpsStackdriverModuleExtensionImpl) extension).getProperties(),
          facetConfigurationTag);
    }
  }
}
