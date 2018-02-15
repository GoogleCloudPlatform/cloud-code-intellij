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

package com.google.cloud.tools.intellij.project;

import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.cloud.tools.intellij.GoogleCloudCoreMessageBundle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** Model for project list table in {@link ProjectSelectionDialog}. */
class ProjectListTableModel extends AbstractTableModel {

  private static final int PROJECT_NAME_COLUMN = 0;
  private static final int PROJECT_ID_COLUMN = 1;

  private final List<Project> projectList = new ArrayList<>();

  @Override
  public int getRowCount() {
    return projectList.size();
  }

  @Override
  public int getColumnCount() {
    return 2;
  }

  @Override
  public Object getValueAt(int row, int column) {
    switch (column) {
      case PROJECT_NAME_COLUMN:
        return projectList.get(row).getName();
      case PROJECT_ID_COLUMN:
        return projectList.get(row).getProjectId();
      default:
        return "";
    }
  }

  @Override
  public String getColumnName(int column) {
    switch (column) {
      case PROJECT_NAME_COLUMN:
        return GoogleCloudCoreMessageBundle.getString(
            "cloud.project.selector.project.list.project.name.column");
      case PROJECT_ID_COLUMN:
        return GoogleCloudCoreMessageBundle.getString(
            "cloud.project.selector.project.list.project.id.column");
      default:
        return "";
    }
  }

  String getProjectNameAtRow(int row) {
    return getValueAt(row, ProjectListTableModel.PROJECT_NAME_COLUMN).toString();
  }

  String getProjectIdAtRow(int row) {
    return getValueAt(row, ProjectListTableModel.PROJECT_ID_COLUMN).toString();
  }

  Long getProjectNumberAtRow(int row) {
    return projectList.get(row).getProjectNumber();
  }

  void setProjectList(List<Project> updatedList) {
    projectList.clear();
    projectList.addAll(updatedList);
    fireTableDataChanged();
  }
}
