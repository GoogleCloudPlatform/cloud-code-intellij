/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.google.cloud.tools.intellij.appengine.java.facet.standard;

import com.google.cloud.tools.intellij.appengine.java.AppEngineIcons;
import com.intellij.framework.FrameworkTypeEx;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.util.frameworkSupport.FrameworkRole;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

/** @author nik */
public class AppEngineStandardFrameworkType extends FrameworkTypeEx {
  public static final String ID = "appengine-java";

  public AppEngineStandardFrameworkType() {
    super(ID);
  }

  static AppEngineStandardFrameworkType getFrameworkType() {
    return EP_NAME.findExtension(AppEngineStandardFrameworkType.class);
  }

  @NotNull
  @Override
  public FrameworkSupportInModuleProvider createProvider() {
    return new AppEngineStandardSupportProvider();
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return "Google App Engine Standard";
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AppEngineIcons.APP_ENGINE;
  }

  @Override
  public FrameworkRole[] getRoles() {
    // Determines which ProjectCategory (the sections in the project creation left nav) this
    // framework appears in
    return AppEngineStandardWebIntegration.getInstance().getFrameworkRoles();
  }

  @Override
  public String getUnderlyingFrameworkTypeId() {
    // Determines the parent of this framework in the "Additional Libraries and Frameworks" menu
    return AppEngineStandardWebIntegration.getInstance().getUnderlyingFrameworkTypeId();
  }
}
