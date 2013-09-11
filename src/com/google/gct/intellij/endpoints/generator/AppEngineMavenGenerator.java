package com.google.gct.intellij.endpoints.generator;

import com.google.gct.intellij.endpoints.GctConstants;
import com.google.gct.intellij.endpoints.generator.template.TemplateHelper;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.wizards.MavenProjectBuilder;

import java.io.IOException;

/**
 * This is class is general purpose AppEngine maven project creator, should be modified for specific use cases
 */
public abstract class AppEngineMavenGenerator {

  protected final Project myProject;
  protected final String myModuleName;
  protected final String myRootPackage;
  protected Module myModule;
  protected VirtualFile myModuleRootDir;

  private static final Logger LOG = Logger.getInstance(AppEngineMavenGenerator.class);

  public AppEngineMavenGenerator(Project project, String moduleName, String rootPackage) {
    myProject = project;
    myModuleName = moduleName;
    myRootPackage = rootPackage;
  }

  /** the callback may be ignored by the implementation */
  public abstract void generate(@Nullable Callback callback);

  /** Create the required directory stucture for the AppEngine project */
  protected VirtualFile createAppEngineDirStructure() throws IOException {

    return ApplicationManager.getApplication().runWriteAction(new ThrowableComputable<VirtualFile, IOException>() {

      @Override
      public VirtualFile compute() throws IOException {
        // Create the root directory
        VirtualFile appEngineModuleRootDir = myProject.getBaseDir().createChildDirectory(null, myModuleName);

        // Create the maven src dirs
        VirtualFile sourceRoot = appEngineModuleRootDir.createChildDirectory(null, GctConstants.APP_ENGINE_SRC_DIR)
          .createChildDirectory(null, GctConstants.APP_ENGINE_MAIN_DIR);
        sourceRoot.createChildDirectory(null, GctConstants.APP_ENGINE_WEBAPP_DIR);
        sourceRoot.createChildDirectory(null, GctConstants.APP_ENGINE_JAVA_DIR);

        // create the res dirs
        VirtualFile resourcesRoot = sourceRoot.createChildDirectory(null, GctConstants.APP_ENGINE_RES_DIR);
        resourcesRoot.createChildDirectory(null, GctConstants.APP_ENGINE_META_INF_DIR);

        // maybe we don't need to return here?
        myModuleRootDir = appEngineModuleRootDir;
        return appEngineModuleRootDir;
      }
    });
  }

  protected PsiFile addMavenFunctionality() throws IOException, ConfigurationException {

    final PsiFile pomFile = ApplicationManager.getApplication().runWriteAction(new ThrowableComputable<PsiFile, IOException>() {
      @Override
      public PsiFile compute() throws IOException {
        PsiManager psiManager = PsiManager.getInstance(myProject);
        PsiDirectory moduleDirectory = psiManager.findDirectory(myModuleRootDir);
        PsiFile pomFile = TemplateHelper.generatePomXml(myProject, myRootPackage, myModuleRootDir.getName());
        moduleDirectory.add(pomFile);
        return pomFile;
      }
    });

    final MavenProjectBuilder projectBuilder = new MavenProjectBuilder();
    projectBuilder.setRootDirectory(myProject, myModuleRootDir.getPath());
    projectBuilder.commit(myProject, null, new DefaultModulesProvider(myProject));
    myProject.save();
    return pomFile;
  }

  protected void waitForMavenImport() {
    MavenProjectsManager.getInstance(myProject).waitForResolvingCompletion();
    // TODO: I'm not sure if the above wait include the wait below, we should look into this
    MavenProjectsManager.getInstance(myProject).waitForPluginsResolvingCompletion();
  }


  /** callback after App Engine project is created */
  public static interface Callback {
    /** There are no guarantees on what thread this will return */
    void moduleCreated(Module appEngineModule);
    /** There are no guarantees on what thread this will return */
    void onFailure(String errorMessage);
  }

}
