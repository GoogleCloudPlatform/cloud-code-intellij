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
package com.google.gct.idea.debugger.ui;

import com.google.gct.idea.debugger.CloudLineBreakpointProperties;
import com.google.gct.idea.debugger.CloudLineBreakpointType;
import com.google.gct.idea.debugger.CloudLineBreakpointType.CloudLineBreakpoint;

import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.breakpoints.BreakpointManager;
import com.intellij.ide.DataManager;
import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.*;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.impl.XDebuggerUtilImpl;
import com.intellij.xdebugger.impl.actions.XDebuggerActions;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointUtil;
import com.intellij.xdebugger.impl.frame.XWatchesView;
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil;
import com.intellij.xdebugger.impl.ui.XDebuggerExpressionComboBox;
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTreePanel;
import com.intellij.xdebugger.impl.ui.tree.nodes.WatchNode;
import com.intellij.xdebugger.impl.ui.tree.nodes.WatchesRootNode;
import com.intellij.xdebugger.impl.ui.tree.nodes.XDebuggerTreeNode;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * The breakpoint config panel is shown for both the config popup (right click on a breakpoint) and in the full
 * breakpoint manager dialog. Cloud snapshot locations can have a condition and custom watches. They do not support
 * "Suspend" options.
 */
public class BreakpointConfigurationPanel
    extends XBreakpointCustomPropertiesPanel<XLineBreakpoint<CloudLineBreakpointProperties>>
    implements Disposable, XWatchesView {

  private static final Logger LOG = Logger.getInstance(BreakpointConfigurationPanel.class);
  private final CloudLineBreakpointType myCloudLineBreakpointType;
  private JPanel myMainPanel;
  private WatchesRootNode myRootNode;
  private JBCheckBox mySuspendCheckbox;
  private XDebuggerTreePanel myTreePanel;
  private JBLabel myWatchLabel;
  private JPanel myWatchPanel;

  public BreakpointConfigurationPanel(@NotNull CloudLineBreakpointType cloudLineBreakpointType) {
    myCloudLineBreakpointType = cloudLineBreakpointType;

    // We conditionally show the "custom watches" panel only if we are shown in the dialog.
    $$$setupUI$$$();
    myWatchPanel.addAncestorListener(new AncestorListener() {
      @Override
      public void ancestorAdded(AncestorEvent event) {
        JRootPane pane = myWatchPanel.getRootPane();
        if (pane != null) {
          myWatchPanel.setVisible(UIUtil.isDialogRootPane(pane));
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
    myRootNode.addWatchExpression(null, expression, index, navigateToWatchNode);
  }

  @Override
  public void dispose() {
    if (myTreePanel != null && myTreePanel.getTree() != null) {
      myTreePanel.getTree().dispose();
    }
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myMainPanel;
  }

  @Override
  public void loadFrom(@NotNull XLineBreakpoint<CloudLineBreakpointProperties> breakpoint) {
    XBreakpointBase lineBreakpointImpl = breakpoint instanceof XBreakpointBase ? (XBreakpointBase) breakpoint : null;
    Breakpoint javaBreakpoint = BreakpointManager.getJavaBreakpoint(breakpoint);
    CloudLineBreakpoint cloudBreakpoint = null;
    if (javaBreakpoint instanceof CloudLineBreakpoint) {
      cloudBreakpoint = (CloudLineBreakpoint) javaBreakpoint;
    }

    if (cloudBreakpoint == null || lineBreakpointImpl == null) {
      return;
    }

    XDebuggerEditorsProvider debuggerEditorsProvider =
        myCloudLineBreakpointType.getEditorsProvider(breakpoint, cloudBreakpoint.getProject());

    if (debuggerEditorsProvider != null) {
      myTreePanel = new XDebuggerTreePanel(cloudBreakpoint.getProject(),
          debuggerEditorsProvider,
          this,
          breakpoint.getSourcePosition(),
          "GoogleCloudTools.BreakpointWatchContextMenu",
          null);
      List<XExpression> watches = new ArrayList<XExpression>();
      for (String watchExpression : breakpoint.getProperties().getWatchExpressions()) {
        watches.add(debuggerEditorsProvider
            .createExpression(((XBreakpointBase) breakpoint).getProject(),
                new DocumentImpl(watchExpression),
                getFileTypeLanguage(breakpoint),
                EvaluationMode.EXPRESSION));
      }

      myRootNode = new WatchesRootNode(myTreePanel.getTree(), this, watches.toArray(new XExpression[watches.size()]));
      myTreePanel.getTree().setRoot(myRootNode, false);

      myWatchPanel.removeAll();
      myWatchPanel.add(myWatchLabel, BorderLayout.NORTH);
      myTreePanel.getTree().getEmptyText().setText("There are no custom watches for this snapshot location.");
      final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myTreePanel.getTree()).disableUpDownActions();
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
      myWatchPanel.add(decorator.createPanel(), BorderLayout.CENTER);
    }
  }

  @Override
  public void removeAllWatches() {
    myRootNode.removeAllChildren();
  }

  @Override
  public void removeWatches(List<? extends XDebuggerTreeNode> nodes) {
    List<? extends WatchNode> children = myRootNode.getAllChildren();
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
    myRootNode.removeChildren(toRemove);

    List<? extends WatchNode> newChildren = myRootNode.getAllChildren();
    if (newChildren != null && !newChildren.isEmpty()) {
      WatchNode node =
          minIndex < newChildren.size() ? newChildren.get(minIndex) : newChildren.get(newChildren.size() - 1);
      TreeUtil.selectNode(myTreePanel.getTree(), node);
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
        xIdebreakpoint instanceof XBreakpointBase ? (XBreakpointBase) xIdebreakpoint : null;

    if (myRootNode != null && lineBreakpointImpl != null) {
      List<String> expressionsToSave = new ArrayList<String>();
      for (WatchNode node : myRootNode.getAllChildren()) {
        expressionsToSave.add(node.getExpression().getExpression());
      }
      if (properties.setWatchExpressions(expressionsToSave.toArray(new String[expressionsToSave.size()]))) {
        lineBreakpointImpl.fireBreakpointChanged();
      }
    }
  }

  @Nullable
  private static Language getFileTypeLanguage(XLineBreakpoint<CloudLineBreakpointProperties> breakpoint) {
    if (breakpoint.getSourcePosition() != null) {
      FileType fileType = breakpoint.getSourcePosition().getFile().getFileType();
      if (fileType instanceof LanguageFileType) {
        return ((LanguageFileType) fileType).getLanguage();
      }
    }
    return null;
  }

  private void createUIComponents() {
    myWatchPanel = new MyPanel();
  }

  /**
   * Executes the standard add and remove watch from the watch list.
   */
  private void executeAction(@NotNull String watch) {
    AnAction action = ActionManager.getInstance().getAction(watch);
    Presentation presentation = action.getTemplatePresentation().clone();
    DataContext context = DataManager.getInstance().getDataContext(myTreePanel.getTree());

    AnActionEvent actionEvent =
        new AnActionEvent(null, context, ActionPlaces.DEBUGGER_TOOLBAR, presentation, ActionManager.getInstance(), 0);
    action.actionPerformed(actionEvent);
  }

  /**
   * Method generated by IntelliJ IDEA GUI Designer >>> IMPORTANT!! <<< DO NOT edit this method OR call it in your code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    createUIComponents();
    myMainPanel = new JPanel();
    myMainPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 10, 0), -1, -1));
    final Spacer spacer1 = new Spacer();
    myMainPanel.add(spacer1,
        new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    myWatchPanel.setLayout(new BorderLayout(0, 0));
    myMainPanel.add(myWatchPanel,
        new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null,
            new Dimension(-1, 100), null, 0, false));
    myWatchLabel = new JBLabel();
    myWatchLabel.setText("Watches:");
    myWatchPanel.add(myWatchLabel, BorderLayout.NORTH);
    final Spacer spacer2 = new Spacer();
    myMainPanel.add(spacer2,
        new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    mySuspendCheckbox = new JBCheckBox();
    mySuspendCheckbox.setEnabled(false);
    this.$$$loadButtonText$$$(mySuspendCheckbox,
        ResourceBundle.getBundle("messages/CloudToolsBundle").getString("clouddebug.suspendnotavailable"));
    myMainPanel.add(mySuspendCheckbox,
        new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JBLabel jBLabel1 = new JBLabel();
    jBLabel1.setFont(new Font(jBLabel1.getFont().getName(), Font.ITALIC, 10));
    jBLabel1.setHorizontalAlignment(10);
    this.$$$loadLabelText$$$(jBLabel1,
        ResourceBundle.getBundle("messages/CloudToolsBundle").getString("clouddebug.neversuspends"));
    jBLabel1.setVerticalAlignment(0);
    jBLabel1.setVerticalTextPosition(0);
    myMainPanel.add(jBLabel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 3, false));
  }

  /**
   * @noinspection ALL
   */
  private void $$$loadLabelText$$$(JLabel component, String text) {
    StringBuffer result = new StringBuffer();
    boolean haveMnemonic = false;
    char mnemonic = '\0';
    int mnemonicIndex = -1;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '&') {
        i++;
        if (i == text.length()) {
          break;
        }
        if (!haveMnemonic && text.charAt(i) != '&') {
          haveMnemonic = true;
          mnemonic = text.charAt(i);
          mnemonicIndex = result.length();
        }
      }
      result.append(text.charAt(i));
    }
    component.setText(result.toString());
    if (haveMnemonic) {
      component.setDisplayedMnemonic(mnemonic);
      component.setDisplayedMnemonicIndex(mnemonicIndex);
    }
  }

  /**
   * @noinspection ALL
   */
  private void $$$loadButtonText$$$(AbstractButton component, String text) {
    StringBuffer result = new StringBuffer();
    boolean haveMnemonic = false;
    char mnemonic = '\0';
    int mnemonicIndex = -1;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '&') {
        i++;
        if (i == text.length()) {
          break;
        }
        if (!haveMnemonic && text.charAt(i) != '&') {
          haveMnemonic = true;
          mnemonic = text.charAt(i);
          mnemonicIndex = result.length();
        }
      }
      result.append(text.charAt(i));
    }
    component.setText(result.toString());
    if (haveMnemonic) {
      component.setMnemonic(mnemonic);
      component.setDisplayedMnemonicIndex(mnemonicIndex);
    }
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return myMainPanel;
  }

  /**
   * The XWatchesView contract is used to actually perform the add/remove watch when the item is added or removed from the
   * watches view.  It is supplied somewhat indirectly through the visual hierarchy via getData.
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
