/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.google.gct.idea.settings;

import com.google.common.annotations.VisibleForTesting;
import com.google.gct.idea.util.GctBundle;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.ImportSettingsFilenameFilter;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.components.ExportableApplicationComponent;
import com.intellij.openapi.components.ExportableComponent;
import com.intellij.openapi.components.ServiceBean;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.updateSettings.impl.UpdateSettings;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.io.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Provides functionality to update IDEA setting from a jar containing new settings
 */
public class ImportSettings {
  private static final String DIALOG_TITLE = "Setting Synchronization";
  public static final String SETTINGS_JAR_MARKER = "IntelliJ IDEA Global Settings";

  /**
   * Parse and update the IDEA settings in the jar at <code>path</code>.
   * Note: This function might require a restart of the application.
   * @param path The location of the jar with the new IDEA settings.
   */
  public static void doImport(String path) {
    final File saveFile = new File(path);
    ZipFile saveZipFile = null;
    try {
      if (!saveFile.exists()) {
        Messages.showErrorDialog(IdeBundle.message("error.cannot.find.file", presentableFileName(saveFile)), DIALOG_TITLE);
        return;
      }

      // What is this file used for?
      saveZipFile = new ZipFile(saveFile);
      final ZipEntry magicEntry = saveZipFile.getEntry(SETTINGS_JAR_MARKER);
      if (magicEntry == null) {
        Messages.showErrorDialog("The file " + presentableFileName(saveFile) + " contains no settings to import",
          DIALOG_TITLE);
        return;
      }

      final ArrayList<ExportableComponent> registeredComponents = new ArrayList<ExportableComponent>(
        Arrays.asList(ApplicationManager.getApplication().getComponents(ExportableApplicationComponent.class)));
      registeredComponents.addAll(ServiceBean.loadServicesFromBeans(ExportableComponent.EXTENSION_POINT, ExportableComponent.class));

      List<ExportableComponent> storedComponents = getComponentsStored(saveFile, registeredComponents);

      Set<String> relativeNamesToExtract = new HashSet<String>();
      for (final ExportableComponent aComponent : storedComponents) {
        final File[] exportFiles = aComponent.getExportFiles();
        for (File exportFile : exportFiles) {
          final File configPath = new File(PathManager.getConfigPath());
          final String rPath = FileUtil.getRelativePath(configPath, exportFile);
          assert rPath != null;
          final String relativePath = FileUtil.toSystemIndependentName(rPath);
          relativeNamesToExtract.add(relativePath);
        }
      }

      relativeNamesToExtract.add(PluginManager.INSTALLED_TXT);

      final File tempFile = new File(PathManager.getPluginTempPath() + "/" + saveFile.getName());
      FileUtil.copy(saveFile, tempFile);
      File outDir = new File(PathManager.getConfigPath());
      final ImportSettingsFilenameFilter filenameFilter = new ImportSettingsFilenameFilter(relativeNamesToExtract);
      StartupActionScriptManager.ActionCommand unzip = new StartupActionScriptManager.UnzipCommand(tempFile, outDir, filenameFilter);
      StartupActionScriptManager.addActionCommand(unzip);

      // remove temp file
      StartupActionScriptManager.ActionCommand deleteTemp = new StartupActionScriptManager.DeleteCommand(tempFile);
      StartupActionScriptManager.addActionCommand(deleteTemp);

      UpdateSettings.getInstance().forceCheckForUpdateAfterRestart();

      String key = ApplicationManager.getApplication().isRestartCapable()
                   ? "message.settings.imported.successfully.restart"
                   : "message.settings.imported.successfully";
      final int ret = Messages.showOkCancelDialog(IdeBundle.message(key,
                                                                    ApplicationNamesInfo.getInstance().getProductName(),
                                                                    ApplicationNamesInfo.getInstance().getFullProductName()),
                                                  IdeBundle.message("title.restart.needed"), Messages.getQuestionIcon());
      if (ret == Messages.OK) {
        ((ApplicationEx)ApplicationManager.getApplication()).restart(true);
      }
    }
    catch (ZipException e1) {
      Messages.showErrorDialog(
        "Error reading file " + presentableFileName(saveFile) + ".\\nThere was " + e1.getMessage(),
        DIALOG_TITLE);
    }
    catch (IOException e1) {
      Messages.showErrorDialog(IdeBundle.message("error.reading.settings.file.2", presentableFileName(saveFile), e1.getMessage()),
                               DIALOG_TITLE);
    } finally {
      try {
        if (saveZipFile != null) {
          saveZipFile.close();
        }
      } catch (IOException e1) {
        Messages.showErrorDialog(
          GctBundle.message("settings.error.closing.file", presentableFileName(saveFile), e1.getMessage()),
                            DIALOG_TITLE);
      }
    }
  }

  private static String presentableFileName(final File file) {
    return "'" + FileUtil.toSystemDependentName(file.getPath()) + "'";
  }

  private static List<ExportableComponent> getComponentsStored(File zipFile,
    ArrayList<ExportableComponent> registeredComponents)
    throws IOException {
    final File configPath = new File(PathManager.getConfigPath());

    final ArrayList<ExportableComponent> components = new ArrayList<ExportableComponent>();
    for (ExportableComponent component : registeredComponents) {
      final File[] exportFiles = component.getExportFiles();
      for (File exportFile : exportFiles) {
        final String rPath = FileUtil.getRelativePath(configPath, exportFile);
        assert rPath != null;
        String relativePath = FileUtil.toSystemIndependentName(rPath);
        if (exportFile.isDirectory()) relativePath += "/";
        if (ZipUtil.isZipContainsEntry(zipFile, relativePath)) {
          components.add(component);
          break;
        }
      }
    }
    return components;
  }
}
