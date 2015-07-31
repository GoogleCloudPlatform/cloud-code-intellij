/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.samples;

import com.android.tools.idea.wizard.dynamic.DynamicWizardStepWithHeaderAndDescription;
import com.android.tools.idea.wizard.dynamic.ScopedStateStore;
import com.android.utils.HtmlBuilder;
import com.appspot.gsamplesindex.samplesindex.model.Sample;
import com.appspot.gsamplesindex.samplesindex.model.SampleCollection;
import com.google.common.base.Strings;
import com.google.gct.idea.util.GctStudioBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import static com.android.tools.idea.wizard.dynamic.ScopedStateStore.*;
import static com.google.gct.idea.samples.SampleImportWizardPath.SAMPLE_KEY;
import static com.google.gct.idea.samples.SampleImportWizardPath.SAMPLE_URL;

/**
 * SampleBrowserStep is the first page in the Sample Import wizard that allows the user to select a sample to import
 */
public class SampleBrowserStep extends DynamicWizardStepWithHeaderAndDescription {
  private Tree mySampleTree;
  private SampleImportTreeManager mySampleTreeManager;
  private JPanel myPanel;
  private HyperlinkLabel myUrlField;
  private JBLabel myDescriptionLabel;
  private SearchTextField mySearchBox;
  private JPanel myDescriptionPanel;
  private SamplePreviewPanel mySamplePreviewPanel;
  private JBScrollPane mySamplePreviewScrollPanel;
  private final SampleCollection mySampleList;
  private static final Key<Sample> SAMPLE_SCREENSHOT = createKey("SampleScreenshot", Scope.STEP, Sample.class);

  public SampleBrowserStep(@NotNull SampleCollection sampleList, Disposable parentDisposable) {
    super(GctStudioBundle.message("sample.browser.title"), GctStudioBundle.message("sample.browser.subtitle"), parentDisposable);
    mySampleList = sampleList;
    setBodyComponent(myPanel);
  }

  @Override
  public void init() {
    super.init();
    initSamplesTree();
    register(SAMPLE_URL, myUrlField, new ComponentBinding<String, HyperlinkLabel>() {
      @Override
      public void setValue(@Nullable String newValue, @NotNull HyperlinkLabel component) {
        component.setHyperlinkTarget(newValue);
        newValue = (StringUtil.isEmpty(newValue)) ? "" : GctStudioBundle.message("sample.browse.source");
        component.setHyperlinkText(newValue);
      }
    });
    register(SAMPLE_SCREENSHOT, mySamplePreviewPanel, new SamplePreviewComponentBinding());
    registerValueDeriver(SAMPLE_URL, new SampleUrlValueDeriver());
    registerValueDeriver(SAMPLE_SCREENSHOT, new SamplePreviewDeriver());
    registerValueDeriver(KEY_DESCRIPTION, new DescriptionValueDeriver());
    mySearchBox.addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        try {
          String keyword = e.getDocument().getText(0, e.getDocument().getLength());
          mySampleTreeManager.filterTree(keyword);
        }
        catch (BadLocationException e1) {
          // don't do anything if we can't figure out what the location is
        }
      }
    });
    myUrlField.setOpaque(false);
    myDescriptionPanel.setBackground(UIUtil.getTextFieldBackground());
    // for better mouse wheel scrolling
    mySamplePreviewScrollPanel.getVerticalScrollBar().setUnitIncrement(16);
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return mySearchBox;
  }

  @NotNull
  @Override
  public String getStepName() {
    return GctStudioBundle.message("sample.browser.title");
  }

  @Override
  public boolean validate() {
    if (myState.get(SAMPLE_KEY) == null) {
      setErrorHtml(GctStudioBundle.message("sample.browser.please.select"));
      return false;
    }
    setErrorHtml("");
    return true;
  }

  protected void initSamplesTree() {
    mySampleTreeManager = new SampleImportTreeManager(mySampleTree, mySampleList);
    myState.put(SAMPLE_KEY, mySampleTreeManager.getSelectedSample());
    register(SAMPLE_KEY, mySampleTree, new ComponentBinding<Sample, Tree>() {
      @Nullable
      @Override
      public Sample getValue(@NotNull Tree component) {
        return mySampleTreeManager.getSelectedSample();
      }

      @Override
      public void addActionListener(@NotNull ActionListener listener, @NotNull Tree component) {
        component.addTreeSelectionListener(new SampleSelectionListener(listener));
      }
    });
  }

  private static class SampleSelectionListener implements TreeSelectionListener {

    // this is required to redirect selectionListener to actionListener for use with ComponentBindings
    private final ActionListener myExternalListener;

    public SampleSelectionListener(ActionListener externalListener) {
      myExternalListener = externalListener;
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
      if (myExternalListener != null) {
        // populating a nonsense ActionEvent
        myExternalListener.actionPerformed(new ActionEvent(e.getSource(), -1, ""));
      }
    }
  }

  private static class SampleUrlValueDeriver extends ValueDeriver<String> {
    @Nullable
    @Override
    public Set<Key<?>> getTriggerKeys() {
      Set<Key<?>> filterKeys = new HashSet<Key<?>>(1);
      filterKeys.add(SAMPLE_KEY);
      return filterKeys;
    }

    @Nullable
    @Override
    public String deriveValue(@NotNull ScopedStateStore state, Key changedKey, @Nullable String currentValue) {
      Sample sample = state.get(SAMPLE_KEY);
      if (sample == null) {
        return "";
      }
      String url = sample.getCloneUrl();
      String path = sample.getPath();
      if (!Strings.isNullOrEmpty(path)) {
        return url + (url.endsWith("/") ? "" : "/") + "tree/master/" + SampleImportWizardPath.trimSlashes(path);
      }
      else {
        return url;
      }
    }
  }

  private static class DescriptionValueDeriver extends ValueDeriver<String> {
    @Nullable
    @Override
    public Set<Key<?>> getTriggerKeys() {
      Set<Key<?>> filterKeys = new HashSet<Key<?>>(1);
      filterKeys.add(SAMPLE_KEY);
      return filterKeys;
    }

    @Nullable
    @Override
    public String deriveValue(@NotNull ScopedStateStore state, Key changedKey, @Nullable String currentValue) {
      Sample sample = state.get(SAMPLE_KEY);
      if (sample == null) {
        return "";
      }
      HtmlBuilder description = new HtmlBuilder();
      description.openHtmlBody();
      if (sample.getDescription() != null) {
        description.addHtml(sample.getDescription());
      }
      else {
        description.add(GctStudioBundle.message("sample.browser.no.description"));
      }
      description.newlineIfNecessary().newline();
      description.add("Tags: ");
      description.add(StringUtil.join(sample.getCategories(),","));
      description.newlineIfNecessary().newline();
      description.closeHtmlBody();
      return description.getHtml();
    }
  }

  private static class SamplePreviewComponentBinding extends ComponentBinding<Sample, SamplePreviewPanel> {
    @Override
    public void setValue(@Nullable Sample sample, @NotNull SamplePreviewPanel component) {
      component.setSample(sample);
    }
  }

  private static class SamplePreviewDeriver extends ValueDeriver<Sample> {
    @Nullable
    @Override
    public Set<Key<?>> getTriggerKeys() {
      Set<Key<?>> filterKeys = new HashSet<Key<?>>(1);
      filterKeys.add(SAMPLE_KEY);
      return filterKeys;
    }

    @Nullable
    @Override
    public Sample deriveValue(@NotNull ScopedStateStore state, Key changedKey, @Nullable Sample currentValue) {
      return state.get(SAMPLE_KEY);
    }
  }

  protected JLabel getDescriptionLabel() {
    return myDescriptionLabel;
  }

  @NotNull
  @Override
  protected WizardStepHeaderSettings getStepHeader() {
    return WizardStepHeaderSettings.createProductHeader(GctStudioBundle.message("sample.import.title"));
  }
}
