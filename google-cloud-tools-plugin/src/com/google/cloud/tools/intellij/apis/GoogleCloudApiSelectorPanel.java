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

package com.google.cloud.tools.intellij.apis;

import com.google.cloud.tools.libraries.CloudLibraries;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Comparator;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;

/** The form-bound class for the Cloud API selector panel. */
final class GoogleCloudApiSelectorPanel {

  private static final Logger logger = Logger.getInstance(GoogleCloudApiSelectorPanel.class);

  private JPanel panel;
  private JBScrollPane leftScrollPane;
  private JBScrollPane rightScrollPane;
  private JPanel checkboxPanel;
  private JBSplitter splitter;
  private GoogleCloudApiDetailsPanel detailsForm;

  /** Returns the {@link JPanel} that holds the UI elements in this panel. */
  public JPanel getPanel() {
    return panel;
  }

  /**
   * Initializes some UI components in this panel that require special set-up.
   *
   * <p>This is automatically called by the IDEA SDK and should not be directly invoked.
   */
  private void createUIComponents() {
    splitter = new JBSplitter();
    splitter.setFirstComponent(leftScrollPane);
    splitter.setSecondComponent(rightScrollPane);

    detailsForm = new GoogleCloudApiDetailsPanel();

    checkboxPanel = new JPanel();
    checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.PAGE_AXIS));
    try {
      CloudLibraries.getCloudLibraries()
          .stream()
          .sorted(Comparator.comparing(CloudLibrary::getName))
          .forEach(this::addLibraryCheckbox);
    } catch (IOException e) {
      logger.error(e);
    }
  }

  /**
   * Adds a new checkbox for the given {@link CloudLibrary} to the {@link #checkboxPanel}.
   *
   * @param library the {@link CloudLibrary} to add the checkbox for
   */
  private void addLibraryCheckbox(CloudLibrary library) {
    LibraryCheckboxPanel panel = new LibraryCheckboxPanel(library, detailsForm::setCloudLibrary);
    checkboxPanel.add(panel);
  }

  /** The panel that represents a checkbox for a {@link CloudLibrary}. */
  private static final class LibraryCheckboxPanel extends JPanel {

    private static final int BORDER_THICKNESS = 2;
    private static final Color FOCUS_BORDER_COLOR = UIManager.getColor("Button.focus");
    private static final Border FOCUSED_BORDER =
        BorderFactory.createLineBorder(FOCUS_BORDER_COLOR, BORDER_THICKNESS);
    private static final Border EMPTY_BORDER =
        BorderFactory.createEmptyBorder(
            BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS);

    private final CloudLibrary library;
    private final Consumer<CloudLibrary> focusHandler;

    /**
     * Returns a new instance for the given {@link CloudLibrary} and selected handler.
     *
     * @param library the {@link CloudLibrary} that this checkbox panel represents
     * @param focusHandler a {@link Consumer} that accepts the {@link CloudLibrary} when the panel
     *     is focused
     */
    LibraryCheckboxPanel(CloudLibrary library, Consumer<CloudLibrary> focusHandler) {
      super(new FlowLayout(FlowLayout.LEFT));
      this.library = library;
      this.focusHandler = focusHandler;
      init();
    }

    /** Initializes the UI for this panel. */
    void init() {
      setBorder(EMPTY_BORDER);
      addMouseListener(
          new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              e.getComponent().requestFocusInWindow();
            }
          });
      addFocusListener(
          new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
              ((JPanel) e.getComponent()).setBorder(FOCUSED_BORDER);
              focusHandler.accept(library);
            }

            @Override
            public void focusLost(FocusEvent e) {
              ((JPanel) e.getComponent()).setBorder(EMPTY_BORDER);
            }
          });

      JBCheckBox checkbox = new JBCheckBox();
      checkbox.setFocusable(false);
      checkbox.addActionListener(e -> checkbox.getParent().requestFocusInWindow());
      add(checkbox);
      add(new JLabel(library.getName()));
    }
  }
}
