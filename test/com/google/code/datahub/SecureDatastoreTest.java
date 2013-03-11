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

import org.json.JSONObject;

/**
 * Tests for SecureDatastore, to exercise ACL controls for allow,
 * restrict and combinations with different precedence.
 *
 * @author Pablo Mayrgundter <pmy@google.com>
 */
public class SecureDatastoreTest extends DatastoreTest {

  SecureDatastore store;

  public void setUp() {
    super.setUp();
    store = new SecureDatastore();
  }
  
  public void tearDown() {
    store = null;
    super.tearDown();
  }

  public void testAssertAllowed() {
    store.assertAllowed(Path.ROOT, User.TEST_USER, SecureDatastore.Op.READ);
    store.retrieve(Path.ROOT, User.TEST_USER);
  }

  public void testSetRestricted() {
    testAssertAllowed();
    store.setRestricted(Path.ROOT, User.TEST_USER, SecureDatastore.Op.READ);
    try {
      store.retrieve(Path.ROOT, User.TEST_USER);
      fail("Read allowed but should be restricted.");
    } catch (SecureDatastore.OperationRestrictedException e) {
      // OK.
    }
  }

  public void testClearRestricted() {
    testSetRestricted();
    store.clearRestricted(Path.ROOT, User.TEST_USER, SecureDatastore.Op.READ);
    store.assertAllowed(Path.ROOT, User.TEST_USER, SecureDatastore.Op.READ);
    store.retrieve(Path.ROOT, User.TEST_USER);
  }

  public void testParentControlsChild() {

    JSONObject o = new JSONObject();
    User u = User.TEST_USER;

    Path a = store.create(Path.ROOT, "a", o, u);
    Path b = store.create(a, "b", o, u);
    Path c = store.create(b, "c", o, u);
    store.setRestricted(Path.ROOT, User.TEST_USER, SecureDatastore.Op.READ);
    store.setAllowed(c, User.TEST_USER, SecureDatastore.Op.READ);

    try {
      store.retrieve(a, User.TEST_USER);
      fail("Read allowed but should be restricted.");
    } catch (SecureDatastore.OperationRestrictedException e) {
      // OK.
    }

    try {
      store.retrieve(b, User.TEST_USER);
      fail("Read allowed but should be restricted.");
    } catch (SecureDatastore.OperationRestrictedException e) {
      // OK.
    }

    try {
      store.retrieve(c, User.TEST_USER);
    } catch (SecureDatastore.OperationRestrictedException e) {
      fail("Read restricted but should be allowed.");
    }
  }
}
