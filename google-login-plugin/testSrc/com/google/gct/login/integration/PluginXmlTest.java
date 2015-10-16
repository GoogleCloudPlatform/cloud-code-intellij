package com.google.gct.login.integration;

import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;

/**
 * Test that the plugin.xml file is at least marginally sensible.
 */
public class PluginXmlTest {

    @Test
    public void testWellFormed() throws SAXException, IOException {
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.parse("src/META-INF/plugin.xml");
        // throws exception if file is malformed
    }

}
