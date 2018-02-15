/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.debugger;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.cloud.tools.intellij.debugger.ui.BreakpointConfigurationPanel;
import com.google.cloud.tools.intellij.debugger.ui.BreakpointErrorStatusPanel;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.breakpoints.JavaBreakpointType;
import com.intellij.debugger.ui.breakpoints.LineBreakpoint;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointGroupingRule;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.JavaDebuggerEditorsProvider;

/**
 * CloudLineBreakpointType defines our custom line breakpoint. It adds properties for custom watches
 * and controls when its valid to add a cloud breakpoint instead of the normal java breakpoint.
 */
public class CloudLineBreakpointType extends XLineBreakpointType<CloudLineBreakpointProperties>
    implements JavaBreakpointType, Disposable {

  public CloudLineBreakpointType() {
    super("cloud-snapshotlocation", GctBundle.getString("clouddebug.breakpoint.description"));
  }

  public static CloudLineBreakpointType getInstance() {
    return XBreakpointType.EXTENSION_POINT_NAME.findExtension(CloudLineBreakpointType.class);
  }

  /**
   * We can only place a cloud breakpoint on a line if: 1) Its normally ok to do so with a java
   * breakpoint. 2) The selected run config is a {@link CloudDebugRunConfiguration}.
   */
  @Override
  public final boolean canPutAt(
      @NotNull final VirtualFile file, final int line, @NotNull Project project) {
    RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);
    if (runManager.getSelectedConfiguration() == null
        || !(runManager.getSelectedConfiguration().getConfiguration()
            instanceof CloudDebugRunConfiguration)) {
      return false;
    }

    // Most of this code is reused from java linebreakpoint.
    final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    if (psiFile == null || psiFile.getVirtualFile().getFileType() == StdFileTypes.XHTML) {
      return false;
    }

    if (!StdFileTypes.CLASS.equals(psiFile.getFileType())
        && !DebuggerUtils.isBreakpointAware(psiFile)) {
      return false;
    }

    final Document document = FileDocumentManager.getInstance().getDocument(file);
    final Ref<Class<? extends CloudLineBreakpointType>> result = Ref.create();
    assert document != null;
    XDebuggerUtil.getInstance()
        .iterateLine(
            project,
            document,
            line,
            new Processor<PsiElement>() {
              @Override
              public boolean process(PsiElement element) {
                // avoid comments
                if ((element instanceof PsiWhiteSpace)
                    || (PsiTreeUtil.getParentOfType(element, PsiComment.class, false) != null)) {
                  return true;
                }
                PsiElement parent = element;
                while (element != null) {
                  // skip modifiers
                  if (element instanceof PsiModifierList) {
                    element = element.getParent();
                    continue;
                  }

                  final int offset = element.getTextOffset();
                  if (offset >= 0) {
                    if (document.getLineNumber(offset) != line) {
                      break;
                    }
                  }
                  parent = element;
                  element = element.getParent();
                }

                if (parent instanceof PsiMethod) {
                  if (parent.getTextRange().getEndOffset() >= document.getLineEndOffset(line)) {
                    PsiCodeBlock body = ((PsiMethod) parent).getBody();
                    if (body != null) {
                      PsiStatement[] statements = body.getStatements();
                      if (statements.length > 0
                          && document.getLineNumber(statements[0].getTextOffset()) == line) {
                        result.set(CloudLineBreakpointType.class);
                      }
                    }
                  }
                } else {
                  result.set(CloudLineBreakpointType.class);
                }
                return true;
              }
            });

    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.CLOUD_DEBUGGER_CREATE_BREAKPOINT)
        .ping();
    return result.get() == getClass();
  }

  @Override
  public EnumSet<StandardPanels> getVisibleStandardPanels() {
    return EnumSet.noneOf(StandardPanels.class);
  }

  @Nullable
  @Override
  public CloudLineBreakpointProperties createBreakpointProperties(
      @NotNull VirtualFile file, int line) {
    return new CloudLineBreakpointProperties();
  }

  @Override
  @Nullable
  public XBreakpointCustomPropertiesPanel<XLineBreakpoint<CloudLineBreakpointProperties>>
      createCustomPropertiesPanel() {
    return new BreakpointConfigurationPanel(this);
  }

  @Override
  @Nullable
  public XBreakpointCustomPropertiesPanel<XLineBreakpoint<CloudLineBreakpointProperties>>
      createCustomTopPropertiesPanel(@NotNull Project project) {
    return new BreakpointErrorStatusPanel();
  }

  @NotNull
  @Override
  public Breakpoint createJavaBreakpoint(Project project, XBreakpoint breakpoint) {
    CloudLineBreakpoint lineBreakpoint = new CloudLineBreakpoint(project, breakpoint);
    lineBreakpoint.init();
    return lineBreakpoint;
  }

  @Nullable
  @Override
  public CloudLineBreakpointProperties createProperties() {
    return new CloudLineBreakpointProperties();
  }

  @Override
  public void dispose() {}

  @NotNull
  @Override
  public Icon getDisabledIcon() {
    return GoogleCloudToolsIcons.CLOUD_BREAKPOINT_DISABLED;
  }

  @Nullable
  @Override
  public final XDebuggerEditorsProvider getEditorsProvider(
      @NotNull XLineBreakpoint<CloudLineBreakpointProperties> breakpoint,
      @NotNull Project project) {
    return new JavaDebuggerEditorsProvider();
  }

  // TODO Use the new method provided by Jetbrains to customize the breakpoint ui and hide the
  // default panels.
  // @Override
  // @Nullable
  // public XBreakpointCustomPropertiesPanel<XLineBreakpoint<CloudLineBreakpointProperties>>
  // createMainPanel(@NotNull Project project) {
  //  return new BreakpointConfigurationPanel(this);
  // }

  @NotNull
  @Override
  public Icon getEnabledIcon() {
    return GoogleCloudToolsIcons.CLOUD_BREAKPOINT;
  }

  @Override
  public List<XBreakpointGroupingRule<XLineBreakpoint<CloudLineBreakpointProperties>, ?>>
      getGroupingRules() {
    return XDebuggerUtil.getInstance().getGroupingByFileRuleAsList();
  }

  @Override
  public int getPriority() {
    return 101;
  }

  @Override
  public boolean isAddBreakpointButtonVisible() {
    return false;
  }

  @Override
  public final boolean isSuspendThreadSupported() {
    return false;
  }

  public static class CloudLineBreakpoint extends LineBreakpoint {

    private String errorMessage = null;
    private boolean isVerified = false;

    public CloudLineBreakpoint(Project project, XBreakpoint breakpoint) {
      super(project, breakpoint);
    }

    @Override
    protected Icon getDisabledIcon(boolean isMuted) {
      return GoogleCloudToolsIcons.CLOUD_BREAKPOINT_DISABLED;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
    }

    @Override
    protected Icon getSetIcon(boolean isMuted) {
      if (!Strings.isNullOrEmpty(errorMessage)) {
        return GoogleCloudToolsIcons.CLOUD_BREAKPOINT_ERROR;
      }
      if (isVerified) {
        return GoogleCloudToolsIcons.CLOUD_BREAKPOINT_CHECKED;
      }
      return GoogleCloudToolsIcons.CLOUD_BREAKPOINT;
    }

    /** Get the watch expressions from the breakpoint. */
    @Nullable
    public List<String> getWatchExpressions() {
      CloudLineBreakpointProperties properties =
          (CloudLineBreakpointProperties) getXBreakpoint().getProperties();
      if (properties.getWatchExpressions() != null && properties.getWatchExpressions().length > 0) {
        return Arrays.asList(properties.getWatchExpressions());
      }
      return null;
    }

    public boolean hasError() {
      return !Strings.isNullOrEmpty(getErrorMessage());
    }

    public void setVerified(boolean isVerified) {
      this.isVerified = isVerified;
    }

    public boolean isVerified() {
      return isVerified;
    }
  }
}
