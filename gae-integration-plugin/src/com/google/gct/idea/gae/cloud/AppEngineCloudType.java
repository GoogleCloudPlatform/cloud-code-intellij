package com.google.gct.idea.gae.cloud;


import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.remoteServer.RemoteServerConfigurable;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurator;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.configuration.deployment.DummyDeploymentConfiguration;
import com.intellij.remoteServer.configuration.deployment.JavaDeploymentSourceUtil;
import com.intellij.remoteServer.runtime.ServerConnector;
import com.intellij.remoteServer.runtime.ServerTaskExecutor;
import icons.GoogleAppEngineIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class AppEngineCloudType extends ServerType<AppEngineServerConfiguration> {

    public AppEngineCloudType() {
        super("google-app-engine");
    }


    @NotNull
    @Override
    public String getPresentableName() {
        return "Google App Engine MVM";
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return GoogleAppEngineIcons.AppEngine;
    }

    @NotNull
    @Override
    // Called when you creating a new GAE cloud instance from Settings->Cloud
    public AppEngineServerConfiguration createDefaultConfiguration() {
        return new AppEngineServerConfiguration();
    }

    @NotNull
    @Override
    // Called on start-up to create app engine deployment configuration
    public DeploymentConfigurator<?, AppEngineServerConfiguration> createDeploymentConfigurator(Project project) {
        return new AppEngineDeploymentConfigurator(project);
    }

    @NotNull
    @Override
    public ServerConnector<?> createConnector(@NotNull AppEngineServerConfiguration configuration, @NotNull ServerTaskExecutor asyncTasksExecutor) {
        return new AppEngineServerConnector(configuration);
    }

    @NotNull
    @Override
    // Need to create an new instance of the GAE in Settings->Cloud, thows an exception if this does not exist
    // java.lang.UnsupportedOperationException
    // at com.intellij.remoteServer.ServerType.createServerConfigurable(ServerType.java:45)
    // at com.intellij.remoteServer.ServerType.createConfigurable(ServerType.java:53)
    public RemoteServerConfigurable createServerConfigurable(@NotNull AppEngineServerConfiguration configuration) {
        return new AppEngineCloudConfigurable(configuration, null, true);
    }

    private static class AppEngineDeploymentConfigurator extends DeploymentConfigurator<DummyDeploymentConfiguration, AppEngineServerConfiguration> {
        private final Project myProject;

        public AppEngineDeploymentConfigurator(Project project) {
            myProject = project;
        }

        @NotNull
        @Override
        public List<DeploymentSource> getAvailableDeploymentSources() {
            //List<Artifact> artifacts = AppEngineUtil.collectAppEngineArtifacts(myProject, true);
            //return JavaDeploymentSourceUtil.getInstance().createArtifactDeploymentSources(myProject, artifacts);

            return new ArrayList<DeploymentSource>();
        }

        @NotNull
        @Override
        public DummyDeploymentConfiguration createDefaultConfiguration(@NotNull DeploymentSource source) {
            return new DummyDeploymentConfiguration();
        }

        @Override
        public SettingsEditor<DummyDeploymentConfiguration> createEditor(@NotNull DeploymentSource source, @NotNull RemoteServer<AppEngineServerConfiguration> server) {
            return null;
        }
    }

    private static class AppEngineServerConnector extends ServerConnector<DummyDeploymentConfiguration> {
        private final AppEngineServerConfiguration myConfiguration;

        public AppEngineServerConnector(AppEngineServerConfiguration configuration) {
            myConfiguration = configuration;
        }

        @Override
        public void connect(@NotNull final ConnectionCallback<DummyDeploymentConfiguration> callback) {
            //callback.connected(new AppEngineRuntimeInstance(myConfiguration));
        }
    }
}
