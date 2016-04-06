/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.elysium;

import com.google.cloud.tools.intellij.CloudToolsPluginInfoService;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;
import com.google.cloud.tools.intellij.login.CredentialedUser;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.awt.Image;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

@RunWith(MockitoJUnitRunner.class)
public class GoogleUserModelItemTest extends BasePluginTestCase {

  @Mock private CredentialedUser user;
  @Mock private Image image;
  @Mock private CloudToolsPluginInfoService mockPluginInfoService;

  private DefaultTreeModel model;

  @Before
  public void setUp() {
    Mockito.when(user.getEmail()).thenReturn("foo@example.com");
    Mockito.when(user.getName()).thenReturn("Jane Smith");
    Mockito.when(user.getPicture()).thenReturn(image);

    TreeNode root = new DefaultMutableTreeNode();
    model = new DefaultTreeModel(root);
  }

  @Test
  public void testGetters() {
    registerService(CloudToolsPluginInfoService.class, mockPluginInfoService);
    GoogleUserModelItem item = new GoogleUserModelItem(user, model);
    Assert.assertEquals(user, item.getCredentialedUser());
    Assert.assertEquals("foo@example.com", item.getEmail());
    Assert.assertEquals("Jane Smith", item.getName());
    Assert.assertEquals(image, item.getImage());
    Assert.assertFalse(item.isSynchronizing());
  }

}
