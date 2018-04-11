/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.java.sdk;

import static org.mockito.Mockito.verify;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.managedcloudsdk.ProgressListener;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests for {@link ManagedCloudSdkProgressListener} */
public class ManagedCloudSdkProgressListenerTest {

  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock private BackgroundableProcessIndicator mockProgressIndicator;

  @Mock private ManagedCloudSdkService mockCloudSdkService;

  private ManagedCloudSdkProgressListener progressListener;

  @Before
  public void setUp() {
    progressListener =
        new ManagedCloudSdkProgressListener(mockCloudSdkService) {
          @Override
          BackgroundableProcessIndicator initProgressIndicator() {
            return mockProgressIndicator;
          }
        };
  }

  @Test
  public void update_sets_valid_mainProgressFractions() {
    String message = "start";
    progressListener.start(message, 10);
    progressListener.update(5);

    verify(mockProgressIndicator)
        .setText(GctBundle.message("managedsdk.progress.message", message));
    verify(mockProgressIndicator).setFraction(0.5);
  }

  @Test
  public void update_withUnknown_setsProgressIndeterminate() {
    String message = "working";
    progressListener.start(message, ProgressListener.UNKNOWN);

    verify(mockProgressIndicator)
        .setText(GctBundle.message("managedsdk.progress.message", message));
    verify(mockProgressIndicator).setIndeterminate(true);
  }

  @Test
  public void child_update_setsProportionedFractions() {
    progressListener.start("main", 100);
    ProgressListener childListener = progressListener.newChild(20);
    childListener.start("child", 10);
    childListener.update(5);

    verify(mockProgressIndicator)
        .setText(GctBundle.message("managedsdk.progress.message", "child"));
    verify(mockProgressIndicator).setFraction(5d / 10d * 20d / 100d);
  }
}
