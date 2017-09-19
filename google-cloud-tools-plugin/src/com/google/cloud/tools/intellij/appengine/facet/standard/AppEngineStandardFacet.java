/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.google.cloud.tools.intellij.appengine.facet.standard;

import com.google.cloud.tools.intellij.appengine.descriptor.dom.AppEngineStandardWebApp;
import com.google.cloud.tools.intellij.appengine.project.AppEngineAssetProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import org.apache.commons.lang3.JavaVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @author nik */
public class AppEngineStandardFacet extends Facet<AppEngineStandardFacetConfiguration> {

  private static final ImmutableMap<String, JavaVersion> RUNTIMES_MAP =
      ImmutableMap.of(
          "java",
          JavaVersion.JAVA_1_7,
          "java7",
          JavaVersion.JAVA_1_7,
          "java8",
          JavaVersion.JAVA_1_8);

  public AppEngineStandardFacet(
      @NotNull FacetType facetType,
      @NotNull Module module,
      @NotNull String name,
      @NotNull AppEngineStandardFacetConfiguration configuration) {
    super(facetType, module, name, configuration, null);
  }

  public static FacetType<AppEngineStandardFacet, AppEngineStandardFacetConfiguration>
      getFacetType() {
    return FacetTypeRegistry.getInstance().findFacetType(AppEngineStandardFacetType.ID);
  }

  @Nullable
  public static AppEngineStandardFacet getAppEngineFacetByModule(@Nullable Module module) {
    if (module == null) {
      return null;
    }
    return FacetManager.getInstance(module).getFacetByType(AppEngineStandardFacetType.ID);
  }

  /**
   * Returns {@code true} if this app is targeting the Flexible Environment instead of the Standard
   * Environment.
   */
  public boolean isFlexCompatEnvironment() {
    AppEngineStandardWebApp appEngineStandardWebApp = getAppEngineStandardWebXml();
    return appEngineStandardWebApp != null
        && "flex".equalsIgnoreCase(appEngineStandardWebApp.getEnv().getStringValue());
  }

  /**
   * Returns {@code true} if this app is targeting the Managed VM environment instead of the
   * Standard Environment.
   */
  public boolean isManagedVmCompatEnvironment() {
    AppEngineStandardWebApp appEngineStandardWebApp = getAppEngineStandardWebXml();
    return appEngineStandardWebApp != null
        && "true".equalsIgnoreCase(appEngineStandardWebApp.getVm().getStringValue());
  }

  /**
   * Returns {@code true} if this app is targeting some environment other than the Standard
   * Environment.
   */
  public boolean isNonStandardCompatEnvironment() {
    return isFlexCompatEnvironment() || isManagedVmCompatEnvironment();
  }

  /**
   * Returns the {@link JavaVersion} of the Java runtime, as defined in the {@code
   * appengine-web.xml}.
   *
   * <p>If no runtime is specified in the {@code appengine-web.xml} or if the specified runtime is
   * not valid, {@link JavaVersion#JAVA_1_7} is returned.
   */
  public JavaVersion getRuntimeJavaVersion() {
    AppEngineStandardWebApp appEngineStandardWebApp = getAppEngineStandardWebXml();
    if (appEngineStandardWebApp != null) {
      String runtimeString = appEngineStandardWebApp.getRuntime().getStringValue();
      JavaVersion runtimeVersion = RUNTIMES_MAP.get(runtimeString);
      if (runtimeVersion != null) {
        return runtimeVersion;
      }
    }

    // The default runtime is Java 7.
    return JavaVersion.JAVA_1_7;
  }

  @Nullable
  public AppEngineStandardWebApp getAppEngineStandardWebXml() {
    XmlFile appengineWebXmlFile =
        AppEngineAssetProvider.getInstance()
            .loadAppEngineStandardWebXml(getModule().getProject(), ImmutableList.of(getModule()));
    final DomManager domManager = DomManager.getDomManager(getModule().getProject());
    DomFileElement<AppEngineStandardWebApp> appEngineWebXmlDom =
        domManager.getFileElement(appengineWebXmlFile, AppEngineStandardWebApp.class);
    if (appEngineWebXmlDom == null) {
      return null;
    }
    return appEngineWebXmlDom.getRootElement();
  }
}
