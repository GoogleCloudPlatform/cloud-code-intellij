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

package com.google.cloud.tools.intellij.stackdriver.debugger;

import com.google.cloud.tools.intellij.stackdriver.debugger.actions.ToggleSnapshotLocationAction;
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
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores process state to workspace.xml. This allows us to continue watching the process after a
 * restart.
 */
@State(
  name = "CloudDebugProcessStateSerializer",
  storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)}
)
public class CloudDebugProcessStateSerializer
    implements PersistentStateComponent<CloudDebugProcessStateSerializer.ProjectState> {

  private final Project project;
  private final Map<String, CloudDebugProcessState> stateMap =
      new HashMap<String, CloudDebugProcessState>();

  private CloudDebugProcessStateSerializer(@NotNull Project project) {
    this.project = project;

    if (CloudDebugConfigType.isFeatureEnabled()) {
      // We listen on mouse events to calculate the line where we should add a cloud breakpoint
      // in our right click menu action.
      EditorFactory.getInstance()
          .addEditorFactoryListener(
              new EditorFactoryListener() {
                private final Map<Editor, TargetLineMouseAdapter> mouseAdapterMap =
                    new HashMap<Editor, TargetLineMouseAdapter>();

                @Override
                public void editorCreated(@NotNull EditorFactoryEvent event) {
                  if (event.getEditor().getProject()
                          == CloudDebugProcessStateSerializer.this.project
                      && event.getEditor().getGutter() instanceof Component) {
                    Component gutterComponent = (Component) event.getEditor().getGutter();
                    TargetLineMouseAdapter adapter = new TargetLineMouseAdapter(event.getEditor());
                    assert !mouseAdapterMap.containsKey(event.getEditor());
                    mouseAdapterMap.put(event.getEditor(), adapter);
                    gutterComponent.addMouseListener(adapter);
                  }
                }

                @Override
                public void editorReleased(@NotNull EditorFactoryEvent event) {
                  TargetLineMouseAdapter adapter = mouseAdapterMap.get(event.getEditor());
                  if (adapter != null && event.getEditor().getGutter() instanceof Component) {
                    Component gutterComponent = (Component) event.getEditor().getGutter();
                    gutterComponent.removeMouseListener(adapter);
                    mouseAdapterMap.remove(event.getEditor());
                  }
                }
              },
              project);
    }
  }

  public static CloudDebugProcessStateSerializer getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, CloudDebugProcessStateSerializer.class);
  }

  /**
   * Enumerates all runconfigurations in this project and returns a serialized form of their process
   * states.
   *
   * @return a serialized form of the debuggee state as known to the client
   */
  @Nullable
  @Override
  public CloudDebugProcessStateSerializer.ProjectState getState() {
    ProjectState projectState = new ProjectState();
    if (CloudDebugConfigType.isFeatureEnabled()) {
      RunManager manager = RunManager.getInstance(project);
      for (RunnerAndConfigurationSettings config : manager.getAllSettings()) {
        if (config.getConfiguration() == null) {
          continue;
        }

        if (config.getConfiguration() instanceof CloudDebugRunConfiguration) {
          final CloudDebugRunConfiguration cloudConfig =
              (CloudDebugRunConfiguration) config.getConfiguration();
          final CloudDebugProcessState state = cloudConfig.getProcessState();
          if (state != null) {
            projectState.configStates.add(new RunConfigState(cloudConfig.getName(), state));
          }
        }
      }
    }

    return projectState;
  }

  /**
   * Called from {@link CloudDebugRunConfiguration}, it finds any serialized state for that named
   * run config and if the project name matches, it initializes and returns it.
   *
   * @param runConfig the runconfig for which state is queried for
   * @param projectName the GCP project name associated with this runconfig
   * @return deserialized state that may have been cached in workspace.xml
   */
  public CloudDebugProcessState getState(@NotNull String runConfig, @NotNull String projectName) {
    CloudDebugProcessState state = stateMap.get(runConfig);
    if (state != null
        && state.getProjectName() != null
        && state.getProjectName().equals(projectName)) {
      state.setProject(project);
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
    if (CloudDebugConfigType.isFeatureEnabled() && state.configStates != null) {
      for (RunConfigState configState : state.configStates) {
        stateMap.put(configState.configName, configState.processState);
      }
    }
  }

  static class TargetLineMouseAdapter extends MouseAdapter {

    private final Editor editor;

    public TargetLineMouseAdapter(Editor editor) {
      this.editor = editor;
    }

    @Override
    public void mousePressed(MouseEvent event) {
      if (event.isPopupTrigger()) {
        // We should see if we can get JB to make this information public from the Gutter so we
        // don't have to calculate it.
        editor.putUserData(
            ToggleSnapshotLocationAction.POPUP_LINE,
            Integer.valueOf(EditorUtil.yPositionToLogicalLine(editor, event.getPoint())));
      } else {
        editor.putUserData(ToggleSnapshotLocationAction.POPUP_LINE, null);
      }
    }
  }

  public static class ProjectState {

    // For serialization purposes, this cannot be final.
    public List<RunConfigState> configStates = new ArrayList<RunConfigState>();

    public ProjectState() {}
  }

  public static class RunConfigState {

    public String configName;
    public CloudDebugProcessState processState;

    /** This is used during deserialization. */
    public RunConfigState() {}

    public RunConfigState(String configName, CloudDebugProcessState processState) {
      this.configName = configName;
      this.processState = processState;
    }
  }
}
