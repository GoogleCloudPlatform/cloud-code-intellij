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

package com.google.cloud.tools.intellij.appengine.java.ultimate.server.integration;

import com.google.cloud.tools.intellij.appengine.java.AppEngineIcons;
import com.intellij.javaee.appServerIntegrations.AppServerIntegration;
import com.intellij.javaee.appServerIntegrations.ApplicationServerHelper;
import com.intellij.javaee.appServerIntegrations.ApplicationServerPersistentDataEditor;
import com.intellij.javaee.openapi.ex.AppServerIntegrationsManager;
import javax.swing.Icon;

/** @author nik */
public class AppEngineServerIntegration extends AppServerIntegration {

  private final AppEngineServerHelper serverHelper;

  public static AppEngineServerIntegration getInstance() {
    return AppServerIntegrationsManager.getInstance()
        .getIntegration(AppEngineServerIntegration.class);
  }

  public AppEngineServerIntegration() {
    serverHelper = new AppEngineServerHelper();
  }

  @Override
  public Icon getIcon() {
    return AppEngineIcons.APP_ENGINE;
  }

  @Override
  public String getPresentableName() {
    return "Google App Engine Dev Server";
  }

  @Override
  public ApplicationServerPersistentDataEditor createNewServerEditor() {
    // Google App Engine server should not be shown in 'Application Server' combobox in the new
    // project wizard because there is a special 'Google App Engine' option
    return null;
  }

  @Override
  public ApplicationServerHelper getApplicationServerHelper() {
    return serverHelper;
  }
}
