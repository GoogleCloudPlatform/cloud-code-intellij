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
import com.google.cloud.tools.intellij.appengine.util.AppEngineUtil;
import com.google.cloud.tools.intellij.stats.UsageTrackerProvider;
import com.google.cloud.tools.intellij.util.GctTracking;
import com.google.common.annotations.VisibleForTesting;

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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.ArtifactRootElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.util.PlatformUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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
    if (PlatformUtils.isIdeaUltimate()) {
      return moduleType instanceof JavaModuleType;
    } else {
      return false;
    }
  }

  @Override
  public boolean isSupportAlreadyAdded(@NotNull Module module,
      @NotNull FacetsProvider facetsProvider) {
    return !facetsProvider.getFacetsByType(module, AppEngineStandardFacet.ID).isEmpty();
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
      FrameworkSupportModel frameworkSupportModel,
      Set<AppEngineStandardMavenLibrary> librariesToAdd) {
    FacetType<AppEngineStandardFacet, AppEngineFacetConfiguration> facetType
        = AppEngineStandardFacet.getFacetType();
    AppEngineStandardFacet appEngineStandardFacet = FacetManager.getInstance(module)
        .addFacet(facetType, facetType.getDefaultFacetName(), null);
    AppEngineWebIntegration webIntegration = AppEngineWebIntegration.getInstance();
    webIntegration.registerFrameworkInModel(frameworkSupportModel, appEngineStandardFacet);
    final Artifact webArtifact = findOrCreateWebArtifact(appEngineStandardFacet);

    final VirtualFile webDescriptorDir = webIntegration
        .suggestParentDirectoryForAppEngineWebXml(module, rootModel);
    if (webDescriptorDir != null) {
      VirtualFile descriptor = createFileFromTemplate(
          AppEngineTemplateGroupDescriptorFactory.APP_ENGINE_WEB_XML_TEMPLATE, webDescriptorDir,
          AppEngineUtil.APP_ENGINE_WEB_XML_NAME);
      if (descriptor != null) {
        webIntegration.addDescriptor(webArtifact, module.getProject(), descriptor);
      }
    }

    addMavenLibraries(librariesToAdd, module, rootModel, webArtifact);
  }

  @NotNull
  static Artifact findOrCreateWebArtifact(AppEngineStandardFacet appEngineStandardFacet) {
    Module module = appEngineStandardFacet.getModule();
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

  static void addMavenLibraries(
      final Set<AppEngineStandardMavenLibrary> librariesToAdd, final Module module,
      final ModifiableRootModel rootModel, final Artifact webArtifact) {
    new WriteAction() {
      @Override
      protected void run(@NotNull Result result) throws Throwable {
        if (librariesToAdd != null && !librariesToAdd.isEmpty()) {
          for (AppEngineStandardMavenLibrary libraryToAdd : librariesToAdd) {
            Library mavenLibrary = loadMavenLibrary(module, libraryToAdd);
            if (mavenLibrary != null) {
              rootModel.addLibraryEntry(mavenLibrary).setScope(libraryToAdd.getScope());
              AppEngineWebIntegration.getInstance()
                  .addLibraryToArtifact(mavenLibrary, webArtifact, module.getProject());

              UsageTrackerProvider.getInstance()
                  .trackEvent(GctTracking.APP_ENGINE_ADD_LIBRARY)
                  .withLabel(libraryToAdd.name())
                  .ping();
            } else {
              LOG.warn("Failed to load library: " + libraryToAdd.getDisplayName());
            }
          }
        }
      }
    }.execute();
  }

  static void removeMavenLibraries(final Set<AppEngineStandardMavenLibrary> librariesToRemove,
      final Module module) {
    final ModuleRootManager manager = ModuleRootManager.getInstance(module);
    final ModifiableRootModel model = manager.getModifiableModel();
    final LibraryTable libraryTable = ProjectLibraryTable.getInstance(module.getProject());

    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        new WriteAction() {
          @Override
          protected void run(@NotNull Result result) throws Throwable {
            for (AppEngineStandardMavenLibrary libraryToRemove : librariesToRemove) {
              final String displayName = AppEngineStandardMavenLibrary
                  .toMavenDisplayVersion(libraryToRemove.getLibraryProperties());
              final Library library = libraryTable.getLibraryByName(displayName);
              if (library != null) {
                libraryTable.removeLibrary(library);

                for (OrderEntry orderEntry : model.getOrderEntries()) {
                  if (orderEntry.getPresentableName().equals(library.getName())) {
                    model.removeOrderEntry(orderEntry);
                  }
                }

              }
            }
            model.commit();
          }
        }.execute();
      }
    }, ModalityState.NON_MODAL);
  }

  static Library loadMavenLibrary(final Module module, AppEngineStandardMavenLibrary library) {
    RepositoryLibraryProperties libraryProperties = library.getLibraryProperties();

    return MavenRepositoryLibraryDownloader.getInstance().downloadLibrary(module,
        RepositoryLibraryDescription.findDescription(libraryProperties), libraryProperties,
        libraryProperties.getVersion());
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

  @VisibleForTesting
  public class AppEngineSupportConfigurable extends FrameworkSupportInModuleConfigurable implements
      FrameworkSupportModelListener {

    private final FrameworkSupportModel frameworkSupportModel;
    private JPanel mainPanel;
    private CloudSdkPanel cloudSdkPanel;
    private AppEngineStandardLibraryPanel appEngineStandardLibraryPanel;

    private AppEngineSupportConfigurable(FrameworkSupportModel model) {
      frameworkSupportModel = model;
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
      sdkService.setSdkHomePath(cloudSdkPanel.getCloudSdkDirectoryText());

      AppEngineSupportProvider.this
          .addSupport(module, rootModel, frameworkSupportModel,
              appEngineStandardLibraryPanel.getSelectedLibraries());

      // Called when creating a new App Engine module from the 'new project' or 'new module' wizards
      // or upon adding App Engine 'Framework Support' to an existing module.
      UsageTrackerProvider.getInstance()
          .trackEvent(GctTracking.APP_ENGINE_ADD_SUPPORT)
          .ping();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
      return mainPanel;
    }

    @VisibleForTesting
    public void setAppEngineStandardLibraryPanel(
        AppEngineStandardLibraryPanel appEngineStandardLibraryPanel) {
      this.appEngineStandardLibraryPanel = appEngineStandardLibraryPanel;
    }

    @SuppressWarnings("checkstyle:abbreviationaswordinname")
    private void createUIComponents() {
      cloudSdkPanel = new CloudSdkPanel();
      appEngineStandardLibraryPanel = new AppEngineStandardLibraryPanel(true /*enabled*/);
    }
  }
}
