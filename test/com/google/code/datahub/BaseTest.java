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

import com.google.appengine.tools.development.testing.LocalChannelServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalSearchServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalUserServiceTestConfig;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Base test to setup the GAE environment.
 *
 * @author Pablo Mayrgundter <pmy@google.com>
 */
abstract class BaseTest extends TestCase {

  LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalChannelServiceTestConfig(),
                                 new LocalDatastoreServiceTestConfig(),
                                 new LocalSearchServiceTestConfig(),
                                 new LocalUserServiceTestConfig())
      .setEnvAppId("app")
      .setEnvAuthDomain("example.com")
      .setEnvIsAdmin(true)
      .setEnvIsLoggedIn(true)
      .setEnvEmail("test@example.com");

  public void setUp() {
    helper.setUp();
  }

  public void tearDown() {
    helper.tearDown();
  }
}
