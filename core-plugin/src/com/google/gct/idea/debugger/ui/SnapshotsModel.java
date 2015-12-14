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

import com.google.api.services.clouddebugger.model.Breakpoint;
import com.google.gct.idea.debugger.BreakpointUtil;
import com.google.gct.idea.ui.GoogleCloudToolsIcons;
import com.google.gct.idea.util.GctBundle;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

/**
 * Swing TableModel for a list of cloud debugger snapshots. Each table row represents a single
 * snapshot. There are five columns in the table:
 *
 * 0. An icon indicating the state of the breakpoint: error, checked, or final
 * 1. A date-time for received snapshots or the word "Pending" otherwise.
 * 2. The file and line number of the snapshot; e.g. "GeneratorServlet.java:40"
 * 3. The breakpoint condition, if any
 * 4. For pending snapshots only, the word "More" which is a link to the Breakpoints dialog.
 */
class SnapshotsModel extends AbstractTableModel {

  private static final int ourColumnCount = 5;

  private final List<Breakpoint> myBreakpoints;
  private final Set<String> myPendingDeletes = new HashSet<String>();
  private final Set<String> myNewlyReceived = new HashSet<String>();
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
          if (newBreakpoint.getIsFinalState() != Boolean.TRUE) {
            continue;
          }
          if (tempHashMap.containsKey(newBreakpoint.getId())) {
            Breakpoint previousBreakpoint = tempHashMap.get(newBreakpoint.getId());
            if (previousBreakpoint.getIsFinalState() == Boolean.TRUE) {
              if (!oldModel.isNewlyReceived(previousBreakpoint.getId())) {
                continue;
              }
            }
          }
          myNewlyReceived.add(newBreakpoint.getId());
        }
      }
    }

    myBreakpoints = breakpoints != null ? breakpoints : new ArrayList<Breakpoint>();
    this.snapshots = snapshots;
  }

  @NotNull
  // todo: seem to be exposing mutable private stat here, though not too broadly so it may be OK
  List<Breakpoint> getBreakpoints() {
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

  void markForDelete(String id) {
    myPendingDeletes.add(id);
  }

  void unMarkAsNewlyReceived(String id) {
    myNewlyReceived.remove(id);
  }

  boolean isMarkedForDelete(int row) {
    Breakpoint breakpoint = null;
    if (row >= 0 && row < myBreakpoints.size()) {
      breakpoint = myBreakpoints.get(row);
    }
    return breakpoint != null && myPendingDeletes.contains(breakpoint.getId());
  }

  boolean isNewlyReceived(String id) {
    return myNewlyReceived.contains(id);
  }

  boolean isNewlyReceived(int row) {
    Breakpoint breakpoint = null;
    if (row >= 0 && row < myBreakpoints.size()) {
      breakpoint = myBreakpoints.get(row);
    }
    return breakpoint != null && isNewlyReceived(breakpoint.getId());
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
        return BreakpointUtil.parseDateTime(breakpoint.getFinalTime());
      case 2:
        String path = breakpoint.getLocation().getPath();
        int startIndex = path.lastIndexOf('/');
        return path.substring(startIndex >= 0 ? startIndex + 1 : 0) +
            ":" +
            breakpoint.getLocation().getLine().toString();
      case 3:
        return breakpoint.getCondition();
      case 4:
        if (snapshots.supportsMoreConfig(breakpoint)) {
          return GctBundle.getString("clouddebug.moreHTML");
        }
    }
    return null;
  }
}
