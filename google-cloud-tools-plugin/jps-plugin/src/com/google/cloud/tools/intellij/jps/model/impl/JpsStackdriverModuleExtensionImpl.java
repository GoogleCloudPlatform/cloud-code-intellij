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

import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.ex.JpsElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JpsStackdriverModuleExtensionImpl
    extends JpsElementBase<JpsStackdriverModuleExtensionImpl>
    implements JpsStackdriverModuleExtension {

  private StackdriverProperties properties;
  public static final JpsElementChildRole<JpsStackdriverModuleExtension> ROLE =
      JpsElementChildRoleBase.create("Stackdriver");

  public JpsStackdriverModuleExtensionImpl(StackdriverProperties properties) {
    this.properties = properties;
  }

  @Override
  public boolean isGenerateSourceContext() {
    return properties.isGenerateSourceContext();
  }

  @Override
  public boolean isIgnoreErrors() {
    return properties.isIgnoreErrors();
  }

  @Override
  public Path getCloudSdkPath() {
    // TODO(joaomartins): Validate path when validators are in the common library. Do it here, or
    // in StackdriverProperties, when path is set.
    return properties.getCloudSdkPath() != null ? Paths.get(properties.getCloudSdkPath()) : null;
  }

  @Override
  public Path getModuleSourceDirectory() {
    try {
      return properties.getModuleSourceDirectory() != null
          ? Paths.get(properties.getModuleSourceDirectory()) : null;
    } catch (InvalidPathException ipe) {
      return null;
    }
  }

  @NotNull
  @Override
  public BulkModificationSupport<?> getBulkModificationSupport() {
    return null;
  }

  @NotNull
  @Override
  public JpsStackdriverModuleExtensionImpl createCopy() {
    return new JpsStackdriverModuleExtensionImpl(XmlSerializerUtil.createCopy(properties));
  }

  @Override
  public void applyChanges(@NotNull JpsStackdriverModuleExtensionImpl modified) {
    XmlSerializerUtil.copyBean(modified.getProperties(), properties);
  }

  public StackdriverProperties getProperties() {
    return properties;
  }
}
