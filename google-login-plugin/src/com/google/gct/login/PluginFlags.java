package com.google.gct.login;

import com.google.api.client.repackaged.javax.annotation.Nullable;

/**
 * Login Plugin configuration flags.
 */
public interface PluginFlags {

  @Nullable
  String getAnalyticsId();
}
