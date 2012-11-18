/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.code.datahub;

import com.google.appengine.api.search.Document;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tests for Search.
 *
 * @author Pablo Mayrgundter
 */
public class SearchTest extends UtilTest {

  LocalServiceTestHelper helper =
    new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  Document doc;

  public void setUp() {
    super.setUp();
    helper.setUp();
    doc = Search.jsonToDocument(path, json);
  }

  public void tearDown() {
    doc = null;
    helper.tearDown();
    super.tearDown();
  }

  public void testJsonToDocument() {
    assertEquals(3, doc.getFieldNames().size());
    assertEquals(Search.makePathTokens(path),
                 doc.getOnlyField(Search.INTERNAL_QUERY_FIELD_PATH).getText());
    assertEquals("b", doc.getOnlyField("a").getText());
    assertEquals("d", doc.getOnlyField("c").getText());
  }

  public void testDocumentToJson() throws JSONException {
    JSONObject json = Search.documentToJson(doc);
    assertEquals(2, json.length());
    assertEquals("b", json.get("a"));
    assertEquals("d", json.get("c"));
  }

  public void testMakePathTokens() {
    String [][] pathsToTokens = {
      {"/", "ROOT"},
      {"/a", "ROOTa ROOT"},
      {"/a/", "ROOTa ROOT"},
      {"/a/b", "ROOTa_P2Fb ROOTa ROOT"},
    };
    for (String [] pathAndTokens : pathsToTokens) {
      Path p = new Path(pathAndTokens[0]);
      assertEquals("path tokens",
                   pathAndTokens[1], Search.makePathTokens(p));
    }
  }

  public static void main(final String [] args) {
    junit.textui.TestRunner.run(UtilTest.class);
  }
}
