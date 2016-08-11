/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.appengine.server.instance;

import com.google.cloud.tools.intellij.appengine.facet.AppEngineFacet;
import com.google.cloud.tools.intellij.appengine.util.AppEngineUtilLegacy;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.configurations.RuntimeConfigurationWarning;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.javaee.deployment.DeploymentProvider;
import com.intellij.javaee.run.configuration.CommonModel;
import com.intellij.javaee.run.configuration.DeploysArtifactsOnStartupOnly;
import com.intellij.javaee.run.configuration.ServerModel;
import com.intellij.javaee.run.execution.DefaultOutputProcessor;
import com.intellij.javaee.run.execution.OutputProcessor;
import com.intellij.javaee.serverInstances.J2EEServerInstance;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.packaging.artifacts.ArtifactPointerManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Tag;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class AppEngineServerModel implements ServerModel, DeploysArtifactsOnStartupOnly {

  private ArtifactPointer myArtifactPointer;
  private int myPort = 8080;
  private String myServerParameters = "";
  private CommonModel myCommonModel;

  @Override
  public J2EEServerInstance createServerInstance() throws ExecutionException {
    return new AppEngineServerInstance(myCommonModel);
  }

  @Override
  public DeploymentProvider getDeploymentProvider() {
    return null;
  }

  @Override
  @NotNull
  public String getDefaultUrlForBrowser() {
    return "http://" + myCommonModel.getHost() + ":" + myPort;
  }

  @Override
  public SettingsEditor<CommonModel> getEditor() {
    return new AppEngineRunConfigurationEditor(myCommonModel.getProject());
  }

  @Override
  public OutputProcessor createOutputProcessor(ProcessHandler processHandler,
      J2EEServerInstance serverInstance) {
    return new DefaultOutputProcessor(processHandler);
  }

  @Override
  public List<Pair<String, Integer>> getAddressesToCheck() {
    return Collections.singletonList(Pair.create(myCommonModel.getHost(), myPort));
  }

  @Override
  public boolean isResourcesReloadingSupported() {
    return myCommonModel.isLocal();
  }

  @Override
  public List<Artifact> getArtifactsToDeploy() {
    return ContainerUtil.createMaybeSingletonList(getArtifact());
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    Artifact artifact;
    if (myArtifactPointer == null || (artifact = myArtifactPointer.getArtifact()) == null) {
      throw new RuntimeConfigurationError("Artifact isn't specified");
    }

    final AppEngineFacet facet = AppEngineUtilLegacy
        .findAppEngineFacet(myCommonModel.getProject(), artifact);
    if (facet == null) {
      throw new RuntimeConfigurationWarning(
          "App Engine facet not found in '" + artifact.getName() + "' artifact");
    }
  }

  @Override
  public int getDefaultPort() {
    return 8080;
  }

  @Override
  public void setCommonModel(CommonModel commonModel) {
    myCommonModel = commonModel;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  @Override
  public int getLocalPort() {
    return myPort;
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    final AppEngineModelSettings settings = new AppEngineModelSettings();
    XmlSerializer.deserializeInto(settings, element);
    myPort = settings.getPort();
    myServerParameters = settings.getServerParameters();
    final String artifactName = settings.getArtifact();
    if (artifactName != null) {
      myArtifactPointer = ArtifactPointerManager.getInstance(myCommonModel.getProject())
          .createPointer(artifactName);
    } else {
      myArtifactPointer = null;
    }
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    XmlSerializer
        .serializeInto(new AppEngineModelSettings(myPort, myArtifactPointer, myServerParameters),
            element, new SkipDefaultValuesSerializationFilters());
  }

  @Nullable
  public Artifact getArtifact() {
    return myArtifactPointer != null ? myArtifactPointer.getArtifact() : null;
  }

  public void setPort(int port) {
    myPort = port;
  }

  public String getServerParameters() {
    return myServerParameters;
  }

  public void setServerParameters(String serverParameters) {
    myServerParameters = serverParameters;
  }

  public void setArtifact(@Nullable Artifact artifact) {
    if (artifact != null) {
      myArtifactPointer = ArtifactPointerManager.getInstance(myCommonModel.getProject())
          .createPointer(artifact);
    } else {
      myArtifactPointer = null;
    }
  }

  public static class AppEngineModelSettings {

    @Tag("port")
    private int myPort = 8080;
    @Tag("artifact")
    private String myArtifact;
    @Tag("server-parameters")
    private String myServerParameters = "";

    public AppEngineModelSettings() {
    }

    public AppEngineModelSettings(int port, ArtifactPointer pointer, String serverParameters) {
      myPort = port;
      myServerParameters = serverParameters;
      myArtifact = pointer != null ? pointer.getArtifactName() : null;
    }

    public int getPort() {
      return myPort;
    }

    public void setPort(int port) {
      myPort = port;
    }

    public String getArtifact() {
      return myArtifact;
    }

    public void setArtifact(String artifact) {
      myArtifact = artifact;
    }

    public String getServerParameters() {
      return myServerParameters;
    }

    public void setServerParameters(String serverParameters) {
      myServerParameters = serverParameters;
    }
  }
}
