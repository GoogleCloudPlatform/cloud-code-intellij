/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.google.gct.intellij.endpoints.action.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * A dialog when the user wants to generate an AppEngine endpoints backend informing them that an Android library module and AppEngine
 * module are going to be created. Also allows the user to enter in optional configuration information
 */
public class GenerateBackendDialog extends DialogWrapper {

  private Project myProject;
  private String myAppEngineSdkPath = null;
  protected String myApiKey = "";
  protected String myAppId = "";
  protected String myProjectNum = "";

  private JPanel myGenerateBackendPanel;
  private JTextField myApiKeyField;
  private JTextField myAppIdField;
  private JTextField myProjectNumField;
  private JEditorPane myDescriptionTextPane;

  public GenerateBackendDialog(Project project) {
    super(project, true);
    this.myProject = project;
    init();
    initValidation();
    setTitle("Generate App Engine Backend");
    setOKButtonText("Generate");
    setupDescriptionPanel();
  }

  public String getAppEngineSdkPath() {
    return myAppEngineSdkPath;
  }

  public String getApiKey() {
    return myApiKey;
  }

  public String getAppId() {
    return myAppId;
  }

  public String getProjectNumber() {
    return myProjectNum;
  }

  @Override
  protected ValidationInfo doValidate() {
    if (!myProjectNumField.getText().trim().matches("[0-9]*")) {
      return new ValidationInfo("Project Number must be numeric");
    }
    return null;
  }

  @Override
  protected void doOKAction() {
    // TODO : this might be redundant
    ValidationInfo validationInfo = doValidate();
    if (validationInfo != null) {
      Messages.showErrorDialog(myProjectNumField, "Cannot Generate Backend : ", validationInfo.message);
      return;
    }

    myAppId = myAppIdField.getText().trim();
    myProjectNum = myProjectNumField.getText().trim();
    myApiKey = myApiKeyField.getText().trim();

    super.doOKAction();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myGenerateBackendPanel;

  }

  private void setupDescriptionPanel() {
    myDescriptionTextPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    myDescriptionTextPane.setFont(UIManager.getFont("Label.font"));
    myDescriptionTextPane.setOpaque(false);
    myDescriptionTextPane.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
    myDescriptionTextPane.setText("<html>" +
                                "<body>" +
                                "When you generate an App Engine backend, sample code will be generated in " +
                                "both your Android project and your new App Engine project." +
                                "<br><br>" +
                                "You will be able to register devices with your backend and send notifications to " +
                                "them from a sample web application. The Android application has " +
                                "functionality that allows it to register and receive messages via Google " +
                                "Cloud Messaging." +
                                "<br><br>" +
                                "The App Engine backend provides an Endpoints API " +
                                "to register Android clients and a Web UI to broadcast messages to clients." +
                                "<br><br>" +
                                "These configuration parameters below are required for a working example. If " +
                                "entered now they will be injected into the project. If left blank, they " +
                                "can be manually entered into code after the project is generated." +
                                "<br><br>" +
                                "The Project ID is chosen when creating a Cloud Project. "  +
                                "You can create a new Cloud Project at the <a href='https://cloud.google.com/console'>Cloud Console</a>. " +
                                "Alternatively, you can enter an existing App Engine application ID in place of the Project ID." +
                                "<br><br>" +
                                "Your Project ID is listed in the upper-left hand corner of the Cloud Console. Click on the words " +
                                "'Project ID' to toggle the Project Number. To obtain your API key, click on the 'APIs' section, " +
                                "ensure that the 'Google Cloud Messaging' API is enabled, and then click on 'Register App' to obtain " +
                                "a Server key." +
                                "<br><br>" +
                                "More information about Google Cloud Messaging can " +
                                "be found <a href='http://developer.android.com/google/gcm/index.html'>here</a> " +
                                "and <a href='http://developer.android.com/google/gcm/gs.html'>here</a>" +
                                "</body>" +
                                "</html> ");

    myDescriptionTextPane.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          BrowserUtil.launchBrowser(e.getURL().toString());
        }
      }
    });

    // TODO : Panel acts weird if we don't use setsize + repaint, it draws components over each other
    // perhaps there is a better way to do this.
    myDescriptionTextPane.setSize(-1, -1);
    myDescriptionTextPane.repaint();
  }

}
