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

package com.google.cloud.tools.intellij.appengine.application;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.appengine.v1.model.Application;
import com.google.api.services.appengine.v1.model.Location;
import com.google.cloud.tools.intellij.ui.BrowserOpeningHyperLinkListener;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.ui.JBColor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;

/**
 * Dialog that allows creation of an App Engine Application
 */
public class AppEngineApplicationCreateDialog extends DialogWrapper {

  private final static String LOCATIONS_DOCUMENTATION_URL
      = "https://cloud.google.com/docs/geography-and-regions";
  private final static String FLEX_DOCUMENTATION_URL
      = "https://cloud.google.com/appengine/docs/flexible/";
  private final static String STANDARD_DOCUMENTATION_URL
      = "https://cloud.google.com/appengine/docs/about-the-standard-environment";
  private static final String HTML_OPEN_TAG = "<html><font face='sans' size='-1'>";
  private static final String HTML_CLOSE_TAG = "</font></html>";

  private JPanel panel;
  private JTextPane instructionsTextPane;
  private JComboBox<AppEngineLocationSelectorItem> regionComboBox;
  private JTextPane statusPane;
  private JTextPane regionDetailPane;

  private final Component parent;
  private final Credential userCredential;
  private final String gcpProjectId;

  public AppEngineApplicationCreateDialog(@NotNull Component parent, @NotNull String gcpProjectId,
      @NotNull Credential userCredential) {
    super(parent, false);

    this.parent = parent;
    this.gcpProjectId = gcpProjectId;
    this.userCredential = userCredential;

    init();
    setTitle(GctBundle.message("appengine.application.region.select"));
    refreshLocationsSelector();

    regionComboBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          updateLocationDetailMessage();
        }
      }
    });

    regionDetailPane.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
    instructionsTextPane.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
    instructionsTextPane.setText(HTML_OPEN_TAG
        + GctBundle.message("appengine.application.create.instructions") + "<p>"
        + GctBundle.message("appengine.application.create.documentation",
        "<a href=\"" + LOCATIONS_DOCUMENTATION_URL + "\">", "</a>") + "</p>" + HTML_CLOSE_TAG);
  }

  @Override
  protected void doOKAction() {
    final Location selectedLocation
        = ((AppEngineLocationSelectorItem) regionComboBox.getSelectedItem()).getLocation();

    // show loading state
    setOKActionEnabled(false);

    try {
      // attempt to create the application, and close the dialog if successful
      ProgressManager.getInstance().runProcessWithProgressSynchronously(
          new ThrowableComputable<Application, Exception>() {
        @Override
        public Application compute() throws IOException, GoogleApiException {
          return AppEngineAdminService.getInstance().createApplication(
              selectedLocation.getLocationId(), gcpProjectId, userCredential);
        }
      }, "Creating App Engine Application. This can take up to a minute.", true /* cancellable */,
          ProjectManager.getInstance().getDefaultProject());

      close(OK_EXIT_CODE);

    } catch (IOException e) {
      setStatusMessage(GctBundle.message("appengine.application.create.error.transient"), true);
      setOKActionEnabled(true);
      return;
    } catch (GoogleApiException e) {
      setStatusMessage(e.getMessage(), true);
      setOKActionEnabled(true);
      return;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void setStatusMessageAsync(final String message, final boolean isError) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        setStatusMessage(message, isError);
      }
    }, ModalityState.stateForComponent(AppEngineApplicationCreateDialog.this.getContentPane()));
  }

  private void setStatusMessage(String message, boolean isError) {
    statusPane.setText(message);
    statusPane.setForeground(isError ? JBColor.red : JBColor.black);
    statusPane.setVisible(true);
  }

  private void refreshLocationsSelector() {
    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        final List<Location> appEngineRegions;
        try {
          appEngineRegions = AppEngineAdminService.getInstance()
              .getAllAppEngineLocations(userCredential);
        } catch (IOException | GoogleApiException e) {
          setStatusMessageAsync(GctBundle.message("appengine.application.region.list.fetch.error"),
              true);
          return;
        }

        // perform the actual UI updates on the event dispatch thread
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            regionComboBox.removeAllItems();
            for (Location location : appEngineRegions) {
              regionComboBox.addItem(new AppEngineLocationSelectorItem(location));
            }
          }
        }, ModalityState.stateForComponent(AppEngineApplicationCreateDialog.this.getContentPane()));
      }
    });
  }

  private void updateLocationDetailMessage() {
    AppEngineLocationSelectorItem item
        = (AppEngineLocationSelectorItem) regionComboBox.getSelectedItem();

    String displayText = HTML_OPEN_TAG +
        GctBundle.message("appengine.application.region.supported.environments",
            "<strong>" + item.getLocation().getLocationId() + "</strong>") + "<ul>";

    if (item.isStandardSupported()) {
      displayText += "<li>" + GctBundle.message("appengine.application.region.supports.standard",
          "<a href=\"" + STANDARD_DOCUMENTATION_URL + "\">", "</a>")
          + "</li>";
    }
    if (item.isFlexSupported()) {
      displayText += "<li>" + GctBundle.message("appengine.application.region.supports.flex",
          "<a href=\"" + FLEX_DOCUMENTATION_URL + "\">", "</a>")
          + "</li>";
    }

    displayText += "</ul>" + HTML_CLOSE_TAG;
    regionDetailPane.setText(displayText);
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel;
  }

}
