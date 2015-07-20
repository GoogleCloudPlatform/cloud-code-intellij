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

import com.google.api.services.debugger.model.Breakpoint;
import com.google.gct.idea.debugger.*;
import com.google.gct.idea.util.GctBundle;
import com.intellij.diagnostic.logging.AdditionalTabComponent;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.UI;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.impl.breakpoints.ui.BreakpointsDialogFactory;
import icons.GoogleCloudToolsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.text.DateFormat;
import java.util.*;
import java.util.List;

/**
 * This panel shows a list of historical and pending snapshots. The user can navigate to them by double clicking on
 * them, which synchronizes the debugger state to that snapshot.
 */
public class CloudDebugHistoricalSnapshots extends AdditionalTabComponent
  implements XDebugSessionListener, CloudBreakpointListener {

  private static final int COLUMN_MARGIN_PX = 3;
  private static final Cursor DEFAULT_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
  private static final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
  private static final int WINDOW_HEIGHT_PX = 8 * JBTable.PREFERRED_SCROLLABLE_VIEWPORT_HEIGHT_IN_ROWS;
  private static final int WINDOW_WIDTH_PX = 200;
  private final JBTable myTable;
  private CloudDebugProcess myProcess;

  public CloudDebugHistoricalSnapshots(@NotNull CloudDebugProcessHandler processHandler) {
    super(new BorderLayout());

    myTable = new JBTable() {
      //  Returning the Class of each column will allow different
      //  renderers to be used based on Class
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
        Component c = super.prepareRenderer(renderer, row, column);
        if (c instanceof JComponent) {
          JComponent jc = (JComponent)c;
          Breakpoint breakpoint = CloudDebugHistoricalSnapshots.this.getModel().getBreakpoints().get(row);
          jc.setToolTipText(BreakpointUtil.getUserErrorMessage(breakpoint.getStatus()));
        }
        return c;
      }
    };

    myTable.setModel(new MyModel(null));

    myTable.setTableHeader(null);
    myTable.setShowGrid(false);
    myTable.setRowMargin(0);
    myTable.getColumnModel().setColumnMargin(0);
    myTable.getColumnModel().getColumn(1).setCellRenderer(new SnapshotTimeCellRenderer());
    myTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultRenderer());
    myTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultRenderer());
    myTable.getColumnModel().getColumn(4).setCellRenderer(new MoreCellRenderer());
    myTable.resetDefaultFocusTraversalKeys();
    myTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    myTable.setPreferredScrollableViewportSize(new Dimension(WINDOW_WIDTH_PX, WINDOW_HEIGHT_PX));
    myTable.setAutoCreateColumnsFromModel(false);
    myTable.getEmptyText().setText(GctBundle.getString("clouddebug.nosnapshots"));

    final ToolbarDecorator decorator =
      ToolbarDecorator.createDecorator(myTable).disableUpDownActions().disableAddAction();

    decorator.setToolbarPosition(ActionToolbarPosition.TOP);
    decorator.setRemoveAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {
        fireDeleteBreakpoints(getSelectedBreakpoints());
      }
    });

    decorator.addExtraAction(new AnActionButton(GctBundle.getString("clouddebug.delete.all"),
                                                GoogleCloudToolsIcons.CLOUD_DEBUG_DELETE_ALL_BREAKPOINTS) {
      @Override
      public void actionPerformed(AnActionEvent e) {
        if (Messages.showDialog(GctBundle.getString("clouddebug.remove.all"),
                                GctBundle.getString("clouddebug.delete.snapshots"),
                                new String[]{GctBundle.getString("clouddebug.buttondelete"),
                                  GctBundle.getString("clouddebug.cancelbutton")}, 1, Messages.getQuestionIcon()) ==
            0) {
          MyModel model = (MyModel)myTable.getModel();
          fireDeleteBreakpoints(model.getBreakpoints());
        }
      }
    });

    decorator.addExtraAction(new AnActionButton(GctBundle.getString("clouddebug.reactivatesnapshotlocation"),
                                                GoogleCloudToolsIcons.CLOUD_DEBUG_REACTIVATE_BREAKPOINT) {
                               @Override
                               public void actionPerformed(AnActionEvent e) {
                                 myProcess.getBreakpointHandler().cloneToNewBreakpoints(getSelectedBreakpoints());
                               }
                             });

    this.add(decorator.createPanel());
    myProcess = processHandler.getProcess();
    onBreakpointsChanged();

    myProcess.getXDebugSession().addSessionListener(this);
    myProcess.addListener(this);

    // This is the  click handler that does one of three things:
    // 1. Single click on a final snapshot will load the debugger with that snapshot
    // 2. Single click on a pending snapshot will show the line of code
    // 3. Single click on "More" will show the breakpoint config dialog.
    myTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent me) {
        JTable table = (JTable)me.getSource();
        Point p = me.getPoint();
        Breakpoint breakpoint = getBreakPoint(p);
        int col = table.columnAtPoint(p);
        if (breakpoint != null && col == 4 && supportsMoreConfig(breakpoint)) {
          BreakpointsDialogFactory.getInstance(myProcess.getXDebugSession().getProject())
            .showDialog(myProcess.getBreakpointHandler().getXBreakpoint(breakpoint));
        }
        else if (me.getClickCount() == 1 && breakpoint != null && myTable.getSelectedRows().length == 1) {
          myProcess.navigateToSnapshot(breakpoint.getId());
        }
      }
    });

    // we use a motion listener to create a hand cursor over a link within a table.
    myTable.addMouseMotionListener(new MouseMotionListener() {
      @Override
      public void mouseDragged(MouseEvent me) {
      }

      @Override
      public void mouseMoved(MouseEvent me) {
        JTable table = (JTable)me.getSource();
        Point p = me.getPoint();
        int column = table.columnAtPoint(p);
        Breakpoint breakpoint = getBreakPoint(p);
        if (column == 4 && breakpoint != null && supportsMoreConfig(breakpoint)) {
          if (myTable.getCursor() != HAND_CURSOR) {
            myTable.setCursor(HAND_CURSOR);
          }
          return;
        }
        if (myTable.getCursor() != DEFAULT_CURSOR) {
          myTable.setCursor(DEFAULT_CURSOR);
        }
      }
    });
  }

  @Override
  public void beforeSessionResume() {
  }

  @Override
  public void dispose() {
  }

  @Override
  public JComponent getPreferredFocusableComponent() {
    return myTable;
  }

  @Nullable
  @Override
  public JComponent getSearchComponent() {
    return null;
  }

  @NotNull
  @Override
  public String getTabTitle() {
    return "Cloud Debugger Snapshots";
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
    onBreakpointsChanged();
  }

  @Override
  public void sessionPaused() {
    onBreakpointsChanged();
  }

  @Override
  public void sessionResumed() {
  }

  @Override
  public void sessionStopped() {
    myProcess.removeListener(this);

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        myTable.setModel(new MyModel(null));
      }
    });
  }

  @Override
  public void stackFrameChanged() {
  }

  /**
   * Deletes breakpoints on a threadpool thread.  The user will see these breakpoints gradually disappear
   */
  private void fireDeleteBreakpoints(@NotNull final List<Breakpoint> breakpointsToDelete) {
    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        for (Breakpoint aBreakpointsToDelete : breakpointsToDelete) {
          myProcess.getBreakpointHandler().deleteBreakpoint(aBreakpointsToDelete);
        }
      }
    });
  }

  @Nullable
  private Breakpoint getBreakPoint(@NotNull Point p) {
    int row = myTable.rowAtPoint(p);
    if (row >= 0 && row < getModel().getBreakpoints().size()) {
      return getModel().getBreakpoints().get(row);
    }
    return null;
  }

  @NotNull
  private MyModel getModel() {
    return (MyModel)myTable.getModel();
  }

  /**
   * Used by delete and clone, this returns which lines the user currently has selected in the snapshot list.
   */
  @NotNull
  private List<Breakpoint> getSelectedBreakpoints() {
    List<Breakpoint> selectedBreakpoints = new ArrayList<Breakpoint>();
    MyModel model = (MyModel)myTable.getModel();
    int[] selectedRows = myTable.getSelectedRows();
    for (int selectedRow : selectedRows) {
      selectedBreakpoints.add(model.getBreakpoints().get(selectedRow));
    }
    return selectedBreakpoints;
  }

  /**
   * This is fired when the set of breakpoints from the server changes. We create a new table model while keeping the
   * selection as it was. Most routines on selection are therefore based on the breakpoint Id and not a reference to a
   * breakpoint object -- because we never know when the server instance will get replaced.
   */
  private void onBreakpointsChanged() {
    // Read the list of bps and show them.
    // We always snap the current breakpoint list into a local to eliminate threading issues.
    final List<Breakpoint> breakpointList = myProcess.getCurrentBreakpointList();
    int selection = -1;

    if (breakpointList != null) {
      for (int i = 0; i < breakpointList.size(); i++) {
        if (breakpointList.get(i).getFinalTime() == null) continue;
        if (myProcess.getCurrentSnapshot() != null &&
            breakpointList.get(i).getId().equals(myProcess.getCurrentSnapshot().getId())) {
          selection = i;
          break;
        }
      }
    }

    final int finalSelection = selection;

    // Setting the model must happen on the UI thread, while most of this method executes on the
    // background.
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        myTable.setModel(new MyModel(breakpointList));
        if (finalSelection != -1) {
          myTable.setRowSelectionInterval(finalSelection, finalSelection);
        }
        resizeColumnWidth();
      }
    });
  }

  /**
   * Re-sizes the table to respect the contents of each column.
   */
  private void resizeColumnWidth() {
    final TableColumnModel columnModel = myTable.getColumnModel();
    for (int column = 0; column < myTable.getColumnCount(); column++) {
      int width = 2; // Min width
      for (int row = 0; row < myTable.getRowCount(); row++) {
        TableCellRenderer renderer = myTable.getCellRenderer(row, column);
        Component comp = myTable.prepareRenderer(renderer, row, column);
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
   * Returns true if we have a local representation of the snapshot. The snapshot may be pending or in final state.  If
   * in final state, then the local representation will be disabled (not enabled).  The user can re-enable the local
   * state and it will create a new pending snapshot and de-link the old snapshot from the local representation.
   */
  private boolean supportsMoreConfig(@Nullable Breakpoint breakpoint) {
    return myProcess.getBreakpointHandler().getXBreakpoint(breakpoint) != null;
  }

  final static class SnapshotTimeCellRenderer extends DefaultTableCellRenderer {
    private static final DateFormat ourDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    private static final DateFormat ourDateFormatToday = DateFormat.getTimeInstance(DateFormat.SHORT);
    private final Date myTodayDate;

    public SnapshotTimeCellRenderer() {
      setHorizontalAlignment(SwingConstants.LEFT);
      Calendar c = new GregorianCalendar();
      c.set(Calendar.HOUR_OF_DAY, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      myTodayDate = c.getTime();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      setBorder(noFocusBorder);

      boolean isToday = false;
      if (value instanceof Date) {
        Date finalDate = (Date)value;
        if (finalDate.after(myTodayDate)) {
          setText(ourDateFormatToday.format(finalDate));
          isToday = true;
        }
        else {
          setText(ourDateFormat.format(finalDate));
        }
      }
      else {
        setText(value.toString());
      }

      if (GctBundle.getString("clouddebug.pendingstatus").equals(value)) {
        Font font = getFont();
        Font boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
        setFont(boldFont);
        if (!isSelected) {
          setForeground(UIUtil.getActiveTextColor());
        }
      }
      else {
        Font font = getFont();
        Font boldFont = new Font(font.getFontName(), Font.PLAIN, font.getSize());
        setFont(boldFont);
        if (!isSelected) {
          setForeground(isToday ? UIUtil.getActiveTextColor() : UIUtil.getInactiveTextColor());
        }
      }

      return this;
    }
  }

  final static class MoreCellRenderer extends DefaultTableCellRenderer {
    public MoreCellRenderer() {
      setHorizontalAlignment(SwingConstants.LEFT);
      setForeground(UI.getColor("link.foreground"));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      setBorder(noFocusBorder);
      if (value != null) {
        setText(value.toString());
      }
      return this;
    }
  }

  private static class DefaultRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      setBorder(noFocusBorder);
      return this;
    }
  }

  private class MyModel extends AbstractTableModel {

    private static final int ourColumnCount = 5;
    private final List<Breakpoint> myBreakpoints;

    public MyModel(List<Breakpoint> breakpoints) {
      myBreakpoints = breakpoints != null ? breakpoints : new ArrayList<Breakpoint>();
    }

    @NotNull
    public List<Breakpoint> getBreakpoints() {
      return myBreakpoints;
    }

    @Override
    public int getColumnCount() {
      return ourColumnCount;
    }

    @Override
    public int getRowCount() {
      return myBreakpoints.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      if (rowIndex < 0 || rowIndex >= myBreakpoints.size()) {
        return null;
      }
      Breakpoint breakpoint = myBreakpoints.get(rowIndex);

      switch (columnIndex) {
        case 0:
          if (breakpoint.getStatus() != null && breakpoint.getStatus().getIsError() == Boolean.TRUE) {
            return GoogleCloudToolsIcons.CLOUD_BREAKPOINT_ERROR;
          }
          if (breakpoint.getIsFinalState() != Boolean.TRUE) {
            return GoogleCloudToolsIcons.CLOUD_BREAKPOINT_CHECKED;
          }
          return GoogleCloudToolsIcons.CLOUD_BREAKPOINT_FINAL;
        case 1:
          if (breakpoint.getIsFinalState() != Boolean.TRUE) {
            return GctBundle.getString("clouddebug.pendingstatus");
          }
          return new Date(breakpoint.getFinalTime().getSeconds() * 1000);
        case 2:
          String path = breakpoint.getLocation().getPath();
          int startIndex = path.lastIndexOf('/');
          return path.substring(startIndex >= 0 ? startIndex + 1 : 0) +
                 ":" +
                 breakpoint.getLocation().getLine().toString();
        case 3:
          return breakpoint.getCondition();
        case 4:
          if (supportsMoreConfig(breakpoint)) {
            return GctBundle.getString("clouddebug.moreHTML");
          }
      }
      return null;
    }
  }
}
