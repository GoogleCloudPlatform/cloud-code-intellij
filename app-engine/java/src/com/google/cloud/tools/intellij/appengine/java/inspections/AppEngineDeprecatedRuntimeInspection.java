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

package com.google.cloud.tools.intellij.appengine.java.inspections;

import com.google.cloud.tools.intellij.appengine.java.AppEngineMessageBundle;
import com.google.cloud.tools.intellij.appengine.java.util.AppEngineUtil;
import com.google.common.collect.ImmutableList;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.XmlSuppressableInspectionTool;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import java.util.List;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nls.Capitalization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An XML inspection that inspects appengine-web.xml files for deprecated App Engine Java runtime
 * values. Deprecated values will be underlined with an error, and a quickfix to update to the
 * supported Java 8 runtime will be presented
 */
public class AppEngineDeprecatedRuntimeInspection extends XmlSuppressableInspectionTool {

  private List<String> deprecatedRuntimes = ImmutableList.of("java", "java7");

  private static final String APP_ENGINE_JAVA8_RUNTIME_VALUE = "java8";
  private static final String APP_ENGINE_WEB_XML_ROOT_TAG_NAME = "appengine-web-app";
  private static final String APP_ENGINE_WEB_XML_RUNTIME_TAG_NAME = "runtime";

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return AppEngineMessageBundle
        .message("appengine.deprecated.java.runtime.inspection.display.name");
  }

  @Nullable
  @Override
  public String getStaticDescription() {
    return AppEngineMessageBundle
        .message("appengine.deprecated.java.runtime.inspection.description");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new XmlElementVisitor() {

      @Override
      public void visitXmlTag(XmlTag tag) {
        if (isAppEngineWebXmlDeprecatedRuntimeTag(tag)) {
          holder.registerProblem(tag,
              AppEngineMessageBundle
                  .message("appengine.deprecated.java.runtime.inspection.problem.text"),
              new UpdateDeprecatedAppEngineJavaRuntimeQuickFix());
        }
      }
    };
  }

  /**
   * A quickfix that updates deprecated App Engine Java runtime values to the supported Java 8
   * runtime.
   */
  private class UpdateDeprecatedAppEngineJavaRuntimeQuickFix implements LocalQuickFix {

    @Nls(capitalization = Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
      return AppEngineMessageBundle.message("appengine.deprecated.java.runtime.quickfix.text");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      XmlTag xmlTag = (XmlTag) descriptor.getPsiElement();
      xmlTag.getValue().setText(APP_ENGINE_JAVA8_RUNTIME_VALUE);
    }
  }

  /**
   * Returns {@code true} if the inspected tag is part of an appengine-web.xml config file, is a
   * runtime tag, and the value is a deprecated Java runtime.
   */
  private boolean isAppEngineWebXmlDeprecatedRuntimeTag(XmlTag tag) {
    XmlFile xmlFile = (XmlFile) tag.getContainingFile();
    boolean isAppEngineWebXml =
        xmlFile.getRootTag() != null && AppEngineUtil.APP_ENGINE_WEB_XML_NAME
            .equals(xmlFile.getName()) && APP_ENGINE_WEB_XML_ROOT_TAG_NAME
            .equals(xmlFile.getRootTag().getName());

    if (isAppEngineWebXml) {
      return APP_ENGINE_WEB_XML_RUNTIME_TAG_NAME.equals(tag.getName()) && deprecatedRuntimes
          .contains(tag.getValue().getText());
    }

    return false;
  }
}
