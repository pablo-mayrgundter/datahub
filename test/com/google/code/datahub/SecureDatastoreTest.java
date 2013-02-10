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

/**
 * Tests for SecureDatastore.
 *
 * @author Pablo Mayrgundter
 */
public class SecureDatastoreTest extends DatastoreTest {

  SecureDatastore secureStore;

  public void setUp() throws Exception {
    super.setUp();
    secureStore = (SecureDatastore) datastore;
  }

  public void testAssertNotRestricted() {
    secureStore.assertNotRestricted(Path.ROOT, User.TEST_USER, SecureDatastore.Op.READ);
    datastore.retrieve(Path.ROOT, User.TEST_USER);
  }

  public void testSetRestricted() {
    testAssertNotRestricted();
    secureStore.setRestricted(Path.ROOT, User.TEST_USER, SecureDatastore.Op.READ);
    try {
      datastore.retrieve(Path.ROOT, User.TEST_USER);
      fail("Read restricted but allowed.");
    } catch (SecureDatastore.OperationRestrictedException e) {
      // OK.
    }
  }

  public void testClearRestricted() {
    testSetRestricted();
    secureStore.clearRestricted(Path.ROOT, User.TEST_USER, SecureDatastore.Op.READ);
    secureStore.assertNotRestricted(Path.ROOT, User.TEST_USER, SecureDatastore.Op.READ);
    datastore.retrieve(Path.ROOT, User.TEST_USER);
  }
}
