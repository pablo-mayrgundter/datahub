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

import java.io.IOException;
import java.io.Reader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author pmy@google.com (Pablo Mayrgundter)
 */
public class AbstractServlet extends HttpServlet {

  static final long serialVersionUID = 1286199158370880444L;

  HttpServletRequest req;
  protected boolean paramsOk;

  /**
   * Acquires member references to the request and response objects
   * for use by the param checking methods, and sets the paramsOk flag
   * to true, before invoking the superclass service method.
   */
  @Override
  public void service(final HttpServletRequest req, final HttpServletResponse rsp)
      throws IOException, ServletException {
    this.req = req;
    paramsOk = true;
    super.service(req, rsp);
  }

  // Proto helpers.

  /** HTTP POST succeeded creating entity: RFC 2616 Section 9.5 */
  protected void httpPostSuccess(HttpServletResponse rsp) {
    rsp.setStatus(HttpServletResponse.SC_CREATED);
  }

  /** HTTP DELETE succeeded deleting entity: RFC 2616 Section 9.7 */
  protected void httpDeleteSuccess(HttpServletResponse rsp) {
    rsp.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }

  /**
   * HTTP Success for GET, POST or DELETE.
   * @param msg optional response message, may be null.
   */
  protected void httpOk(String msg, HttpServletResponse rsp) throws IOException {
    rsp.setStatus(HttpServletResponse.SC_OK);
    if (msg != null) {
      rsp.getWriter().println(msg);
    }
  }

  protected void badRequest(String msg, HttpServletResponse rsp) throws IOException {
    rsp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    if (msg != null) {
      rsp.getWriter().println(msg);
    }
  }

  protected void notFound(String msg, HttpServletResponse rsp) throws IOException {
    rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    if (msg != null) {
      rsp.getWriter().println(msg);
    }
  }

  // Parameter checking helpers.

  /**
   * Helper for terse handling of param checking in request handlers.
   * Sample usage from EntityServlet:
   *
   * <pre>
   * public void doPost(HttpServletRequest req, HttpServletResponse rsp)
   *     throws ServletException, IOException {
   *
   *   String reqKind = param("kind");
   *   String reqJson = param("json");
   *   String reqTopic = param("topic");
   *   if (!paramsOk("Request for an entity must include a topic, kind and JSON.")) {
   *     return;
   *   }
   * </pre>
   */
  protected boolean paramsOk(String msg, HttpServletResponse rsp) throws IOException {
    if (paramsOk) {
      return true;
    }
    badRequest(msg, rsp);
    return false;
  }

  /**
   * @return the value of the request parameter with the given name.
   * If null, {@link #paramsOk(String)} will return false.
   */
  protected String param(String name) {
    String val;
    try {
      val = paramAllowNull(name);
    } catch (NullPointerException e) {
      // TODO(pmy): fix this terrible bug.
      System.err.println(e);
      return "";
    }
    paramsOk &= val != null;
    if (val == null) {
      return val;
    }
    return val.trim();
  }

  protected String readPostBody(Reader r) throws IOException {
    char [] inBuf = new char[1024];
    int len;
    StringBuffer outBuf = new StringBuffer();
    while ((len = r.read(inBuf)) != -1) {
      outBuf.append(inBuf, 0, len);
    }
    return outBuf.toString();
  }

  protected String param(String name, String defaultVal) {
    String val = req.getParameter(name);
    if (val == null) {
      return defaultVal;
    }
    return val;
  }

  protected String [] params(String name) {
    String [] vals = req.getParameterValues(name);
    if (vals == null) {
      paramsOk = false;
    }
    return vals;
  }

  /**
   * @return the value of the request parameter with the given name.
   */
  protected String paramAllowNull(String name) {
    paramsOk &= req.getParameterMap().containsKey(name);
    return req.getParameter(name);
  }

  /**
   * @return the value of the given parameter parsed to an integer, or
   * -1 if there is an error.  In the case of an error, paramsOk will
   * be set to false;
   */
  protected int paramToInt(String name, int ... defaultVal) {
    String val = req.getParameter(name);
    if (val == null) {
      if (defaultVal.length == 0) {
        paramsOk = false;
        return -1;
      } else {
        return defaultVal[0];
      }
    }
    try {
      return Integer.parseInt(val);
    } catch (NumberFormatException e) {
      paramsOk = false;
      return -1;
    }
  }
}
