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

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Tests for Datastore.
 *
 * @author Pablo Mayrgundter <pmy@google.com>
 */
public class DatastoreTest extends BaseTest {

  Datastore datastore;

  public void setUp() {
    super.setUp();
    datastore = new Datastore();
  }
  
  public void tearDown() {
    datastore = null;
    super.tearDown();
  }

  public void testAllocatedCreate() throws Exception {
    Path parent = Path.ROOT;
    JSONObject obj = new JSONObject();

    assertEquals(Path.fromString("/__1__"),
                 datastore.create(parent, obj, User.TEST_USER));

    assertEquals(Path.fromString("/__2__"),
                 datastore.create(parent, obj, User.TEST_USER));

    assertEquals(Path.fromString("/__3__"),
                 datastore.create(parent, obj, User.TEST_USER));

    assertEquals(Path.fromString("/__2__/__4__"),
                 datastore.create(Path.fromString("/__2__"), obj, User.TEST_USER));
  }

  public void testNamedCreate() throws Exception {
    Path parent = null;
    JSONObject obj = new JSONObject();

    assertEquals(Path.fromString("/foo"),
                 parent = datastore.create(Path.ROOT,
                                           "foo",
                                           obj,
                                           User.TEST_USER));

    assertEquals(Path.fromString("/foo/bar"),
                 datastore.create(parent, "bar", obj, User.TEST_USER));
  }

  public void testJSONConversion() throws Exception {
    JSONObject obj = new JSONObject();
    obj.put("foo", "bar");
    obj.put("null", JSONObject.NULL);

    Path path = datastore.create(Path.ROOT, "item", obj, User.TEST_USER);
    assertEquals(Path.fromString("/item"), path);

    obj = datastore.retrieve(path, User.TEST_USER);
    assertEquals("bar", obj.get("foo"));
    assertEquals(JSONObject.NULL, obj.get("null"));
  }

  public void testDelete() throws Exception {
    Path parent = Path.ROOT;
    JSONObject obj = new JSONObject();

    for (int i = 0; i < 3; i++) {
      datastore.create(parent, obj, User.TEST_USER);
    }

    Path [] paths = new Path[]{Path.fromString("/__1__"),
                               Path.fromString("/__2__"),
                               Path.fromString("/__3__")};
    for (Path p : paths) {
      datastore.delete(User.TEST_USER, p);
    }
  }

  public void testDeleteNonexistent() throws Exception {
    try {
      datastore.delete(User.TEST_USER, Path.fromString("/pinkelephant"));
      fail("Delete of non-existent object allowed.");
    } catch (Store.NotFoundException e) {
      // OK
    }
  }

  public void testMultiDelete() throws Exception {
    Path parent = Path.ROOT;
    JSONObject obj = new JSONObject();

    for (int i = 0; i < 3; i++) {
      datastore.create(parent, obj, User.TEST_USER);
    }

    Path [] paths = new Path[]{Path.fromString("/__1__"),
                               Path.fromString("/__2__"),
                               Path.fromString("/__3__")};
    datastore.delete(User.TEST_USER, paths);
  }

  public void testList() throws Exception {
    Path parent = Path.ROOT;
    JSONObject obj = new JSONObject();

    for (int i = 0; i < 3; i++) {
      datastore.create(parent, obj, User.TEST_USER);
    }

    // Create /__3__/__4__
    datastore.create(Path.fromString("/__3__"), obj, User.TEST_USER);

    JSONObject list = datastore.list(parent, 0, 10, null, null, null, -1,
                                     User.TEST_USER);

    assertNotNull(list.get("__1__"));
    assertNotNull(list.get("__2__"));
    assertNotNull(list.get("__3__"));
    try {
      list.get("__4__");
      fail("list of non-existent property should throw");
    } catch (org.json.JSONException e) {
      // OK
    }
    list = datastore.list(Path.fromString("/__3__/"), 0, 10, null, null, null, -1,
                          User.TEST_USER);
    assertNotNull(list.get("__4__"));
  }

  public static void main(final String [] args) {
    junit.textui.TestRunner.run(UtilTest.class);
  }

  // Helpers.
  JSONObject obj(String prop, String val) throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put(prop, val);
    return obj;
  }
}
