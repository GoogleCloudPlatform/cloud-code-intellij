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

package com.google.cloud.tools.intellij.cloudapis;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.extensions.DefaultPluginDescriptor;
import com.intellij.openapi.extensions.Extensions;
import org.jdom.Element;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests action decoration for {@link AddCloudLibrariesAction}. */
public class AddCloudLibrariesActionTest {

  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  private AddCloudLibrariesAction addCloudLibrariesAction = new AddCloudLibrariesAction();
  @Mock private AnActionEvent mockActionEvent;

  @Before
  public void setUp() {
    TestDecoratorOne.decoratorCalled = false;
    TestDecoratorTwo.decoratorCalled = false;
  }

  @Test
  public void decorator_returnsTrue_noOtherDecorators_called() {
    // register two implementations for decorator extension point.
    // TODO(ivanporty) possibly make this part of CloudToolsRule using @ExtensionPoint signature.
    // TODO(ivanporty) figure out if it's possible to pass existing mocks instead of class names.
    Element extensionElement1 = new Element("cloudApiActionDecorator");
    extensionElement1.setAttribute("implementation", TestDecoratorOne.class.getName());
    Extensions.getArea(null)
        .registerExtension(
            new DefaultPluginDescriptor("com.gct.core"),
            extensionElement1,
            "com.google.gct.cloudapis");
    Element extensionElement2 = new Element("cloudApiActionDecorator");
    extensionElement2.setAttribute("implementation", TestDecoratorTwo.class.getName());
    Extensions.getArea(null)
        .registerExtension(
            new DefaultPluginDescriptor("com.gct.core"),
            extensionElement2,
            "com.google.gct.cloudapis");

    addCloudLibrariesAction.update(mockActionEvent);

    // make sure decorator that decorated an action stops other decoration.
    assertThat(TestDecoratorOne.decoratorCalled).isTrue();
    assertThat(TestDecoratorTwo.decoratorCalled).isFalse();
  }

  public static class TestDecoratorOne implements CloudApiActionDecoratorExtension {
    static boolean decoratorCalled;

    @Override
    public boolean decorate(AnActionEvent e) {
      decoratorCalled = true;
      return true;
    }
  }

  public static class TestDecoratorTwo implements CloudApiActionDecoratorExtension {
    static boolean decoratorCalled;

    @Override
    public boolean decorate(AnActionEvent e) {
      decoratorCalled = true;
      return false;
    }
  }
}
