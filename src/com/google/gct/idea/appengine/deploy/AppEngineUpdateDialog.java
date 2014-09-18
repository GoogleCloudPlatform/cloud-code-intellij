/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.appengine.deploy;

import com.google.common.base.Strings;
import com.google.gct.idea.appengine.dom.AppEngineWebApp;
import com.google.gct.idea.appengine.dom.AppEngineWebFileDescription;
import com.google.gct.idea.appengine.gradle.facet.AppEngineConfigurationProperties;
import com.google.gct.idea.appengine.gradle.facet.AppEngineGradleFacet;
import com.google.gct.login.GoogleLogin;
import com.google.gct.login.IGoogleLoginCompletedCallback;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesCombobox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.SortedComboBoxModel;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * AppEngineUpdateDialog shows a dialog allowing the user to select a module and deploy.
 */
public class AppEngineUpdateDialog extends DialogWrapper {
  private static final Logger LOG = Logger.getInstance(AppEngineUpdateDialog.class);

  private ModulesCombobox myModuleComboBox;
  private JTextField myProjectId;
  private JTextField myVersion;
  private JPanel myPanel;
  private List<Module> myDeployableModules;
  private Project myProject;
  private Module myInitiallySelectedModule;

  private AppEngineUpdateDialog(Project project, List<Module> deployableModules, Module selectedModule) {
    super(project, true);
    myDeployableModules = deployableModules;
    myProject = project;
    myInitiallySelectedModule = selectedModule;

    init();
    initValidation();
    setTitle("Deploy to App Engine");
    setOKButtonText("Deploy");

    Window myWindow = getWindow();
    if (myWindow != null) {
      myWindow.setPreferredSize(new Dimension(285, 135));
    }
  }

  /**
   * Shows a dialog to deploy a module to AppEngine.  Will force a login if required
   * If either the login fails or there are no valid modules to upload, it will return  after
   * displaying an error.
   *
   * @param project The project whose modules will be uploaded.
   * @param selectedModule The module selected by default in the deploy dialog.  Can be null.  If null or not a valid app engine module,
   *                       no module will be selected by default.
   */
  static void show(final Project project, Module selectedModule) {

    final java.util.List<Module> modules = new ArrayList<Module>();

    // Filter the module list by whether we can actually deploy them to appengine.
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      AppEngineGradleFacet facet = AppEngineGradleFacet.getAppEngineFacetByModule(module);
      if (facet != null) {
        modules.add(module);
      }
    }

    // Tell the user what he has to do if he has none.
    if (modules.size() == 0) {
      //there are no modules to upload -- or we hit a bug due to gradle sync.
      //TODO do we need to use the mainwindow as owner?
      Messages.showErrorDialog(
        XmlStringUtil.wrapInHtml(
          "This project does not contain any App Engine modules. To add an App Engine module for your project, <br> open “File > New Module…” menu and choose one of App Engine modules.")
        , "Error");
      return;
    }

    if (selectedModule != null && !modules.contains(selectedModule)) {
      selectedModule = null;
    }

    if (selectedModule == null && modules.size() == 1) {
      selectedModule = modules.get(0);
    }

    // To invoke later, we need a final local.
    final Module passedSelectedModule = selectedModule;

    // Login on demand and queue up the dialog to show after a successful login.
    //if login fails, it already shows an error.
    if (!GoogleLogin.getInstance().isLoggedIn()) {
      // log in on demand...
      GoogleLogin.getInstance().logIn(null, new IGoogleLoginCompletedCallback() {
        @Override
        public void onLoginCompleted() {
          if (GoogleLogin.getInstance().isLoggedIn()) {
            EventQueue.invokeLater(new Runnable() {
              @Override
              public void run() {
                // Success!, lets run the deploy now.
                AppEngineUpdateDialog dialog = new AppEngineUpdateDialog(project, modules, passedSelectedModule);
                dialog.show();
              }
            });
          }
        }
      });
    }
    else {
      AppEngineUpdateDialog dialog = new AppEngineUpdateDialog(project, modules, passedSelectedModule);
      dialog.show();
    }
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    @SuppressWarnings("unchecked")
    final SortedComboBoxModel<Module> model = (SortedComboBoxModel<Module>)myModuleComboBox.getModel();
    model.clear();
    model.addAll(myDeployableModules);

    if (myInitiallySelectedModule != null) {
      // Auto select if there is only one item
      model.setSelectedItem(myInitiallySelectedModule);
      populateFields();
    }

    myModuleComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        populateFields();
      }
    });
    return myPanel;
  }

  private void populateFields() {
    myProjectId.setText("");
    myVersion.setText("");

    Module appEngineModule = myModuleComboBox.getSelectedModule();
    if (appEngineModule != null) {
      AppEngineGradleFacet facet = AppEngineGradleFacet.getAppEngineFacetByModule(appEngineModule);
      if (facet == null) {
        Messages.showErrorDialog(this.getPeer().getOwner(), "Could not acquire App Engine module information.", "Deploy");
        return;
      }

      final AppEngineWebApp appEngineWebApp = facet.getAppEngineWebXml();
      if (appEngineWebApp == null) {
        Messages.showErrorDialog(this.getPeer().getOwner(), "Could not locate or parse the appengine-web.xml fle.", "Deploy");
        return;
      }

      myProjectId.setText(appEngineWebApp.getApplication().getRawText());
      myVersion.setText(appEngineWebApp.getVersion().getRawText());
    }
  }

  @Override
  protected void doOKAction() {
    if (getOKAction().isEnabled()) {
      GoogleLogin login = GoogleLogin.getInstance();
      Module selectedModule = myModuleComboBox.getSelectedModule();
      String sdk = "";
      String war = "";
      AppEngineGradleFacet facet = AppEngineGradleFacet.getAppEngineFacetByModule(selectedModule);
      if (facet != null) {
        AppEngineConfigurationProperties model = facet.getConfiguration().getState();
        sdk = model.APPENGINE_SDKROOT;
        war = model.WAR_DIR;
      }

      String client_secret = login.fetchOAuth2ClientSecret();
      String client_id = login.fetchOAuth2ClientId();
      String refresh_token = login.fetchOAuth2RefreshToken();

      if (Strings.isNullOrEmpty(client_secret) ||
          Strings.isNullOrEmpty(client_id) ||
          Strings.isNullOrEmpty(refresh_token)) {
        // The login is somehow invalid, bail -- this shouldn't happen.
        LOG.error("StartUploading while logged in, but it doesn't have full credentials.");
        Messages.showErrorDialog(this.getPeer().getOwner(), "Login credentials are not valid.", "Login");
        return;
      }

      // These should not fail as they are a part of the dialog validation.
      if (Strings.isNullOrEmpty(sdk) ||
          Strings.isNullOrEmpty(war) ||
          Strings.isNullOrEmpty(myProjectId.getText()) ||
          selectedModule == null) {
        Messages.showErrorDialog(this.getPeer().getOwner(), "Could not deploy due to missing information (sdk/war/projectid).", "Deploy");
        LOG.error("StartUploading was called with bad module/sdk/war");
        return;
      }

      close(OK_EXIT_CODE);  // We close before kicking off the update so it doesn't interfere with the output window coming to focus.

      // Kick off the upload.  detailed status will be shown in an output window.
      new AppEngineUpdater(myProject, selectedModule, sdk, war, myProjectId.getText(), myVersion.getText(),
                           client_secret, client_id, refresh_token).startUploading();
    }
  }

  @Override
  protected ValidationInfo doValidate() {
    // These should not normally occur..
    if (!GoogleLogin.getInstance().isLoggedIn()) {
      return new ValidationInfo("You must be logged in to perform this action.");
    }

    Module module = myModuleComboBox.getSelectedModule();
    if (module == null) {
      return new ValidationInfo("Select a module");
    }

    AppEngineGradleFacet facet = AppEngineGradleFacet.getAppEngineFacetByModule(module);
    if (facet == null) {
      return new ValidationInfo("Could not find App Engine gradle configuration on Module");
    }

    // We'll let AppCfg error if the project is wrong.  The user can see this in the console window.
    // Note that version can be blank to indicate current version.
    if (Strings.isNullOrEmpty(myProjectId.getText())) {
      return new ValidationInfo("Please enter a Project ID.");
    }

    return null;
  }

}
