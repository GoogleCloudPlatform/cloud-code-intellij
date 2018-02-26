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

package com.google.cloud.tools.intellij.debugger.ui;

import com.google.api.services.clouddebugger.v2.model.Breakpoint;
import com.google.api.services.clouddebugger.v2.model.StatusMessage;
import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.cloud.tools.intellij.debugger.BreakpointUtil;
import com.google.cloud.tools.intellij.debugger.CloudBreakpointListener;
import com.google.cloud.tools.intellij.debugger.CloudDebugProcess;
import com.google.cloud.tools.intellij.debugger.CloudDebugProcessHandler;
import com.google.cloud.tools.intellij.debugger.CloudDebugProcessState;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.diagnostic.logging.AdditionalTabComponent;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.Balloon.Position;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.UI;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.impl.breakpoints.ui.BreakpointsDialogFactory;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextAttribute;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This panel shows the list of cloud debugger snapshots. It contains one Swing table which is
 * divided into five columns:
 *
 * <p>0. An icon indicating the state of the breakpoint 1. A date-time for received snapshots or the
 * word "Pending" otherwise. 2. The file and line number of the snapshot; e.g.
 * "GeneratorServlet.java:40" 3. The breakpoint condition, if any 4. For pending snapshots only, the
 * word "More" which is a link to the Breakpoints dialog.
 */
// todo: why *historical* snapshots? Isn't this just all snapshots?
public class CloudDebugHistoricalSnapshots extends AdditionalTabComponent
    implements XDebugSessionListener, CloudBreakpointListener {

  private static final int COLUMN_MARGIN_PX = 3;
  private static final Cursor DEFAULT_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
  private static final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
  private static final int WINDOW_HEIGHT_PX =
      8 * JBTable.PREFERRED_SCROLLABLE_VIEWPORT_HEIGHT_IN_ROWS;
  private static final int WINDOW_WIDTH_PX = 200;
  @VisibleForTesting final JBTable table;
  @VisibleForTesting Balloon balloon = null;
  private CloudDebugProcess process;

  /** Initialize the panel. */
  public CloudDebugHistoricalSnapshots(@NotNull CloudDebugProcessHandler processHandler) {
    super(new BorderLayout());

    table = new CloudDebuggerTable();

    configureToolbar();

    process = processHandler.getProcess();

    process.getXDebugSession().addSessionListener(this);
    process.addListener(this);
  }

  /** Sets up the the toolbar that appears in the cloud debugger snapshots panel. */
  private void configureToolbar() {
    final ToolbarDecorator decorator =
        ToolbarDecorator.createDecorator(table)
            .disableUpDownActions()
            .disableAddAction()
            .setToolbarPosition(ActionToolbarPosition.TOP);

    decorator.setRemoveAction(new RemoveSelectedBreakpointsAction());
    decorator.addExtraAction(new RemoveAllBreakpointsAction());
    decorator.addExtraAction(new ReactivateBreakpointAction());

    this.add(decorator.createPanel());
  }

  @Override
  public void beforeSessionResume() {}

  @Override
  public void dispose() {
    // todo: can we clear pending snapshots here to keep them from getting duped on reconnect?
    // https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/142
  }

  @Override
  public JComponent getPreferredFocusableComponent() {
    return table;
  }

  @Nullable
  @Override
  public JComponent getSearchComponent() {
    return null;
  }

  @NotNull
  @Override
  public String getTabTitle() {
    return GctBundle.getString("clouddebug.snapshots");
  }

  @Nullable
  @Override
  public ActionGroup getToolbarActions() {
    return null;
  }

  @Nullable
  @Override
  public JComponent getToolbarContextComponent() {
    return null;
  }

  @Nullable
  @Override
  public String getToolbarPlace() {
    return ActionPlaces.UNKNOWN;
  }

  @Override
  public boolean isContentBuiltIn() {
    return false;
  }

  @Override
  public void onBreakpointListChanged(CloudDebugProcessState state) {
    // todo: I don't think anyone else implements this or uses CloudDebugProcessState here.
    // verify and if so, remove that argument
    onBreakpointsChanged();
  }

  @Override
  public void sessionPaused() {
    onBreakpointsChanged();
  }

  @Override
  public void sessionResumed() {}

  @Override
  public void sessionStopped() {
    process.removeListener(this);

    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            table.setModel(new SnapshotsModel(CloudDebugHistoricalSnapshots.this, null, null));
          }
        });
  }

  @Override
  public void stackFrameChanged() {}

  @VisibleForTesting
  int getSelection() {
    final List<Breakpoint> breakpointList = process.getCurrentBreakpointList();
    int selection = -1;

    if (breakpointList != null) {
      for (int i = 0; i < breakpointList.size(); i++) {
        Breakpoint snapshot = process.getCurrentSnapshot();
        if (snapshot != null && breakpointList.get(i).getId().equals(snapshot.getId())) {
          selection = i;
          break;
        }
      }
    }

    return selection;
  }

  /**
   * Deletes breakpoints asynchronously on a threadpool thread. The user will see these breakpoints
   * gradually disappear.
   */
  private void fireDeleteBreakpoints(@NotNull final List<Breakpoint> breakpointsToDelete) {
    for (Breakpoint breakpoint : breakpointsToDelete) {
      getModel().markForDelete(breakpoint.getId());
      process.getBreakpointHandler().deleteBreakpoint(breakpoint);
    }
    getModel().fireTableDataChanged();
  }

  @Nullable
  private Breakpoint getBreakPoint(@NotNull Point point) {
    int row = table.rowAtPoint(point);
    if (row >= 0 && row < getModel().getBreakpoints().size()) {
      return getModel().getBreakpoints().get(row);
    }
    return null;
  }

  @NotNull
  private SnapshotsModel getModel() {
    return (SnapshotsModel) table.getModel();
  }

  /**
   * Used by delete and clone, this returns the lines the user currently has selected in the
   * snapshot list.
   */
  @NotNull
  private List<Breakpoint> getSelectedBreakpoints() {
    List<Breakpoint> selectedBreakpoints = new ArrayList<Breakpoint>();
    SnapshotsModel model = (SnapshotsModel) table.getModel();
    int[] selectedRows = table.getSelectedRows();
    for (int selectedRow : selectedRows) {
      selectedBreakpoints.add(model.getBreakpoints().get(selectedRow));
    }
    return selectedBreakpoints;
  }

  /**
   * This is fired when the set of breakpoints from the server changes. We create a new table model
   * while keeping the selection as it was. Most routines on selection are therefore based on the
   * breakpoint Id and not a reference to a breakpoint object -- because we never know when the
   * server instance will get replaced.
   */
  private void onBreakpointsChanged() {
    // Read the list of breakpoints and show them.
    // We always snap the current breakpoint list into a local to eliminate threading issues.
    final List<Breakpoint> breakpointList = process.getCurrentBreakpointList();

    // Setting the model must happen on the UI thread, while most of this method executes on the
    // background.
    SwingUtilities.invokeLater(new ModelSetter(breakpointList, getSelection()));
  }

  /** Resizes the table to respect the contents of each column. */
  // todo: arguably belongs inside ColumnDebuggerTable class
  private void resizeColumnWidth() {
    final TableColumnModel columnModel = table.getColumnModel();
    for (int column = 0; column < table.getColumnCount(); column++) {
      int width = 2; // Min width
      for (int row = 0; row < table.getRowCount(); row++) {
        TableCellRenderer renderer = table.getCellRenderer(row, column);
        Component comp = table.prepareRenderer(renderer, row, column);
        width = Math.max(comp.getPreferredSize().width, width);
      }
      width += COLUMN_MARGIN_PX;
      columnModel.getColumn(column).setPreferredWidth(width);
      columnModel.getColumn(column).setMaxWidth(width);
      // The first three columns do not shrink when the window is resized smaller.
      if (column <= 2) {
        columnModel.getColumn(column).setMinWidth(width);
      }
    }
  }

  /**
   * Returns true if we have a local representation of the snapshot. The snapshot may be pending or
   * in final state. If in final state, then the local representation will be disabled (not
   * enabled). The user can re-enable the local state and it will create a new pending snapshot and
   * de-link the old snapshot from the local representation.
   *
   * <p>Snapshots that support more config show a "More..." link in the rightmost column of the
   * table.
   */
  // todo: is there any feasible way to push this into the breakpoint class itself?
  // i.e. breakpoint.supportsMoreConfig()?
  boolean supportsMoreConfig(@Nullable Breakpoint breakpoint) {
    return process.getBreakpointHandler().getXBreakpoint(breakpoint) != null;
  }

  private void selectSnapshot(Breakpoint breakpoint, boolean isSelectedBeforeTrigger) {
    getModel().unMarkAsNewlyReceived(breakpoint.getId());

    if (isSelectedBeforeTrigger || isNewlySelected(breakpoint)) {
      process.navigateToSnapshot(breakpoint.getId());
    }
  }

  private boolean isNewlySelected(Breakpoint breakpoint) {
    return process.getCurrentSnapshot() == null
        || !process.getCurrentSnapshot().getId().equals(breakpoint.getId());
  }

  private static final class MoreCellRenderer extends DefaultTableCellRenderer {

    MoreCellRenderer() {
      setHorizontalAlignment(SwingConstants.LEFT);
      setForeground(UI.getColor("link.foreground"));
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      setBorder(noFocusBorder);
      if (value != null) {
        setText(value.toString());
      }
      return this;
    }
  }

  private final class SnapshotTimeCellRenderer extends DefaultTableCellRenderer {

    private final DateFormat dateFormat =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    private final DateFormat dateformatToday = DateFormat.getTimeInstance(DateFormat.SHORT);
    private final Date todayDate;

    public SnapshotTimeCellRenderer() {
      setHorizontalAlignment(SwingConstants.LEFT);
      Calendar calendar = new GregorianCalendar();
      calendar.set(Calendar.HOUR_OF_DAY, 0);
      calendar.set(Calendar.MINUTE, 0);
      calendar.set(Calendar.SECOND, 0);
      todayDate = calendar.getTime();
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      setBorder(noFocusBorder);

      if (value instanceof Date) {
        Date finalDate = (Date) value;
        if (finalDate.after(todayDate)) {
          setText(dateformatToday.format(finalDate));
        } else {
          setText(dateFormat.format(finalDate));
        }
      } else if (value != null) {
        setText(value.toString());
      } else {
        setText("");
      }

      if (getModel().isNewlyReceived(row)) {
        Font font = getFont();
        Font boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
        setFont(boldFont);
      } else {
        Font font = getFont();
        Font plainFont = new Font(font.getFontName(), Font.PLAIN, font.getSize());
        setFont(plainFont);
      }

      if (getModel().isMarkedForDelete(row)) {
        if (!isSelected) {
          setForeground(UIUtil.getInactiveTextColor());
        }
        Font font = getFont();
        Map attributes = font.getAttributes();
        attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        attributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        setFont(new Font(attributes));
      } else if (!isSelected) {
        setForeground(UIUtil.getActiveTextColor());
      }

      return this;
    }
  }

  private class DefaultRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      setBorder(noFocusBorder);
      if (getModel().isMarkedForDelete(row)) {
        if (!isSelected) {
          setForeground(UIUtil.getInactiveTextColor());
        }
        Font font = getFont();
        Map attributes = font.getAttributes();
        attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        attributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        setFont(new Font(attributes));
      } else if (!isSelected) {
        setForeground(UIUtil.getActiveTextColor());

        if (getModel().isNewlyReceived(row)) {
          Font font = getFont();
          Font plainFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
          setFont(plainFont);
        } else {
          Font font = getFont();
          Font boldFont = new Font(font.getFontName(), Font.PLAIN, font.getSize());
          setFont(boldFont);
        }
      }
      return this;
    }
  }

  private class RemoveSelectedBreakpointsAction implements AnActionButtonRunnable {

    @Override
    public void run(AnActionButton button) {
      // todo(elharo): inject a pointer to the parent class, and we can make this class static
      List<Breakpoint> selectedBreakpoints = getSelectedBreakpoints();
      fireDeleteBreakpoints(selectedBreakpoints);
    }
  }

  private class RemoveAllBreakpointsAction extends AnActionButton {

    RemoveAllBreakpointsAction() {
      super(
          GctBundle.getString("clouddebug.delete.all"),
          GoogleCloudToolsIcons.CLOUD_DEBUG_DELETE_ALL_BREAKPOINTS);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
      int result =
          Messages.showOkCancelDialog(
              GctBundle.getString("clouddebug.remove.all"),
              GctBundle.getString("clouddebug.delete.snapshots"),
              GctBundle.getString("clouddebug.buttondelete"),
              GctBundle.getString("clouddebug.cancelbutton"),
              Messages.getQuestionIcon());

      if (result == Messages.OK) { // pressed remove all
        SnapshotsModel model = getModel();
        fireDeleteBreakpoints(model.getBreakpoints());
      }
    }
  }

  private class ReactivateBreakpointAction extends AnActionButton {

    public ReactivateBreakpointAction() {
      super(
          GctBundle.getString("clouddebug.reactivatesnapshotlocation"),
          GoogleCloudToolsIcons.CLOUD_DEBUG_REACTIVATE_BREAKPOINT);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
      process.getBreakpointHandler().cloneToNewBreakpoints(getSelectedBreakpoints());
    }
  }

  private class CloudDebuggerTable extends JBTable {

    CloudDebuggerTable() {
      setModel(new SnapshotsModel(CloudDebugHistoricalSnapshots.this, null, null));
      setTableHeader(null);
      setShowGrid(false);
      setRowMargin(0);
      getColumnModel().setColumnMargin(0);
      getColumnModel().getColumn(1).setCellRenderer(new SnapshotTimeCellRenderer());
      getColumnModel().getColumn(2).setCellRenderer(new DefaultRenderer());
      getColumnModel().getColumn(3).setCellRenderer(new DefaultRenderer());
      getColumnModel().getColumn(4).setCellRenderer(new MoreCellRenderer());
      resetDefaultFocusTraversalKeys();
      setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
      setPreferredScrollableViewportSize(new Dimension(WINDOW_WIDTH_PX, WINDOW_HEIGHT_PX));
      setAutoCreateColumnsFromModel(false);
      getEmptyText().setText(GctBundle.getString("clouddebug.nosnapshots"));

      addMouseListener(new SnapshotClicker());
      addMouseMotionListener(new CursorSwitcher());
    }

    //  Returning the Class of each column allows different renderers to be used based on Class
    @Override
    public Class getColumnClass(int column) {
      if (column == 0) {
        return Icon.class;
      }
      Object value = getValueAt(0, column);
      return value != null ? getValueAt(0, column).getClass() : String.class;
    }

    // We override prepareRenderer to supply a tooltip in the case of an error.
    @NotNull
    @Override
    public Component prepareRenderer(@NotNull TableCellRenderer renderer, int row, int column) {
      Component component = super.prepareRenderer(renderer, row, column);
      if (component instanceof JComponent) {
        JComponent jc = (JComponent) component;
        SnapshotsModel model = (SnapshotsModel) getModel();
        Breakpoint breakpoint = model.getBreakpoints().get(row);
        jc.setToolTipText(BreakpointUtil.getUserErrorMessage(breakpoint.getStatus()));
      }
      return component;
    }
  }

  /**
   * This click handler does one of three things:
   *
   * <p>1. Single click on a final snapshot will load the debugger with that snapshot. 2. Single
   * click on a pending snapshot will show the line of code. 3. Single click on "More" will show the
   * breakpoint config dialog. todo: single click on a pending snapshot clears the debugger with the
   * previous snapshot https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/143
   */
  private class SnapshotClicker extends MouseAdapter {

    @Override
    public void mousePressed(MouseEvent event) {
      JTable table = (JTable) event.getSource();
      Point point = event.getPoint();
      Breakpoint breakpoint = getBreakPoint(point);
      int column = table.columnAtPoint(point);
      // todo: 4 and 1 here are magic numbers; use named constants for columns; maybe define
      // in CloudDebuggerTable class
      if (breakpoint != null && column == 4 && supportsMoreConfig(breakpoint)) {
        BreakpointsDialogFactory.getInstance(process.getXDebugSession().getProject())
            .showDialog(process.getBreakpointHandler().getXBreakpoint(breakpoint));
      } else if (event.getClickCount() == 1
          && breakpoint != null
          && table.getSelectedRows().length == 1) {
        selectSnapshot(breakpoint, false);
      }
    }
  }

  /** Create a hand cursor over a link within a table. */
  private class CursorSwitcher implements MouseMotionListener {

    @Override
    public void mouseDragged(MouseEvent me) {}

    @Override
    public void mouseMoved(MouseEvent event) {
      JTable table = (JTable) event.getSource();
      Point point = event.getPoint();
      int column = table.columnAtPoint(point);
      Breakpoint breakpoint = getBreakPoint(point);
      if (column == 4 && breakpoint != null && supportsMoreConfig(breakpoint)) {
        if (table.getCursor() != HAND_CURSOR) {
          table.setCursor(HAND_CURSOR);
        }
        return;
      }
      if (table.getCursor() != DEFAULT_CURSOR) {
        table.setCursor(DEFAULT_CURSOR);
      }
    }
  }

  @VisibleForTesting
  class ModelSetter implements Runnable {

    private final List<Breakpoint> breakpointList;
    private final int finalSelection;

    ModelSetter(List<Breakpoint> breakpointList, int finalSelection) {
      this.breakpointList = breakpointList;
      this.finalSelection = finalSelection;
    }

    @Override
    public void run() {
      // todo: why are we switching out the model instead of simply updating the old model?
      // todo: a lot of this code might be pushed into CloudDebuggerTable.setBrekpoints or
      // equivalent
      SnapshotsModel oldModel = getModel();
      SnapshotsModel newModel =
          new SnapshotsModel(CloudDebugHistoricalSnapshots.this, breakpointList, oldModel);
      table.setModel(newModel);
      if (finalSelection != -1) {
        table.setRowSelectionInterval(finalSelection, finalSelection);
      }
      resizeColumnWidth();
      int rowForPopup = -1;
      for (int row = 0; row < getModel().getRowCount(); row++) {
        // todo: getModel should be newModel
        Breakpoint bp = getModel().getBreakpoints().get(row);
        if (Boolean.FALSE.equals(bp.getIsFinalState())) {
          continue;
        }
        StatusMessage status = bp.getStatus();
        if (status != null && Boolean.TRUE.equals(status.getIsError())) {
          continue;
        }
        String id = bp.getId();
        // todo: getModel should be newModel
        boolean newModelNewlyReceived = getModel().isNewlyReceived(id);
        boolean oldModelNewlyReceived = oldModel.isNewlyReceived(id);
        if (newModelNewlyReceived && !oldModelNewlyReceived) {
          rowForPopup = row;
        }
        break; // NOPMD
      }
      if (rowForPopup != -1) {
        UsageTrackerProvider.getInstance()
            .trackEvent(GctTracking.CLOUD_DEBUGGER_SNAPSHOT_RECEIVED)
            .ping();
        // Show a popup indicating a new item has appeared.
        if (balloon != null) {
          balloon.hide();
        }
        Rectangle rectangle = table.getCellRect(rowForPopup, 0, true);
        BalloonBuilder builder =
            JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(
                    GctBundle.getString("clouddebug.new.snapshot.received"), MessageType.INFO, null)
                .setFadeoutTime(3000)
                .setDisposable(process.getXDebugSession().getProject());
        balloon = builder.createBalloon();
        balloon.show(
            new RelativePoint(table, new Point(table.getWidth() / 2, rectangle.y)), Position.above);

        reloadSnapshot();
      } else if (oldModel.hasPendingDeletes()
          && oldModel.getBreakpoints().size() > newModel.getBreakpoints().size()) {
        process.clearExecutionStack();
      }
    }

    /**
     * If the snapshot was already selected prior to being triggered, e.g. the user selected it
     * while in a pending state, we need to force trigger its selection so that the new results are
     * drawn
     */
    private void reloadSnapshot() {
      int selectedRow = table.getSelectedRow();

      if (selectedRow != -1
          && selectedRow < getModel().getBreakpoints().size()
          && getModel().isNewlyReceived(selectedRow)) {
        Breakpoint breakpoint = getModel().getBreakpoints().get(selectedRow);

        if (breakpoint != null && table.getSelectedRows().length == 1) {
          selectSnapshot(breakpoint, true);
        }
      }
    }
  }
}
