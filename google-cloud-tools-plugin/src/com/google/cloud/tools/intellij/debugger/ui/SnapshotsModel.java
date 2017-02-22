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

import com.google.api.services.clouddebugger.v2.model.Breakpoint;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;

import org.jetbrains.annotations.NotNull;
import org.joda.time.format.ISODateTimeFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

/**
 * Swing TableModel for a list of cloud debugger snapshots. Each table row represents a single
 * snapshot. There are five columns in the table:
 * <p/>
 * 0. An icon indicating the state of the breakpoint: error, checked, or final 1. A date-time for
 * received snapshots or the word "Pending" otherwise. 2. The file and line number of the snapshot;
 * e.g. "GeneratorServlet.java:40" 3. The breakpoint condition, if any 4. For pending snapshots
 * only, the word "More" which is a link to the Breakpoints dialog.
 */
class SnapshotsModel extends AbstractTableModel {

  private static final int COLUMN_COUNT = 5;

  private final List<Breakpoint> breakpoints;
  private final Set<String> pendingDeletes = new HashSet<String>();
  private final Set<String> newlyReceived = new HashSet<String>();
  private CloudDebugHistoricalSnapshots snapshots;

  SnapshotsModel(CloudDebugHistoricalSnapshots snapshots,
      List<Breakpoint> breakpoints, SnapshotsModel oldModel) {
    HashMap<String, Breakpoint> tempHashMap = new HashMap<String, Breakpoint>();
    if (oldModel != null && oldModel.getBreakpoints().size() > 0) {
      for (Breakpoint previousBreakpoint : oldModel.getBreakpoints()) {
        tempHashMap.put(previousBreakpoint.getId(), previousBreakpoint);
      }

      if (breakpoints != null) {
        for (Breakpoint newBreakpoint : breakpoints) {
          // We loop through new breakpoints.
          // If a new breakpoint is in final state *and*
          // the old model didn't know about that breakpoint as being final (and not new)
          // then we mark it.
          if (!Boolean.TRUE.equals(newBreakpoint.getIsFinalState())) {
            continue;
          }
          if (tempHashMap.containsKey(newBreakpoint.getId())) {
            Breakpoint previousBreakpoint = tempHashMap.get(newBreakpoint.getId());
            if (Boolean.TRUE.equals(previousBreakpoint.getIsFinalState())) {
              if (!oldModel.isNewlyReceived(previousBreakpoint.getId())) {
                continue;
              }
            }
          }
          newlyReceived.add(newBreakpoint.getId());
        }
      }
    }

    this.breakpoints = breakpoints != null ? breakpoints : new ArrayList<Breakpoint>();
    this.snapshots = snapshots;
  }

  // todo: seem to be exposing mutable private stat here, though not too broadly so it may be OK
  @NotNull
  List<Breakpoint> getBreakpoints() {
    return breakpoints;
  }

  @Override
  public int getColumnCount() {
    return COLUMN_COUNT;
  }

  @Override
  public int getRowCount() {
    return breakpoints.size();
  }

  void markForDelete(String id) {
    pendingDeletes.add(id);
  }

  void unMarkAsNewlyReceived(String id) {
    newlyReceived.remove(id);
  }

  boolean isMarkedForDelete(int row) {
    Breakpoint breakpoint = null;
    if (row >= 0 && row < breakpoints.size()) {
      breakpoint = breakpoints.get(row);
    }
    return breakpoint != null && pendingDeletes.contains(breakpoint.getId());
  }

  boolean hasPendingDeletes() {
    return !pendingDeletes.isEmpty();
  }

  boolean isNewlyReceived(String id) {
    return newlyReceived.contains(id);
  }

  boolean isNewlyReceived(int row) {
    Breakpoint breakpoint = null;
    if (row >= 0 && row < breakpoints.size()) {
      breakpoint = breakpoints.get(row);
    }
    return breakpoint != null && isNewlyReceived(breakpoint.getId());
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    if (rowIndex < 0 || rowIndex >= breakpoints.size()) {
      return null;
    }
    Breakpoint breakpoint = breakpoints.get(rowIndex);

    switch (columnIndex) {
      case 0:
        if (breakpoint.getStatus() != null && Boolean.TRUE
            .equals(breakpoint.getStatus().getIsError())) {
          return GoogleCloudToolsIcons.CLOUD_BREAKPOINT_ERROR;
        }
        if (!Boolean.TRUE.equals(breakpoint.getIsFinalState())) {
          return GoogleCloudToolsIcons.CLOUD_BREAKPOINT_CHECKED;
        }
        return GoogleCloudToolsIcons.CLOUD_BREAKPOINT_FINAL;
      case 1:
        if (!Boolean.TRUE.equals(breakpoint.getIsFinalState())) {
          return GctBundle.getString("clouddebug.pendingstatus");
        }
        try {
          return ISODateTimeFormat.dateTime().parseDateTime(breakpoint.getFinalTime()).toDate();
        } catch (IllegalArgumentException iae) {
          return new Date();
        }
      case 2:
        String path = breakpoint.getLocation().getPath();
        int startIndex = path.lastIndexOf('/');
        return path.substring(startIndex >= 0 ? startIndex + 1 : 0)
            + ":"
            + breakpoint.getLocation().getLine().toString();
      case 3:
        return breakpoint.getCondition();
      case 4:
        if (snapshots.supportsMoreConfig(breakpoint)) {
          return GctBundle.getString("clouddebug.moreHTML");
        } else {
          return null;
        }
      default: return null;
    }
  }
}
