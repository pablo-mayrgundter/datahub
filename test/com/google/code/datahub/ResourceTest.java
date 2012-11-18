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

import static org.mockito.Mockito.*;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests for Resource.
 *
 * @author Pablo Mayrgundter
 */
public class ResourceTest extends UtilTest {

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

  static final String BODY_KEY = "body";
  static final String BODY_VAL = "ecosystem οικοσύστημα पारिस्थितिकी तंत्र";
  String obj = "{" + BODY_KEY + ": '" + BODY_VAL + "'}";
  final String [][] posts = {{"/test/", obj},
                             {"/test/foo/", obj}};
  HttpServletRequest req;
  HttpServletResponse rsp;
  ServletConfig servletConfig;
  Resource r;
  int id;

  public void setUp() {
    super.setUp();
    helper.setUp();
    servletConfig = mock(ServletConfig.class);
    req = mock(HttpServletRequest.class);
    rsp = mock(HttpServletResponse.class);
    r = new Resource();
    when(servletConfig.getInitParameter("path")).thenReturn("/test");
    try {
      r.init(servletConfig);
    } catch (ServletException e) {
      throw new RuntimeException(e);
    }
    id = 1;
  }

  public void tearDown() {
    helper.tearDown();
    super.tearDown();
  }

  /** Test creating items. */
  public void testPost() throws Exception {
    for (String [] post : posts) {
      final String path = post[0];
      final String body = post[1];

      when(req.getMethod()).thenReturn("POST");
      when(req.getRequestURI()).thenReturn(path);
      when(req.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
      final String newPath = path + "__" + id + "__";

      r.service(req, rsp);

      verify(rsp).setHeader(eq("Location"), eq(newPath));
      id++;
    }
  }

  /** Test retrieving items created with a call to testPost. */
  public void testGet() throws Exception {

    // Tests fetching items created in testPost.
    testPost();

    id = 1;
    for (String [] post : posts) {
      final String path = post[0];
      final String body = post[1];
      final String newPath = path + "__" + id + "__";

      StringWriter out;

      when(req.getMethod()).thenReturn("GET");
      when(req.getRequestURI()).thenReturn(newPath);
      when(rsp.getWriter()).thenReturn(new PrintWriter(out = new StringWriter()));

      r.service(req, rsp);

      JSONObject o = new JSONObject(out.toString());
      assertEquals(BODY_VAL, o.get(BODY_KEY));
      id++;
    }
  }

  /** Test updating items created with a call to testPut. */
  public void testPut() throws Exception {

    testPost();

    String newBody = "foo";
    String newObj = "{" + BODY_KEY + ": '" + newBody + "'}";

    id = 1;
    for (String [] post : posts) {
      final String path = post[0];
      final String body = post[1];
      final String newPath = path + "__" + id + "__";


      when(req.getMethod()).thenReturn("PUT");
      when(req.getRequestURI()).thenReturn(newPath);
      when(req.getReader()).thenReturn(new BufferedReader(new StringReader(newObj)));

      r.service(req, rsp);

      reset(req, rsp);

      StringWriter out;

      when(req.getMethod()).thenReturn("GET");
      when(req.getRequestURI()).thenReturn(newPath);
      when(rsp.getWriter()).thenReturn(new PrintWriter(out = new StringWriter()));

      r.service(req, rsp);

      JSONObject o = new JSONObject(out.toString());
      assertEquals(newBody, o.get(BODY_KEY));
      id++;
    }
  }

  /** Test deleting items created with a call to testPost. */
  public void testDelete() throws Exception {

    testPost();

    id = 1;
    for (String [] post : posts) {
      final String path = post[0];
      final String body = post[1];
      final String newPath = path + "__" + id + "__";


      when(req.getMethod()).thenReturn("DELETE");
      when(req.getRequestURI()).thenReturn(newPath);

      r.service(req, rsp);

      reset(req, rsp);

      when(req.getMethod()).thenReturn("GET");
      when(req.getRequestURI()).thenReturn(newPath);

      r.service(req, rsp);

      verify(rsp).setStatus(HttpServletResponse.SC_NOT_FOUND);

      id++;
    }
  }

  public static void main(final String [] args) {
    junit.textui.TestRunner.run(UtilTest.class);
  }
}
