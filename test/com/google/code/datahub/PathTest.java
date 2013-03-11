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

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

/**
 * Tests for the Path class.
 *
 * @author Pablo Mayrgundter <pmy@google.com>
 */
public class PathTest extends BaseTest {

  /**
   * {path, docId} or {path, docId, normalizedPath}. normalized path
   * when path lacks / prefix or has it as a suffix.
   */
  String [][] testPaths = {
    {"/", "ROOT"},
    {"/foo", "ROOTfoo"},
    {"/foo/bar", "ROOTfoo_P2Fbar"},
    {"/foo/bar/", "ROOTfoo_P2Fbar", "/foo/bar"},
    {"/_", "ROOT_U"},
    {"_", "ROOT_U", "/_"},
    {"_/","ROOT_U", "/_"},
    {"/_/","ROOT_U", "/_"},
    {"/a/_foo","ROOTa_P2F_Ufoo", "/a/_foo"},
    {"/a_/foo","ROOTa_U_P2Ffoo", "/a_/foo"},
    {"/_U/U__U_P__D/", "ROOT_UU_P2FU_U_UU_UP_U_UD", "/_U/U__U_P__D"},
  };

  public void testResolvePart() {
    String kind = "kind";
    String name = "name";
    Key pathKey = KeyFactory.createKey(Path.ROOT_KEY, Path.PATH_KIND, name);
    Key kindKey = KeyFactory.createKey(Path.ROOT_KEY, kind, name);
    assertEquals(pathKey, Path.resolvePart(name, Path.ROOT_KEY));
    assertEquals(kindKey, Path.resolvePart(String.format("%s(%s)", kind, name), Path.ROOT_KEY));
  }

  public void testToKey() {
    for (String [] testCase : testPaths) {
      String strPath = testCase[0];
      Path path = Path.fromString(strPath);
      Key key = path.toKey();
      Path pathFromKey = Path.fromKey(key);
      String strFromPath = pathFromKey.toString();
      if (testCase.length == 3) {
        assertEquals("roundtrip encoding (normalized)", testCase[2], strFromPath);
      } else {
        assertEquals("roundtrip encoding (not normalized)", testCase[0], strFromPath);
      }
    }
  }

  public void testDocIdRecoding() {
    for (String [] testCase : testPaths) {
      String docId = Path.fromString(testCase[0]).toDocId();
      assertEquals("docId encoding", testCase[1], docId);
      String roundtripPath = Path.fromDocId(docId).toString();
      if (testCase.length == 3) {
        assertEquals("roundtrip encoding (normalized)", testCase[2], roundtripPath);
      } else {
        assertEquals("roundtrip encoding (not normalized)", testCase[0], roundtripPath);
      }
    }
  }

  public static void main(final String [] args) {
    junit.textui.TestRunner.run(PathTest.class);
  }
}
