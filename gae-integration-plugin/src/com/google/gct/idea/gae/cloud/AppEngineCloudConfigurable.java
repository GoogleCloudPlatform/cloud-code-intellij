package com.google.gct.idea.gae.cloud;

import com.google.gct.login.CredentialedUser;
import com.google.gct.login.ui.GoogleLoginUsersPanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.remoteServer.RemoteServerConfigurable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Viewable in Settings -> Cloud
 */
public class AppEngineCloudConfigurable extends RemoteServerConfigurable {
    private JPanel myMainPanel;

    public AppEngineCloudConfigurable(AppEngineServerConfiguration configuration, Object o, boolean b) {
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        //GoogleLoginUsersPanel usersPanel = new GoogleLoginUsersPanel();

        myMainPanel = new JPanel();
        JLabel label = new JLabel("Extra stuff");
        myMainPanel.add(label);
        //myMainPanel.add(usersPanel);
        return myMainPanel;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }

    @Override
    public void reset() {

    }


}
