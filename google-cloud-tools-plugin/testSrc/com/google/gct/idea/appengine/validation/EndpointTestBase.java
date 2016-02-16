/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.google.gct.idea.appengine.validation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;

import java.io.File;

public abstract class EndpointTestBase extends JavaCodeInsightFixtureTestCase {
  @Override
  protected String getTestDataPath() {
    File currentWorkingDirectory = new File("");
    String homePathParent = currentWorkingDirectory.getAbsolutePath();
    return homePathParent + FileUtil.toSystemDependentName("/testData/");
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    addEndpointSdkToProject();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      removeEndpointsSdkFromProject();
    }
    finally {
      super.tearDown();
    }
  }


  /**
   * Adds the App Engine - Endpoint SDK to the test project's library
   */
  private void addEndpointSdkToProject() {
    LocalFileSystem fs = LocalFileSystem.getInstance();
    final VirtualFile libDir = fs.findFileByPath(getTestDataPath());

    if (libDir != null) {
      final VirtualFile pluginsDir = libDir.findChild("lib");
      if (pluginsDir != null) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          @Override
          public void run() {
            final LibraryTable table = LibraryTablesRegistrar.getInstance()
                .getLibraryTableByLevel(LibraryTablesRegistrar.APPLICATION_LEVEL, myModule.getProject());
            assert table != null;
            final LibraryTable.ModifiableModel tableModel = table.getModifiableModel();
            final Library library = tableModel.createLibrary("endpoints-lib");
            final Library.ModifiableModel libraryModel = library.getModifiableModel();
            libraryModel.addJarDirectory(pluginsDir, true);
            libraryModel.commit();
            tableModel.commit();

            ModifiableRootModel rootModel = ModuleRootManager.getInstance(myModule).getModifiableModel();
            Library jar = table.getLibraries()[0];
            rootModel.addLibraryEntry(jar); // Endpoint is the only jar added
            rootModel.commit();
          }
        });
      }
    }
  }

  private void removeEndpointsSdkFromProject() {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        LibraryTable table = LibraryTablesRegistrar.getInstance()
            .getLibraryTableByLevel(LibraryTablesRegistrar.APPLICATION_LEVEL,
                myModule.getProject());
        if (table != null) {
          LibraryTable.ModifiableModel tableModel = table.getModifiableModel();
          Library library = tableModel.getLibraryByName("endpoints-lib");
          if (library != null) {
            tableModel.removeLibrary(library);
            tableModel.commit();
          }
        }
      }
    });
  }

}
