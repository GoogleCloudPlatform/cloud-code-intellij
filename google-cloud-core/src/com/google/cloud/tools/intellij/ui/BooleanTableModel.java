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

package com.google.cloud.tools.intellij.ui;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The {@link TableModel} for a 2 column headerless table where the first column is immutable and
 * the second column in a boolean.
 *
 * @param <T> the type of the item in the first column.
 */
public class BooleanTableModel<T> extends AbstractTableModel {

  private final SortedMap<T, Boolean> map;
  private final int VALUE_COL = 0;
  private final int BOOLEAN_COL = 1;
  private final Class<T> type;

  public BooleanTableModel(
      @NotNull List<T> items,
      Class<T> type,
      @Nullable Comparator comparator,
      boolean initialConfiguration) {
    map = new TreeMap<>(comparator);
    map.putAll(Maps.toMap(items, item -> initialConfiguration));
    this.type = type;
  }

  /** Returns the set of selected items. */
  public Set<T> getSelectedItems() {
    return map.entrySet()
        .stream()
        .filter(Entry::getValue)
        .map(Entry::getKey)
        .collect(ImmutableSet.toImmutableSet());
  }

  public int getBooleanColumn() {
    return BOOLEAN_COL;
  }

  @Override
  public int getRowCount() {
    return map.size();
  }

  @Override
  public int getColumnCount() {
    return 2;
  }

  @Override
  public String getColumnName(int columnIndex) {
    return null;
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    if (columnIndex == VALUE_COL) {
      return type;
    }
    return Boolean.class;
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return columnIndex == BOOLEAN_COL;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    if (columnIndex == VALUE_COL) {
      return map.keySet().toArray()[rowIndex];
    }
    return map.values().toArray()[rowIndex];
  }

  @Override
  public void setValueAt(Object value, int rowIndex, int columnIndex) {
    if (columnIndex == VALUE_COL) {
      throw new UnsupportedOperationException("The first column is immutable.");
    }

    T key = (T) map.keySet().toArray()[rowIndex];
    map.put(key, (Boolean) value);
    fireTableCellUpdated(rowIndex, columnIndex);
  }
}
