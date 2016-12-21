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

package com.google.cloud.tools.intellij.appengine.cloud;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.appengine.v1.model.Application;
import com.google.cloud.tools.intellij.appengine.application.AppEngineAdminService;
import com.google.cloud.tools.intellij.appengine.application.AppEngineApplicationCreateDialog;
import com.google.cloud.tools.intellij.appengine.application.GoogleApiException;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;

import java.awt.FlowLayout;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import git4idea.DialogManager;

public class AppEngineApplicationInfoPanel extends JPanel {

  private static final String HTML_OPEN_TAG = "<html><font face='sans' size='-1'>";
  private static final String HTML_CLOSE_TAG = "</font></html>";
  private static final String CREATE_APPLICATION_HREF_OPEN_TAG = "<a href='#'>";
  private static final String HREF_CLOSE_TAG = "</a>";

  private CreateApplicationLinkListener  createApplicationLinkListener;
  private JLabel errorIcon;
  private JTextPane messageText;

  public AppEngineApplicationInfoPanel() {
    super(new FlowLayout(FlowLayout.LEFT));

    createApplicationLinkListener = new CreateApplicationLinkListener();
    errorIcon = new JLabel(AllIcons.Ide.Error);
    errorIcon.setVisible(false);
    messageText = new JTextPane();
    messageText.setContentType("text/html");
    messageText.setEditable(false);
    messageText.addHyperlinkListener(createApplicationLinkListener);

    add(errorIcon);
    add(messageText);
  }

  /**
   * Updates the panel to display application info for the given project.
   */
  public void displayInfoForProject(final String projectId, final Credential credential) {
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        Application application =
            AppEngineAdminService.getInstance().getApplicationForProjectId(projectId, credential);

        if (application != null) {
          setMessage(application.getLocationId(), false);
        } else {
          setCreateApplicationMessage(projectId, credential);
        }
      } catch (IOException | GoogleApiException e) {
        setMessage(GctBundle.message("appengine.application.region.fetch.error"), true);
      }
    });
  }

  private void setMessage(String message, boolean isError) {
    ApplicationManager.getApplication().invokeLater(() -> {
      errorIcon.setVisible(false);
      messageText.setText(HTML_OPEN_TAG + message + HTML_CLOSE_TAG);
      messageText.setForeground(isError ? JBColor.red : JBColor.black);

    }, ModalityState.stateForComponent(this));
  }

  private void setCreateApplicationMessage(String projectId, Credential credential) {
    // TODO is this safe? do we need to set these differenrlt
    createApplicationLinkListener.setCredential(credential);
    createApplicationLinkListener.setProjectId(projectId);

    // TODO invalidate? repaint?
    ApplicationManager.getApplication().invokeLater(() -> {
      String message = GctBundle.message("appengine.application.not.exist") + " "
          + GctBundle.message("appengine.application.create",
          CREATE_APPLICATION_HREF_OPEN_TAG, HREF_CLOSE_TAG);

      messageText.setText(HTML_OPEN_TAG + message + HTML_CLOSE_TAG);
      messageText.setForeground(JBColor.red);
      errorIcon.setVisible(true);
    }, ModalityState.stateForComponent(this));
  }

  private class CreateApplicationLinkListener implements HyperlinkListener {

    private Credential credential;
    private String projectId;

    public void setCredential(Credential credential) {
      this.credential = credential;
    }

    public void setProjectId(String projectId) {
      this.projectId = projectId;
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == EventType.ACTIVATED) {
        // construct and show the application creation dialog
        AppEngineApplicationCreateDialog applicationDialog = new AppEngineApplicationCreateDialog(
            AppEngineApplicationInfoPanel.this, projectId, credential);
        DialogManager.show(applicationDialog);
        applicationDialog.getDisposable().dispose();

        // if an application was created, update the region field display
        if (applicationDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
          displayInfoForProject(projectId, credential);
        }
      }
    }
  }
}
