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

package com.google.cloud.tools.intellij.appengine.facet;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkPanel;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.util.AppEngineUtilLegacy;
import com.google.cloud.tools.intellij.stats.UsageTrackerProvider;
import com.google.cloud.tools.intellij.util.GctTracking;

import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.framework.FrameworkTypeEx;
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModelListener;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportProvider;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.ArtifactRootElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * @author nik
 */
public class AppEngineSupportProvider extends FrameworkSupportInModuleProvider {

  private static final Logger LOG = Logger
      .getInstance("#com.intellij.appengine.facet.AppEngineSupportProvider");

  private static final CloudSdkService sdkService = CloudSdkService.getInstance();

  @NotNull
  @Override
  public FrameworkTypeEx getFrameworkType() {
    return AppEngineFrameworkType.getFrameworkType();
  }

  @Override
  public List<FrameworkDependency> getDependenciesFrameworkIds() {
    return AppEngineWebIntegration.getInstance().getAppEngineFrameworkDependencies();
  }

  @Override
  public boolean isEnabledForModuleType(@NotNull ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @Override
  public boolean isSupportAlreadyAdded(@NotNull Module module,
      @NotNull FacetsProvider facetsProvider) {
    return !facetsProvider.getFacetsByType(module, AppEngineFacet.ID).isEmpty();
  }

  @Nullable
  public static VirtualFile createFileFromTemplate(final String templateName,
      final VirtualFile parent, final String fileName) {
    parent.refresh(false, false);
    final FileTemplate template = FileTemplateManager.getDefaultInstance()
        .getJ2eeTemplate(templateName);
    try {
      final String text = template
          .getText(FileTemplateManager.getDefaultInstance().getDefaultProperties());
      VirtualFile file = parent.findChild(fileName);
      if (file == null) {
        file = parent.createChildData(AppEngineSupportProvider.class, fileName);
      }
      VfsUtil.saveText(file, text);
      return file;
    } catch (IOException ioe) {
      LOG.error(ioe);
      return null;
    }
  }

  private void addSupport(final Module module,
      final ModifiableRootModel rootModel,
      FrameworkSupportModel frameworkSupportModel) {
    FacetType<AppEngineFacet, AppEngineFacetConfiguration> facetType = AppEngineFacet
        .getFacetType();
    AppEngineFacet appEngineFacet = FacetManager.getInstance(module)
        .addFacet(facetType, facetType.getDefaultFacetName(), null);
    AppEngineWebIntegration webIntegration = AppEngineWebIntegration.getInstance();
    webIntegration.registerFrameworkInModel(frameworkSupportModel, appEngineFacet);
    final Artifact webArtifact = findOrCreateWebArtifact(appEngineFacet);

    final VirtualFile webDescriptorDir = webIntegration
        .suggestParentDirectoryForAppEngineWebXml(module, rootModel);
    if (webDescriptorDir != null) {
      VirtualFile descriptor = createFileFromTemplate(
          AppEngineTemplateGroupDescriptorFactory.APP_ENGINE_WEB_XML_TEMPLATE, webDescriptorDir,
          AppEngineUtilLegacy.APP_ENGINE_WEB_XML_NAME);
      if (descriptor != null) {
        webIntegration.addDescriptor(webArtifact, module.getProject(), descriptor);
      }
    }

    final Project project = module.getProject();
    webIntegration.addDevServerToModuleDependencies(rootModel);

    final Library apiJar = addProjectLibrary(module, "AppEngine API",
        sdkService.getUserLibraryPaths(),
        VirtualFile.EMPTY_ARRAY);
    rootModel.addLibraryEntry(apiJar);
    webIntegration.addLibraryToArtifact(apiJar, webArtifact, project);
  }

  @NotNull
  private static Artifact findOrCreateWebArtifact(AppEngineFacet appEngineFacet) {
    Module module = appEngineFacet.getModule();
    ArtifactType webArtifactType = AppEngineWebIntegration.getInstance()
        .getAppEngineWebArtifactType();
    final Collection<Artifact> artifacts = ArtifactUtil.getArtifactsContainingModuleOutput(module);
    for (Artifact artifact : artifacts) {
      if (webArtifactType.equals(artifact.getArtifactType())) {
        return artifact;
      }
    }
    ArtifactManager artifactManager = ArtifactManager.getInstance(module.getProject());
    PackagingElementFactory elementFactory = PackagingElementFactory.getInstance();
    ArtifactRootElement<?> root = elementFactory.createArtifactRootElement();
    elementFactory.getOrCreateDirectory(root, "WEB-INF/classes")
        .addOrFindChild(elementFactory.createModuleOutput(module));
    return artifactManager.addArtifact(module.getName(), webArtifactType, root);
  }

  private static Library addProjectLibrary(final Module module, final String name,
      final List<String> jarDirectories, final VirtualFile[] sources) {
    return new WriteAction<Library>() {
      protected void run(@NotNull final Result<Library> result) {
        final LibraryTable libraryTable = LibraryTablesRegistrar.getInstance()
            .getLibraryTable(module.getProject());
        Library library = libraryTable.getLibraryByName(name);
        if (library == null) {
          library = libraryTable.createLibrary(name);
          final Library.ModifiableModel model = library.getModifiableModel();
          for (String path : jarDirectories) {
            String url = VfsUtilCore.pathToUrl(path);
            VirtualFileManager.getInstance().refreshAndFindFileByUrl(url);
            model.addJarDirectory(url, false);
          }
          for (VirtualFile sourceRoot : sources) {
            model.addRoot(sourceRoot, OrderRootType.SOURCES);
          }
          model.commit();
        }
        result.setResult(library);
      }
    }.execute().getResultObject();
  }

  @NotNull
  @Override
  public FrameworkSupportInModuleConfigurable createConfigurable(
      @NotNull FrameworkSupportModel model) {
    return new AppEngineSupportConfigurable(model);
  }

  @TestOnly
  public static void setSdkPath(FrameworkSupportInModuleConfigurable configurable, String path) {
    ((AppEngineSupportConfigurable) configurable).cloudSdkPanel.setCloudSdkDirectoryText(path);
  }

  private class AppEngineSupportConfigurable extends FrameworkSupportInModuleConfigurable implements
      FrameworkSupportModelListener {

    private final FrameworkSupportModel myFrameworkSupportModel;
    private JPanel myMainPanel;
    private CloudSdkPanel cloudSdkPanel;

    private AppEngineSupportConfigurable(FrameworkSupportModel model) {
      myFrameworkSupportModel = model;
      model.addFrameworkListener(this);
    }

    public void frameworkSelected(@NotNull FrameworkSupportProvider provider) {
    }

    public void frameworkUnselected(@NotNull FrameworkSupportProvider provider) {
    }

    @Override
    public void wizardStepUpdated() {
    }

    @Override
    public void onFrameworkSelectionChanged(boolean selected) {
      if (selected) {
        cloudSdkPanel.reset();
      }
    }

    @Override
    public void addSupport(@NotNull Module module,
        @NotNull ModifiableRootModel rootModel,
        @NotNull ModifiableModelsProvider modifiableModelsProvider) {
      sdkService.setSdkHomePath(cloudSdkPanel.getCloudSdkDirectory());

      AppEngineSupportProvider.this
          .addSupport(module, rootModel, myFrameworkSupportModel);

      // Called when creating a new App Engine module from the 'new project' or 'new module' wizards
      // or upon adding App Engine 'Framework Support' to an existing module.
      UsageTrackerProvider.getInstance()
          .trackEvent(GctTracking.APP_ENGINE_ADD_SUPPORT)
          .ping();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
      return myMainPanel;
    }

    @SuppressWarnings("checkstyle:abbreviationaswordinname")
    private void createUIComponents() {
      cloudSdkPanel = new CloudSdkPanel(sdkService);
    }
  }
}
