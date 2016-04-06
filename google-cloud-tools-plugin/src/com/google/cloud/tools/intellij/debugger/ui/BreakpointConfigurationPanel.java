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
package com.google.cloud.tools.intellij.debugger.ui;

import com.google.cloud.tools.intellij.debugger.CloudLineBreakpointProperties;
import com.google.cloud.tools.intellij.debugger.CloudLineBreakpointType;
import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.breakpoints.BreakpointManager;
import com.intellij.ide.DataManager;
import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.CaptionPanel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.impl.actions.XDebuggerActions;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase;
import com.intellij.xdebugger.impl.frame.XWatchesView;
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTreePanel;
import com.intellij.xdebugger.impl.ui.tree.nodes.WatchNode;
import com.intellij.xdebugger.impl.ui.tree.nodes.WatchesRootNode;
import com.intellij.xdebugger.impl.ui.tree.nodes.XDebuggerTreeNode;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * The breakpoint config panel is shown for both the config popup (right click on a breakpoint) and in the full
 * breakpoint manager dialog. Cloud snapshot locations can have a condition and custom watches. They do not support
 * "Suspend" options.
 */
public class BreakpointConfigurationPanel
    extends XBreakpointCustomPropertiesPanel<XLineBreakpoint<CloudLineBreakpointProperties>>
    implements Disposable, XWatchesView {

  private static final Logger LOG = Logger.getInstance(BreakpointConfigurationPanel.class);
  private final CloudLineBreakpointType cloudLineBreakpointType;
  private JPanel mainPanel;
  private WatchesRootNode rootNode;
  private XDebuggerTreePanel treePanel;
  private JBLabel watchLabel;
  private JPanel watchPanel;

  public BreakpointConfigurationPanel(@NotNull CloudLineBreakpointType cloudLineBreakpointType) {
    this.cloudLineBreakpointType = cloudLineBreakpointType;

    // We conditionally show the "custom watches" panel only if we are shown in the dialog.
    watchPanel.addAncestorListener(new AncestorListener() {
      @Override
      public void ancestorAdded(AncestorEvent event) {
        JRootPane pane = watchPanel.getRootPane();
        if (pane != null) {
          watchPanel.setVisible(UIUtil.isDialogRootPane(pane));
        }
      }

      @Override
      public void ancestorMoved(AncestorEvent event) {
      }

      @Override
      public void ancestorRemoved(AncestorEvent event) {
      }
    });
  }

  @Override
  public void addWatchExpression(@NotNull XExpression expression, int index, boolean navigateToWatchNode) {
    rootNode.addWatchExpression(null, expression, index, navigateToWatchNode);
  }

  @Override
  public void dispose() {
    if (treePanel != null && treePanel.getTree() != null) {
      treePanel.getTree().dispose();
    }
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return mainPanel;
  }

  @Override
  public void loadFrom(@NotNull XLineBreakpoint<CloudLineBreakpointProperties> breakpoint) {
    XBreakpointBase lineBreakpointImpl = breakpoint instanceof XBreakpointBase ? (XBreakpointBase)breakpoint : null;
    Breakpoint javaBreakpoint = BreakpointManager.getJavaBreakpoint(breakpoint);
    CloudLineBreakpointType.CloudLineBreakpoint cloudBreakpoint = null;
    if (javaBreakpoint instanceof CloudLineBreakpointType.CloudLineBreakpoint) {
      cloudBreakpoint = (CloudLineBreakpointType.CloudLineBreakpoint)javaBreakpoint;
    }

    if (cloudBreakpoint == null || lineBreakpointImpl == null) {
      return;
    }

    XDebuggerEditorsProvider debuggerEditorsProvider =
      cloudLineBreakpointType.getEditorsProvider(breakpoint, cloudBreakpoint.getProject());

    if (debuggerEditorsProvider != null) {
      treePanel = new XDebuggerTreePanel(cloudBreakpoint.getProject(),
                                           debuggerEditorsProvider,
                                           this,
                                           breakpoint.getSourcePosition(),
                                           "GoogleCloudTools.BreakpointWatchContextMenu",
                                           null);
      List<XExpression> watches = new ArrayList<XExpression>();
      for (String watchExpression : breakpoint.getProperties().getWatchExpressions()) {
        watches.add(debuggerEditorsProvider
                      .createExpression(((XBreakpointBase)breakpoint).getProject(),
                                        new DocumentImpl(watchExpression),
                                        getFileTypeLanguage(breakpoint),
                                        EvaluationMode.EXPRESSION));
      }

      rootNode = new WatchesRootNode(treePanel.getTree(), this, watches.toArray(new XExpression[watches.size()]));
      treePanel.getTree().setRoot(rootNode, false);

      watchPanel.removeAll();
      watchPanel.add(watchLabel, BorderLayout.NORTH);
      treePanel.getTree().getEmptyText().setText("There are no custom watches for this snapshot location.");
      final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(treePanel.getTree()).disableUpDownActions();
      decorator.setToolbarPosition(ActionToolbarPosition.RIGHT);
      decorator.setAddAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton button) {
          executeAction(XDebuggerActions.XNEW_WATCH);
        }
      });
      decorator.setRemoveAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton button) {
          executeAction(XDebuggerActions.XREMOVE_WATCH);
        }
      });
      CustomLineBorder border = new CustomLineBorder(CaptionPanel.CNT_ACTIVE_BORDER_COLOR, SystemInfo.isMac ? 1 : 0, 0,
                                                     SystemInfo.isMac ? 0 : 1, 0);
      decorator.setToolbarBorder(border);
      watchPanel.add(decorator.createPanel(), BorderLayout.CENTER);
    }
  }

  @Override
  public void removeAllWatches() {
    rootNode.removeAllChildren();
  }

  @Override
  public void removeWatches(List<? extends XDebuggerTreeNode> nodes) {
    List<? extends WatchNode> children = rootNode.getAllChildren();
    int minIndex = Integer.MAX_VALUE;
    List<XDebuggerTreeNode> toRemove = new ArrayList<XDebuggerTreeNode>();
    if (children != null) {
      for (XDebuggerTreeNode node : nodes) {
        @SuppressWarnings("SuspiciousMethodCalls") int index = children.indexOf(node);
        if (index != -1) {
          toRemove.add(node);
          minIndex = Math.min(minIndex, index);
        }
      }
    }
    rootNode.removeChildren(toRemove);

    List<? extends WatchNode> newChildren = rootNode.getAllChildren();
    if (newChildren != null && !newChildren.isEmpty()) {
      WatchNode node =
        minIndex < newChildren.size() ? newChildren.get(minIndex) : newChildren.get(newChildren.size() - 1);
      TreeUtil.selectNode(treePanel.getTree(), node);
    }
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  public void saveTo(@NotNull final XLineBreakpoint<CloudLineBreakpointProperties> xIdebreakpoint) {
    CloudLineBreakpointProperties properties = xIdebreakpoint.getProperties();
    if (properties == null) {
      LOG.error(
        "Could not save changes to the breakpoint because for some reason it does not have cloud " + "properties.");
      return;
    }

    XBreakpointBase lineBreakpointImpl =
      xIdebreakpoint instanceof XBreakpointBase ? (XBreakpointBase)xIdebreakpoint : null;

     if (rootNode != null && lineBreakpointImpl != null) {
      List<String> expressionsToSave = new ArrayList<String>();
      List<? extends WatchNode> children = rootNode.getAllChildren();
      if (children != null) {
        for (WatchNode node : rootNode.getAllChildren()) {
          expressionsToSave.add(node.getExpression().getExpression());
        }
        if (properties.setWatchExpressions(expressionsToSave.toArray(new String[expressionsToSave.size()]))) {
          lineBreakpointImpl.fireBreakpointChanged();
        }
      }
    }
  }

  @Nullable
  private static Language getFileTypeLanguage(XLineBreakpoint<CloudLineBreakpointProperties> breakpoint) {
    if (breakpoint.getSourcePosition() != null) {
      FileType fileType = breakpoint.getSourcePosition().getFile().getFileType();
      if (fileType instanceof LanguageFileType) {
        return ((LanguageFileType)fileType).getLanguage();
      }
    }
    return null;
  }

  private void createUIComponents() {
    watchPanel = new MyPanel();
  }

  /**
   * Executes the standard add and remove watch from the watch list.
   */
  private void executeAction(@NotNull String watch) {
    AnAction action = ActionManager.getInstance().getAction(watch);
    Presentation presentation = action.getTemplatePresentation().clone();
    DataContext context = DataManager.getInstance().getDataContext(treePanel.getTree());

    AnActionEvent actionEvent =
      new AnActionEvent(null, context, ActionPlaces.DEBUGGER_TOOLBAR, presentation, ActionManager.getInstance(), 0);
    action.actionPerformed(actionEvent);
  }

  /**
   * The XWatchesView contract is used to actually perform the add/remove watch when the item is added or removed from
   * the watches view.  It is supplied somewhat indirectly through the visual hierarchy via getData.
   */
  private class MyPanel extends JPanel implements DataProvider {
    public MyPanel() {
      setLayout(new BorderLayout());
    }

    @Nullable
    @Override
    public Object getData(@NonNls String dataId) {
      if (XWatchesView.DATA_KEY.is(dataId)) {
        return BreakpointConfigurationPanel.this;
      }
      return null;
    }
  }
}
