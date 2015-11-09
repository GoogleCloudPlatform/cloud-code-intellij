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
package com.google.gct.idea.debugger;

import com.google.gct.idea.debugger.actions.ToggleSnapshotLocationAction;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stores process state to workspace.xml. This allows us to continue watching the
 * process after a restart.
 */
@State(
  name = "CloudDebugProcessStateSerializer",
  storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
public class CloudDebugProcessStateSerializer
   implements PersistentStateComponent<CloudDebugProcessStateSerializer.ProjectState> {
  private final Project myProject;
  private final Map<String, CloudDebugProcessState> myStateMap = new HashMap<String, CloudDebugProcessState>();

  private CloudDebugProcessStateSerializer(@NotNull Project project) {
    myProject = project;

    if (CloudDebugConfigType.isFeatureEnabled()) {
      // We listen on mouse events to calculate the line where we should add a cloud breakpoint
      // in our right click menu action.
      EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
        private final Map<Editor, TargetLineMouseAdapter> myMouseAdapterMap =
          new HashMap<Editor, TargetLineMouseAdapter>();

        @Override
        public void editorCreated(@NotNull EditorFactoryEvent event) {
          if (event.getEditor().getProject() == myProject && event.getEditor().getGutter() instanceof Component) {
            Component gutterComponent = (Component)event.getEditor().getGutter();
            TargetLineMouseAdapter adapter = new TargetLineMouseAdapter(event.getEditor());
            assert !myMouseAdapterMap.containsKey(event.getEditor());
            myMouseAdapterMap.put(event.getEditor(), adapter);
            gutterComponent.addMouseListener(adapter);
          }
        }

        @Override
        public void editorReleased(@NotNull EditorFactoryEvent event) {
          TargetLineMouseAdapter adapter = myMouseAdapterMap.get(event.getEditor());
          if (adapter != null && event.getEditor().getGutter() instanceof Component) {
            Component gutterComponent = (Component)event.getEditor().getGutter();
            gutterComponent.removeMouseListener(adapter);
            myMouseAdapterMap.remove(event.getEditor());
          }
        }
      }, myProject);
    }
  }

  public static CloudDebugProcessStateSerializer getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, CloudDebugProcessStateSerializer.class);
  }

  /**
   * Enumerates all runconfigurations in this project and returns a serialized form of their process states.
   *
   * @return a serialized form of the debuggee state as known to the client
   */
  @Nullable
  @Override
  public CloudDebugProcessStateSerializer.ProjectState getState() {
    ProjectState projectState = new ProjectState();
    if (CloudDebugConfigType.isFeatureEnabled()) {
      RunManager manager = RunManager.getInstance(myProject);
      for (RunnerAndConfigurationSettings config : manager.getAllSettings()) {
        if (config.getConfiguration() == null) {
          continue;
        }

        if (config.getConfiguration() instanceof CloudDebugRunConfiguration) {
          final CloudDebugRunConfiguration cloudConfig = (CloudDebugRunConfiguration)config.getConfiguration();
          final CloudDebugProcessState state = cloudConfig.getProcessState();
          if (state != null) {
            projectState.CONFIG_STATES.add(new RunConfigState(cloudConfig.getName(), state));
          }
        }
      }
    }

    return projectState;
  }

  /**
   * Called from {@link com.google.gct.idea.debugger.CloudDebugRunConfiguration}, it finds any serialized state for that
   * named run config and if the project name matches, it initializes and returns it.
   *
   * @param runConfig   the runconfig for which state is queried for
   * @param projectName the GCP project name associated with this runconfig
   * @return deserialized state that may have been cached in workspace.xml
   */
  public CloudDebugProcessState getState(@NotNull String runConfig, @NotNull String projectName) {
    CloudDebugProcessState state = myStateMap.get(runConfig);
    if (state != null &&
        state.getProjectName() != null &&
        state.getProjectName().equals(projectName)) {
      state.setProject(myProject);
      return state;
    }
    return null;
  }

  /**
   * Loads Cloud Debugger process states so that the background job can continue to monitor them.
   *
   * @param state loaded component state
   */
  @Override
  public void loadState(CloudDebugProcessStateSerializer.ProjectState state) {
    if (CloudDebugConfigType.isFeatureEnabled() && state.CONFIG_STATES != null) {
      for (RunConfigState configState : state.CONFIG_STATES) {
        myStateMap.put(configState.CONFIG_NAME, configState.PROCESS_STATE);
      }
    }
  }

  static class TargetLineMouseAdapter extends MouseAdapter {
    private final Editor myEditor;

    public TargetLineMouseAdapter(Editor editor) {
      myEditor = editor;
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if (e.isPopupTrigger()) {
        // We should see if we can get JB to make this information public from the Gutter so we
        // don't have to calculate it.
        myEditor.putUserData(ToggleSnapshotLocationAction.POPUP_LINE,
                             new Integer(EditorUtil.yPositionToLogicalLine(myEditor, e.getPoint())));
      }
      else {
        myEditor.putUserData(ToggleSnapshotLocationAction.POPUP_LINE, null);
      }
    }
  }

  public static class ProjectState {
    //For serialization purposes, this cannot be final.
    public List<RunConfigState> CONFIG_STATES = new ArrayList<RunConfigState>();

    public ProjectState() {
    }
  }

  public static class RunConfigState {
    public String CONFIG_NAME;
    public CloudDebugProcessState PROCESS_STATE;

    /**
     * This is used during deserialization.
     */
    public RunConfigState() {
    }

    public RunConfigState(String configName, CloudDebugProcessState state) {
      CONFIG_NAME = configName;
      PROCESS_STATE = state;
    }
  }
}
