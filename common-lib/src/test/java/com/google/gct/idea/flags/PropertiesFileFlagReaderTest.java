package com.google.gct.idea.flags;

import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for {@link PropertiesFileFlagReader}.
 */
public class PropertiesFileFlagReaderTest {

  @Test
  public void testGetFlagString_flatExists() {
    PropertiesFileFlagReader flagReader = getValidPropertiesFlagReader();
    Assert.assertEquals("testing123", flagReader.getFlagString("test.flag"));
  }

  @Test
  public void testGetFlagString_flagDoesNotExist() {
    PropertiesFileFlagReader flagReader = getValidPropertiesFlagReader();
    Assert.assertNull(flagReader.getFlagString("non.existent.flag"));
  }

  @Test
  public void testGetFlagString_emptyFlagIsEmptyString() {
    PropertiesFileFlagReader flagReader = getValidPropertiesFlagReader();
    Assert.assertEquals("", flagReader.getFlagString("test.empty.flag"));
  }

  @Test
  public void testInvalidConfigFilePath() {
    try {
      new PropertiesFileFlagReader("idontexist.properties");
    } catch (IllegalArgumentException e) {
      return;
    }
    fail();
  }

  private PropertiesFileFlagReader getValidPropertiesFlagReader() {
    return new PropertiesFileFlagReader("com/google/gct/idea/flags/flags_test.properties");
  }
}