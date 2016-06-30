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

package com.intellij.appengine.gwt;

import com.intellij.appengine.server.integration.AppEngineServerIntegration;
import com.intellij.gwt.run.GwtDevModeServer;
import com.intellij.gwt.run.GwtDevModeServerProvider;
import com.intellij.javaee.appServerIntegrations.ApplicationServer;
import com.intellij.javaee.serverInstances.ApplicationServersManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class AppEngineGwtServerProvider extends GwtDevModeServerProvider {
  @Override
  public List<? extends GwtDevModeServer> getServers() {
    final List<ApplicationServer> servers = ApplicationServersManager.getInstance().getApplicationServers(AppEngineServerIntegration.getInstance());
    final List<GwtDevModeServer> result = new ArrayList<GwtDevModeServer>();
    for (ApplicationServer server : servers) {
      result.add(new AppEngineGwtServer(server));
    }
    return result;
  }
}
