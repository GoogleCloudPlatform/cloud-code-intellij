package com.google.gct.idea.debugger.ui;

/**
 * Stores the error message received when querying debug targets, but provides a blank item as
 * representation in a JComboBox
 */
// The reason for this class is that the combobox must have an item to ensure that
// com.google.gct.idea.debugger.ui.CloudAttachDialog#doValidate() method correctly identifies
// the case when the result for querying debug targets has been received and validation can be
// carried out
public class ErrorHolder implements DebugTargetSelectorItem {

  private String errorMessage;

  public ErrorHolder(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  @Override
  // return empty String for the renderer
  public String toString() {
    return "";
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
