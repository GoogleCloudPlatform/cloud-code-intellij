/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.login.integration;

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
        // Shouldn't this be reading from the build output classpath?
        reader.parse("resources/META-INF/plugin.xml");
        // throws exception if file is malformed
    }
}
