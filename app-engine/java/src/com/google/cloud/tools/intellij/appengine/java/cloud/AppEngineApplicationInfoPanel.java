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

package com.google.cloud.tools.intellij.appengine.java.cloud;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.appengine.v1.model.Application;
import com.google.cloud.tools.intellij.appengine.java.AppEngineMessageBundle;
import com.google.cloud.tools.intellij.appengine.java.application.AppEngineAdminService;
import com.google.cloud.tools.intellij.appengine.java.application.AppEngineApplicationCreateDialog;
import com.google.cloud.tools.intellij.appengine.java.application.GoogleApiException;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.HyperlinkLabel;
import git4idea.DialogManager;
import java.awt.BorderLayout;
import java.io.IOException;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

/** A {@link JPanel} that displays contextual information about an App Engine Application. */
public class AppEngineApplicationInfoPanel extends JPanel {

  private static final int COMPONENTS_HORIZONTAL_PADDING = 5;
  private static final int COMPONENTS_VERTICAL_PADDING = 0;

  private final JLabel errorIcon;
  private HyperlinkLabel messageText;

  public AppEngineApplicationInfoPanel() {
    super(new BorderLayout(COMPONENTS_HORIZONTAL_PADDING, COMPONENTS_VERTICAL_PADDING));

    errorIcon = new JLabel(AllIcons.Ide.Error);
    errorIcon.setVisible(false);
    messageText = new HyperlinkLabel();
    messageText.setOpaque(false);

    add(errorIcon, BorderLayout.WEST);
    add(messageText);
  }

  /**
   * Updates the panel as follows: if {@code projectId} is valid, it displays the given project's
   * information, if {@code projectId} is invalid, it displays an error message, if {@code
   * projectId} is empty, no message is displayed.
   *
   * @param projectId the ID of the project whose application info to display
   * @param credential the Credential to use to make any required API calls
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public void refresh(final String projectId, final Credential credential) {
    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              if (projectId.isEmpty()) {
                clearMessage();
                return;
              }

              if (projectId == null || credential == null) {
                setMessage(
                    AppEngineMessageBundle.getString("appengine.infopanel.no.region"),
                    true /* isError*/);
                return;
              }

              try {
                Application application =
                    AppEngineAdminService.getInstance()
                        .getApplicationForProjectId(projectId, credential);

                if (application != null) {
                  setMessage(application.getLocationId(), false);
                } else {
                  setCreateApplicationMessage(projectId, credential);
                }
              } catch (IOException | GoogleApiException e) {
                setMessage(
                    AppEngineMessageBundle.message("appengine.application.region.fetch.error"),
                    true);
              }
            });
  }

  /** Prints a message that doesn't contain a hyperlink. */
  public void setMessage(String text, boolean isError) {
    setMessage(
        () -> {
          messageText.setText(text);
          // HyperlinkLabels require that revalidate() be called after setText(), in order for text
          // to
          // actually show up. setHyperlinkText() calls revalidate() internally.
          messageText.revalidate();
        },
        isError);
  }

  void clearMessage() {
    setMessage("", false /* isError*/);
  }

  private void setMessage(Runnable messagePrinter, boolean isError) {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              errorIcon.setVisible(isError);

              // TODO(nkibler): Figure out what's causing the HyperlinkLabel to not refresh its view
              // if we re-use the label here.
              remove(messageText);
              messageText = new HyperlinkLabel();
              messageText.setOpaque(false);
              add(messageText);

              messagePrinter.run();
            },
            ModalityState.stateForComponent(this));
  }

  private void setCreateApplicationMessage(String projectId, Credential credential) {
    String beforeLinkText =
        AppEngineMessageBundle.getString("appengine.application.not.exist") + " ";
    String linkText = AppEngineMessageBundle.getString("appengine.application.create.linkText");
    String afterLinkText =
        " " + AppEngineMessageBundle.getString("appengine.application.create.afterLinkText");
    setMessage(
        () -> {
          messageText.addHyperlinkListener(
              new CreateApplicationLinkListener(projectId, credential));
          messageText.setHyperlinkText(beforeLinkText, linkText, afterLinkText);
        },
        true);
  }

  /**
   * Implementation of {@link HyperlinkListener} that opens a {@link
   * AppEngineApplicationCreateDialog} when the link is clicked.
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
        AppEngineApplicationCreateDialog applicationDialog =
            new AppEngineApplicationCreateDialog(
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
