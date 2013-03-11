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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for Util.
 *
 * @author Pablo Mayrgundter <pmy@google.com>
 */
public class UtilTest extends BaseTest {

  final String kind = "foo";
  final String pathStr = "/" + kind + "/bar";
  Path path = null;
  JSONObject json = null;

  public void setUp() {
    super.setUp();
    path = Path.fromString(pathStr);
    json = new JSONObject();
    try {
      json.put("a", "b");
      json.put("c", "d");
    } catch (JSONException e) {
      throw new RuntimeException("JSONObject no longer works as expected.");
    }
  }

  public void tearDown() {
    path = null;
    json = null;
    super.tearDown();
  }

  public void testVisitJson() throws Exception {
    final Map<String, Object> map = new HashMap<String, Object>();
    Util.visitJson(json, new Util.Visitor() {
        // TODO(pmy): fully test various types.
        public void visit(String key, Object val) {
          map.put(key, val);
        }
      });
    assertEquals(2, map.size());
    assertEquals("b", map.get("a"));
    assertEquals("d", map.get("c"));
  }

  public static void main(final String [] args) {
    junit.textui.TestRunner.run(UtilTest.class);
  }
}
