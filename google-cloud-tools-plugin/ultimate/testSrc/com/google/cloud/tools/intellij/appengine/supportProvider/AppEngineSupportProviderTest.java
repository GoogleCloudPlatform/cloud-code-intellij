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

package com.google.cloud.tools.intellij.appengine.supportProvider;

import com.google.cloud.tools.intellij.appengine.facet.AppEngineFacet;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineFrameworkType;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineSupportProvider;

import com.intellij.appengine.AppEngineCodeInsightTestCase;
import com.google.cloud.tools.intellij.appengine.server.run.AppEngineServerConfigurationType;
import com.google.cloud.tools.intellij.compiler.artifacts.ArtifactsTestUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.facet.FacetManager;
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.javaee.JavaeeVersion;
import com.intellij.javaee.application.facet.JavaeeApplicationFacet;
import com.intellij.javaee.application.framework.JavaeeApplicationFrameworkType;
import com.intellij.javaee.application.framework.JavaeeApplicationFrameworkVersion;
import com.intellij.javaee.model.enums.WebAppVersion;
import com.intellij.javaee.run.configuration.CommonModel;
import com.google.cloud.tools.intellij.javaee.supportProvider.JavaeeFrameworkSupportProviderTestCase;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.framework.WebFrameworkType;
import com.intellij.javaee.web.framework.WebFrameworkVersion;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author nik
 */
public class AppEngineSupportProviderTest extends JavaeeFrameworkSupportProviderTestCase {
  public void testAppEngine() {
    setupAppEngine();
    addSupport();

    final AppEngineFacet appEngineFacet = getFacet(AppEngineFacet.ID);
    assertNull(FacetManager.getInstance(myModule).getFacetByType(WebFacet.ID));
    assertEmpty(appEngineFacet.getConfiguration().getFilesToEnhance());
    final String moduleName = myModule.getName();
    ArtifactsTestUtil.assertLayout(myProject, moduleName, "<root>\n" +
                                                          " WEB-INF/\n" +
                                                          "  classes/\n" +
                                                          "   module:" + moduleName + "\n" +
                                                          "  lib/\n" +
                                                          "   lib:AppEngine API(project)\n");
  }

  private void setupAppEngine() {
    FrameworkSupportInModuleConfigurable configurable = selectFramework(AppEngineFrameworkType.ID);
    AppEngineSupportProvider.setSdkPath(configurable, AppEngineCodeInsightTestCase.getSdkPath());
  }

  private void assertRunConfigurationCreated(Artifact artifactToDeploy) {
    List<RunConfiguration> list = RunManager.getInstance(myProject).getConfigurationsList(AppEngineServerConfigurationType.getInstance());
    CommonModel configuration = assertInstanceOf(assertOneElement(list), CommonModel.class);
    assertSameElements(configuration.getDeployedArtifacts(), artifactToDeploy);
  }

  public void testAppEngineWithWeb() {
    setupAppEngine();
    selectFramework(WebFacet.ID);
    selectVersion(WebFrameworkType.getInstance(), new WebFrameworkVersion(WebAppVersion.WebAppVersion_2_5));
    addSupport();

    getFacet(AppEngineFacet.ID);
    assertFileExist("web/WEB-INF/web.xml");
    assertFileExist("web/WEB-INF/appengine-web.xml");

    final String moduleName = myModule.getName();
    Artifact artifact = ArtifactsTestUtil.findArtifact(myProject, moduleName + ":war exploded");
    ArtifactsTestUtil.assertLayout(artifact.getRootElement(), "<root>\n" +
                                                              " javaee-resources:Web(" + moduleName + ")\n" +
                                                              " WEB-INF/\n" +
                                                              "  classes/\n" +
                                                              "   module:" + moduleName + "\n" +
                                                              "  lib/\n" +
                                                              "   lib:AppEngine API(project)\n");
    assertRunConfigurationCreated(artifact);
  }

  public void testAppEngineWithEar() {
    setupAppEngine();
    selectFramework(WebFacet.ID);
    selectFramework(JavaeeApplicationFacet.ID);
    selectVersion(WebFrameworkType.getInstance(), new WebFrameworkVersion(WebAppVersion.WebAppVersion_2_5));
    selectVersion(JavaeeApplicationFrameworkType.getInstance(), new JavaeeApplicationFrameworkVersion(JavaeeVersion.JAVAEE_6));
    addSupport();

    getFacet(AppEngineFacet.ID);
    assertFileExist("web/WEB-INF/web.xml");
    assertFileExist("web/WEB-INF/appengine-web.xml");
    assertFileExist("META-INF/application.xml");
    VirtualFile descriptor = assertFileExist("META-INF/appengine-application.xml");

    final String moduleName = myModule.getName();
    Artifact artifact = ArtifactsTestUtil.findArtifact(myProject, moduleName + ":ear exploded");
    ArtifactsTestUtil.assertLayout(artifact.getRootElement(), "<root>\n" +
                                                              " javaee-resources:javaEEApplication(" + moduleName + ")\n" +
                                                              " web.war/\n" +
                                                              "  artifact:" + moduleName + ":war exploded\n" +
                                                              " META-INF/\n" +
                                                              "  file:" + descriptor.getPath() + "\n");
    assertRunConfigurationCreated(artifact);
  }

  @NotNull
  private VirtualFile assertFileExist(String relativePath) {
    VirtualFile file = getContentRoot().findFileByRelativePath(relativePath);
    assertNotNull("File not found: " + relativePath, file);
    return file;
  }
}
