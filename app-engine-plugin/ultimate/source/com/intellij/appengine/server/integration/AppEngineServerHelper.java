package com.intellij.appengine.server.integration;

import com.intellij.appengine.sdk.AppEngineSdk;
import com.intellij.javaee.appServerIntegrations.*;

/**
 * @author nik
 */
public class AppEngineServerHelper implements ApplicationServerHelper {
  public ApplicationServerInfo getApplicationServerInfo(ApplicationServerPersistentData persistentData)
      throws CantFindApplicationServerJarsException {
    final AppEngineSdk sdk = ((AppEngineServerData)persistentData).getSdk();
    String version = sdk.getVersion();
    return new ApplicationServerInfo(sdk.getLibraries(), "AppEngine Dev" + (version != null ? " " + version : ""));
  }

  public ApplicationServerPersistentData createPersistentDataEmptyInstance() {
    return new AppEngineServerData("");
  }

  public ApplicationServerPersistentDataEditor createConfigurable() {
    return new AppEngineServerEditor();
  }
}
