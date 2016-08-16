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

import com.google.cloud.tools.intellij.appengine.util.AppEngineUtilLegacy;

import com.intellij.facet.Facet;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.ide.presentation.VirtualFilePresentation;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.JBColor;
import com.intellij.ui.RightAlignedLabelUI;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.util.PlatformIcons;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;

/**
 * @author nik
 */
public class AppEngineFacetEditor extends FacetEditorTab {

  private final AppEngineFacetConfiguration myFacetConfiguration;
  private final FacetEditorContext myContext;
  private JPanel myMainPanel;
  private JCheckBox myRunEnhancerOnMakeCheckBox;
  private JPanel myFilesToEnhancePanel;
  private JList myFilesList;
  private JComboBox myPersistenceApiComboBox;
  private JPanel myFilesPanel;
  private DefaultListModel myFilesListModel;

  public AppEngineFacetEditor(AppEngineFacetConfiguration facetConfiguration,
      FacetEditorContext context, FacetValidatorsManager validatorsManager) {
    myFacetConfiguration = facetConfiguration;
    myContext = context;

    myRunEnhancerOnMakeCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        GuiUtils.enableChildren(myRunEnhancerOnMakeCheckBox.isSelected(), myFilesToEnhancePanel);
        if (myRunEnhancerOnMakeCheckBox.isSelected() && myFilesListModel.isEmpty()) {
          fillFilesList(
              AppEngineUtilLegacy.getDefaultSourceRootsToEnhance(myContext.getRootModel()));
        }
      }
    });

    myFilesListModel = new DefaultListModel();
    myFilesList = new JBList(myFilesListModel);
    myFilesList.setCellRenderer(new FilesListCellRenderer());
    myFilesPanel.add(ToolbarDecorator.createDecorator(myFilesList)
        .setAddAction(new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton button) {
            doAdd();
          }
        }).disableUpDownActions().createPanel(), BorderLayout.CENTER);

    PersistenceApiComboboxUtil.setComboboxModel(myPersistenceApiComboBox, false);
  }

  private void doAdd() {
    final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, false, false,
        false, true);
    final ModuleRootModel rootModel = myContext.getRootModel();
    descriptor.setRoots(rootModel.getSourceRoots(JavaModuleSourceRootTypes.SOURCES));
    final VirtualFile[] files = FileChooser.chooseFiles(descriptor, myContext.getProject(), null);
    for (VirtualFile file : files) {
      myFilesListModel.addElement(file.getPath());
    }
  }

  @Nls
  public String getDisplayName() {
    return "Google App Engine";
  }

  @NotNull
  public JComponent createComponent() {
    return myMainPanel;
  }

  @Override
  public boolean isModified() {
    return myRunEnhancerOnMakeCheckBox.isSelected() != myFacetConfiguration.isRunEnhancerOnMake()
        || !getConfiguredFiles().equals(myFacetConfiguration.getFilesToEnhance())
        || PersistenceApiComboboxUtil.getSelectedApi(myPersistenceApiComboBox)
        != myFacetConfiguration.getPersistenceApi();
  }

  private List<String> getConfiguredFiles() {
    final List<String> files = new ArrayList<String>();
    for (int i = 0; i < myFilesListModel.getSize(); i++) {
      files.add((String) myFilesListModel.getElementAt(i));
    }
    return files;
  }

  @Override
  public void apply() {
    myFacetConfiguration.setRunEnhancerOnMake(myRunEnhancerOnMakeCheckBox.isSelected());
    myFacetConfiguration.setFilesToEnhance(getConfiguredFiles());
    myFacetConfiguration
        .setPersistenceApi(PersistenceApiComboboxUtil.getSelectedApi(myPersistenceApiComboBox));
  }

  @Override
  public void reset() {
    myFilesListModel.removeAllElements();
    fillFilesList(myFacetConfiguration.getFilesToEnhance());
    myRunEnhancerOnMakeCheckBox.setSelected(myFacetConfiguration.isRunEnhancerOnMake());
    myPersistenceApiComboBox
        .setSelectedItem(myFacetConfiguration.getPersistenceApi().getDisplayName());
  }

  private void fillFilesList(final List<String> paths) {
    for (String path : paths) {
      myFilesListModel.addElement(path);
    }
  }

  @SuppressWarnings("checkstyle:abbreviationaswordinname")
  public void disposeUIResources() {
  }

  @Override
  public String getHelpTopic() {
    return "Google_App_Engine_Facet";
  }

  @Override
  public void onFacetInitialized(@NotNull Facet facet) {
    AppEngineWebIntegration.getInstance().setupDevServer();
  }

  private class FilesListCellRenderer extends DefaultListCellRenderer {

    private FilesListCellRenderer() {
      setUI(new RightAlignedLabelUI());
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
        boolean isSelected, boolean cellHasFocus) {
      final Component rendererComponent = super
          .getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (value instanceof String) {
        final String path = (String) value;
        final VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
        if (file == null) {
          setForeground(JBColor.RED);
          setIcon(null);
        } else {
          setForeground(myFilesList.getForeground());
          setIcon(file.isDirectory() ? PlatformIcons.FOLDER_ICON
              : VirtualFilePresentation.getIcon(file));
        }
        setText(path);
      }
      return rendererComponent;
    }
  }
}
