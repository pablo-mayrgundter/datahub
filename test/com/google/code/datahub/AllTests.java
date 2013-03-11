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

import junit.framework.TestSuite;

/**
 * This is a convenient target for the build system to invoke all of
 * the unit tests.
 *
 * @author Pablo Mayrgundter <pmy@google.com>
 */
public class AllTests {

  /**
   * Creates a suite of all unit tests in this package and
   * sub-packages.
   */
  public static TestSuite suite() {
    final TestSuite suite = new TestSuite();
    suite.addTestSuite(DatastoreTest.class);
    suite.addTestSuite(PathTest.class);
    suite.addTestSuite(ResourceTest.class);
    suite.addTestSuite(SearchTest.class);
    suite.addTestSuite(SecureDatastoreTest.class);
    suite.addTestSuite(UtilTest.class);
    return suite;
  }

  /**
   * Runnable as:
   *
   *   java com.google.appengine.demos.rest.AllTests
   */
  public static void main(final String [] args) {
    junit.textui.TestRunner.run(suite());
  }
}
