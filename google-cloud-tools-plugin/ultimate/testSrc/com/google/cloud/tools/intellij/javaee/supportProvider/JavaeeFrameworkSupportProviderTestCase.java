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

package com.google.cloud.tools.intellij.javaee.supportProvider;

import com.intellij.ide.util.frameworkSupport.FrameworkSupportProviderTestCase;
import com.intellij.javaee.appServerIntegrations.ApplicationServer;
import com.intellij.javaee.module.components.FrameworkVirtualFileSystem;
import com.intellij.javaee.serverInstances.ApplicationServersManager;
import com.intellij.openapi.application.ApplicationManager;

import java.util.List;

//todo: this class is copied from javaee_tests module. We need to create a separate artifact from that module and use it instead.
public abstract class JavaeeFrameworkSupportProviderTestCase extends FrameworkSupportProviderTestCase {
  public static void deleteApplicationServers() {
    final ApplicationServersManager manager = ApplicationServersManager.getInstance();
    final List<ApplicationServer> servers = manager.getApplicationServers();
    final ApplicationServersManager.ApplicationServersManagerModifiableModel model = manager.createModifiableModel();
    for (ApplicationServer server : servers) {
      model.deleteApplicationServer(server);
    }
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        model.commit();
      }
    });
  }


  @Override
  protected void tearDown() throws Exception {
    FrameworkVirtualFileSystem.getJ2EEInstance().cleanup();
    deleteApplicationServers();
    super.tearDown();
  }
}
