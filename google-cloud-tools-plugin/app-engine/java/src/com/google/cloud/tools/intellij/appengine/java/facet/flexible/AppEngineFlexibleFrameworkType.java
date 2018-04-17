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

package com.google.cloud.tools.intellij.appengine.java.facet.flexible;

import com.google.cloud.tools.intellij.appengine.java.AppEngineIcons;
import com.google.cloud.tools.intellij.appengine.java.AppEngineMessageBundle;
import com.intellij.framework.FrameworkTypeEx;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

/** The Flexible framework type. */
public class AppEngineFlexibleFrameworkType extends FrameworkTypeEx {

  public static final String ID = "appengine-flexible";

  public AppEngineFlexibleFrameworkType() {
    super(ID);
  }

  static AppEngineFlexibleFrameworkType getFrameworkType() {
    return EP_NAME.findExtension(AppEngineFlexibleFrameworkType.class);
  }

  @NotNull
  @Override
  public FrameworkSupportInModuleProvider createProvider() {
    return new AppEngineFlexibleSupportProvider();
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return AppEngineMessageBundle.getString("appengine.flexible.facet.name.title");
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AppEngineIcons.APP_ENGINE;
  }
}
