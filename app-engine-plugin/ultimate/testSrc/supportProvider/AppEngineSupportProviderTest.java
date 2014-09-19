package com.intellij.appengine.supportProvider;

import com.intellij.appengine.AppEngineCodeInsightTestCase;
import com.intellij.appengine.facet.AppEngineFacet;
import com.intellij.appengine.facet.AppEngineFrameworkType;
import com.intellij.appengine.facet.AppEngineSupportProvider;
import com.intellij.appengine.server.run.AppEngineServerConfigurationType;
import com.intellij.compiler.artifacts.ArtifactsTestUtil;
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
import com.intellij.javaee.supportProvider.JavaeeFrameworkSupportProviderTestCase;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.framework.WebFrameworkType;
import com.intellij.javaee.web.framework.WebFrameworkVersion;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * @author nik
 */
public class AppEngineSupportProviderTest extends JavaeeFrameworkSupportProviderTestCase {
  public void testAppEngine() throws IOException {
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

  public void testAppEngineWithWeb() throws IOException {
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

  public void testAppEngineWithEar() throws IOException {
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
