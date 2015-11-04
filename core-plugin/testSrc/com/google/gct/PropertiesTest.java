package com.google.gct;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class PropertiesTest {

  @Test
  public void testSpace() throws IOException {
      File f = new File("resources/messages/CloudToolsBundle.properties");
      InputStream in = null;
      try {
          in = new FileInputStream(f);
          Properties p = new Properties();
          p.load(new InputStreamReader(in, "UTF-8"));
          Assert.assertEquals("Do you want to checkout branch {0} and restore the saved stash?",
                  p.getProperty("clouddebug.restorestash"));
      } finally {
          if (in != null) {
              in.close();
          }
      }
  }
}
