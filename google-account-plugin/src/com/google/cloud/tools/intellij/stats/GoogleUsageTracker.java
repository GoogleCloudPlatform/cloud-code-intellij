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

package com.google.cloud.tools.intellij.stats;

import com.google.cloud.tools.intellij.AccountPluginInfoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.updateSettings.impl.UpdateChecker;

import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Google Usage Tracker that reports to Cloud Tools Analytics backend.
 */
public class GoogleUsageTracker implements UsageTracker {

  private static final Logger logger = Logger.getInstance(GoogleUsageTracker.class);

  private static final String ANALYTICS_URL = "https://ssl.google-analytics.com/collect";

  private final String analyticsId;
  private String externalPluginName;

  public GoogleUsageTracker() {
    analyticsId = UsageTrackerManager.getInstance().getAnalyticsProperty();
    externalPluginName = ServiceManager.getService(AccountPluginInfoService.class)
        .getExternalPluginName();
  }

  private static final List<BasicNameValuePair> analyticsBaseData = ImmutableList
      .of(new BasicNameValuePair("v", "1"),         // Protocol version
          new BasicNameValuePair("t", "pageview"),  // Note the "pageview" type, not "event"
          new BasicNameValuePair("ni", "0"),        // Non-interactive? Report as interactive.
          new BasicNameValuePair("cid",             // UUID for this IntelliJ client
              UpdateChecker.getInstallationUID(PropertiesComponent.getInstance())));

  /**
   * Send a (virtual) "pageview" ping to the Cloud-platform-wide Google Analytics Property.
   */
  @Override
  public void trackEvent(@NotNull String eventCategory,
      @NotNull String eventAction,
      @Nullable String eventLabel,
      @Nullable Integer eventValue) {
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      if (UsageTrackerManager.getInstance().isTrackingEnabled()) {
        // For the semantics of each parameter, consult the followings:
        //
        // https://github.com/google/cloud-reporting/blob/master/src/main/java/com/google/cloud/metrics/MetricsUtils.java#L183
        // https://developers.google.com/analytics/devguides/collection/protocol/v1/reference

        List<BasicNameValuePair> postData = Lists.newArrayList(analyticsBaseData);
        postData.add(new BasicNameValuePair("tid", analyticsId));
        postData.add(new BasicNameValuePair("cd19", externalPluginName));  // Event type
        postData.add(new BasicNameValuePair("cd20", eventAction));  // Event name
        postData.add(new BasicNameValuePair("cd16", "0"));  // Internal user? No.
        postData.add(new BasicNameValuePair("cd17", "0"));  // User signed in? We will ignore this.

        // Virtual page information
        String virtualPageUrl = "/virtual/" + externalPluginName + "/" + eventAction;
        postData.add(new BasicNameValuePair("dp", virtualPageUrl));
        postData.add(new BasicNameValuePair("cd21", "1"));  // Yes, this ping is a virtual "page".
        if (eventLabel != null) {
          // Event metadata are passed as a (virtual) page title.
          String virtualPageTitle = eventLabel + "=" + (eventValue != null ? eventValue : "null");
          postData.add(new BasicNameValuePair("dt", virtualPageTitle));
        }

        sendPing(postData);
      }
    }
  }

  private static void sendPing(@NotNull final List<? extends NameValuePair> postData) {
    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      public void run() {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(ANALYTICS_URL);

        try {
          request.setEntity(new UrlEncodedFormEntity(postData));
          CloseableHttpResponse response = client.execute(request);
          StatusLine status = response.getStatusLine();
          if (status.getStatusCode() >= 300) {
            logger.debug("Non 200 status code : " + status.getStatusCode() + " - " + status
                .getReasonPhrase());
          }
        } catch (IOException ex) {
          logger.debug("IOException during Analytics Ping", ex.getMessage());
        } finally {
          HttpClientUtils.closeQuietly(client);
        }

      }
    });
  }

}
