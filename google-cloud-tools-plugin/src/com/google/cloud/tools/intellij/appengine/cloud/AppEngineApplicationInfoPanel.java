/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import com.google.cloud.tools.intellij.resources.ProjectSelector.ProjectSelectionChangedEvent;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.HyperlinkLabel;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import git4idea.DialogManager;

/**
 * A {@link JPanel} that displays contextual information about an App Engine Application.
 */
public class AppEngineApplicationInfoPanel extends JPanel {

  private static final int COMPONENTS_HORIZONTAL_PADDING = 5;
  private static final int COMPONENTS_VERTICAL_PADDING = 0;

  private final JLabel errorIcon;
  private final HyperlinkLabel messageText;

  // Start in a friendly state before we know whether the application is truly valid.
  private boolean isApplicationValid = true;

  private CreateApplicationLinkListener currentLinkListener;

  public AppEngineApplicationInfoPanel() {
    super(new BorderLayout(COMPONENTS_HORIZONTAL_PADDING, COMPONENTS_VERTICAL_PADDING));

    errorIcon = new JLabel(AllIcons.Ide.Error);
    errorIcon.setVisible(false);
    messageText = new HyperlinkLabel();
    messageText.setOpaque(false);

    add(errorIcon, BorderLayout.WEST);
    add(messageText);
  }

  public void refresh(final ProjectSelectionChangedEvent event) {
    if (event == null) {
      setMessage(GctBundle.getString("appengine.infopanel.noproject"), true /* isError*/);
      return;
    }

    refresh(event.getSelectedProject().getProjectId(), event.getUser().getCredential());
  }

  /**
   * Updates the panel to display application info for the given project.
   *
   * @param projectId the ID of the project whose application info to display
   * @param credential the Credential to use to make any required API calls
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public void refresh(final String projectId, final Credential credential) {
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        Application application =
            AppEngineAdminService.getInstance().getApplicationForProjectId(projectId, credential);

        if (application != null) {
          setMessage(application.getLocationId(), false);
          isApplicationValid = true;
        } else {
          setCreateApplicationMessage(projectId, credential);
          isApplicationValid = false;
        }
      } catch (IOException | GoogleApiException e) {
        setMessage(GctBundle.message("appengine.application.region.fetch.error"), true);
        isApplicationValid = false;
      }
    });
  }

  /**
   * Returns {@code true} if the currently displayed Application is valid, {@code false} if
   * otherwise.
   */
  public boolean isApplicationValid() {
    return isApplicationValid;
  }

  private void setMessage(String text, boolean isError) {
    setMessage(text, "", "", isError);
  }

  private void setMessage(String beforeLinkText, String linkText, String afterLinkText,
      boolean isError) {
    ApplicationManager.getApplication().invokeAndWait(() -> {
      errorIcon.setVisible(isError);
      messageText.setHyperlinkText(beforeLinkText, linkText, afterLinkText);
    }, ModalityState.stateForComponent(this));
  }

  private void setCreateApplicationMessage(String projectId, Credential credential) {
    // dispose the old link listener and replace with a new instance that has the current args
    if (currentLinkListener != null) {
      // if the listener is not found, this is a no-op
      messageText.removeHyperlinkListener(currentLinkListener);
    }
    currentLinkListener = new CreateApplicationLinkListener(projectId, credential);
    messageText.addHyperlinkListener(currentLinkListener);

    setMessage(GctBundle.getString("appengine.application.not.exist") + " ",
        GctBundle.getString("appengine.application.create.linkText"),
        " " + GctBundle.getString("appengine.application.create.afterLinkText"),
        true);
  }

  /**
   * Implementation of {@link HyperlinkListener} that opens a
   * {@link AppEngineApplicationCreateDialog} when the link is clicked.
   */
  private class CreateApplicationLinkListener implements HyperlinkListener {

    private final Credential credential;
    private final String projectId;

    public CreateApplicationLinkListener(String projectId, Credential credential) {
      this.projectId = projectId;
      this.credential = credential;
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
          refresh(projectId, credential);
        }
      }
    }
  }
}
