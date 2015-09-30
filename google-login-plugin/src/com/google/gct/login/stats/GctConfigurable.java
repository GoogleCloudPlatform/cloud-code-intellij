package com.google.gct.login.stats;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Created by nbashirbello on 9/29/15.
 */
public class GctConfigurable implements SearchableConfigurable {

    // EXample CloudTestingConfigurable

    private JCheckBox enableUsageTrackerBox;

    public GctConfigurable() {
    }

    @NotNull
    @Override
    public String getId() {
        return "google.usage.tracker";
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Google Cloud Tools Plugin";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Add the Usage Tracker Box
        JPanel usageTrackerGroup  = creatUsageTrackerComponent();
        mainPanel.add(usageTrackerGroup, BorderLayout.NORTH);

        return mainPanel;
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

    @Override
    public void disposeUIResources() {
        enableUsageTrackerBox = null;
    }

    private JPanel creatUsageTrackerComponent() {
        enableUsageTrackerBox = new JCheckBox("Would you like to help Google improve the Cloud Tools for IntelliJ plug-in?");
        JPanel usageTrackerGroup = new JPanel(new BorderLayout());
        usageTrackerGroup.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Usage Tracker"));
        usageTrackerGroup.add(enableUsageTrackerBox, BorderLayout.NORTH);
        return usageTrackerGroup;
    }
}
