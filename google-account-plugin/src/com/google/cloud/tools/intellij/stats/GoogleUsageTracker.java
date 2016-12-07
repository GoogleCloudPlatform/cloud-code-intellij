/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

/** Google Usage Tracker that reports to Cloud Tools Analytics backend. */
public class GoogleUsageTracker implements UsageTracker, SendsEvents {

  private static final Logger logger = Logger.getInstance(GoogleUsageTracker.class);

  private static final String ANALYTICS_URL = "https://ssl.google-analytics.com/collect";
  private static final String PROTOCOL_VERSION_KEY = "v";
  private static final String UNIQUE_CLIENT_ID_KEY = "cid";
  private static final String IS_NON_INTERACTIVE_KEY = "ni";
  private static final String HIT_TYPE_KEY = "t";
  private static final String PAGE_VIEW_VALUE = "pageview";
  private static final String PROPERTY_ID_KEY = "tid";
  private static final String EVENT_TYPE_KEY = "cd19";
  private static final String EVENT_NAME_KEY = "cd20";
  private static final String IS_INTERNAL_USER_KEY = "cd16";
  private static final String IS_USER_SIGNED_IN_KEY = "cd17";
  private static final String PAGE_URL_KEY = "dp";
  private static final String IS_VIRTUAL_KEY = "cd21";
  private static final String PAGE_TITLE_KEY = "dt";
  private static final String STRING_FALSE_VALUE = "0";
  private static final String STRING_TRUE_VALUE = "1";
  private static final ImmutableList<BasicNameValuePair> ANALYTICS_BASE_DATA =
      ImmutableList.of(
          new BasicNameValuePair(PROTOCOL_VERSION_KEY, "1"),
          // Apparently the hit type should always be of type 'pageview'.
          new BasicNameValuePair(HIT_TYPE_KEY, PAGE_VIEW_VALUE),
          new BasicNameValuePair(IS_NON_INTERACTIVE_KEY, STRING_FALSE_VALUE),
          new BasicNameValuePair(
              UNIQUE_CLIENT_ID_KEY,
              UpdateChecker.getInstallationUID(PropertiesComponent.getInstance())));

  private final String analyticsId;
  private String externalPluginName;
  private String userAgent;

  /**
   * Constructs a usage tracker configured with analytics and plugin name configured from its
   * environment.
   */
  public GoogleUsageTracker() {
    analyticsId = UsageTrackerManager.getInstance().getAnalyticsProperty();

    AccountPluginInfoService pluginInfo = ServiceManager.getService(AccountPluginInfoService.class);
    externalPluginName = pluginInfo.getExternalPluginName();
    userAgent = pluginInfo.getUserAgent();
  }

  /** Send a (virtual) "pageview" ping to the Cloud-platform-wide Google Analytics Property. */
  public void sendEvent(
      @NotNull String eventCategory,
      @NotNull String eventAction,
      @Nullable String eventLabel,
      @Nullable Integer eventValue) {
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      if (UsageTrackerManager.getInstance().isTrackingEnabled()) {
        // For the semantics of each parameter, consult the followings:
        //
        // https://github.com/google/cloud-reporting/blob/master/src/main/java/com/google/cloud/metrics/MetricsUtils.java#L183
        // https://developers.google.com/analytics/devguides/collection/protocol/v1/reference

        List<BasicNameValuePair> postData = Lists.newArrayList(ANALYTICS_BASE_DATA);
        postData.add(new BasicNameValuePair(PROPERTY_ID_KEY, analyticsId));
        postData.add(new BasicNameValuePair(EVENT_TYPE_KEY, eventCategory));
        postData.add(new BasicNameValuePair(EVENT_NAME_KEY, eventAction));
        postData.add(new BasicNameValuePair(IS_INTERNAL_USER_KEY, STRING_FALSE_VALUE));
        postData.add(new BasicNameValuePair(IS_USER_SIGNED_IN_KEY, STRING_FALSE_VALUE));

        // Virtual page information
        String virtualPageUrl = "/virtual/" + eventCategory + "/" + eventAction;
        postData.add(new BasicNameValuePair(PAGE_URL_KEY, virtualPageUrl));
        // I think 'virtual' indicates these don't correspond to real web pages.
        postData.add(new BasicNameValuePair(IS_VIRTUAL_KEY, STRING_TRUE_VALUE));
        if (eventLabel != null) {
          // Event metadata are passed as a (virtual) page title.
          String virtualPageTitle = eventLabel + "=" + (eventValue != null ? eventValue : "null");
          postData.add(new BasicNameValuePair(PAGE_TITLE_KEY, virtualPageTitle));
        }

        sendPing(postData);
      }
    }
  }

  @Override
  public FluentTrackingEventWithLabel trackEvent(String action) {
    return new TrackingEventBuilder(this, externalPluginName, action);
  }

  private void sendPing(@NotNull final List<? extends NameValuePair> postData) {
    ApplicationManager.getApplication()
        .executeOnPooledThread(
            new Runnable() {
              public void run() {
                CloseableHttpClient client =
                    HttpClientBuilder.create().setUserAgent(userAgent).build();
                HttpPost request = new HttpPost(ANALYTICS_URL);

                try {
                  request.setEntity(new UrlEncodedFormEntity(postData));
                  CloseableHttpResponse response = client.execute(request);
                  StatusLine status = response.getStatusLine();
                  if (status.getStatusCode() >= 300) {
                    logger.debug(
                        "Non 200 status code : "
                            + status.getStatusCode()
                            + " - "
                            + status.getReasonPhrase());
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
