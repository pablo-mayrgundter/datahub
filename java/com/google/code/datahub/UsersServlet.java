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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The UsersServlet provides information about usersthe current logged in
 * or anonymous user.
 *
 * @author Pablo Mayrgundter <pmy@google.com>
 */
public class UsersServlet extends Resource {

  static final long serialVersionUID = -6927719971633391020L;

  public UsersServlet() {}

  @Override
  public void doDelete(HttpServletRequest req, HttpServletResponse rsp)
      throws ServletException, IOException {
    throw new UnsupportedOperationException("Only GET is supported on this URL");
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse rsp)
      throws ServletException, IOException {
    String continueUrl = param("continueUrl", "/");
    JSONObject rspJson = new JSONObject();
    Util.jsonPut(rspJson, "signedIn", reqUser.signedIn());
    Util.jsonPut(rspJson, "name", reqUser.name);
    Util.jsonPut(rspJson, "loginUrl", User.createLoginURL(continueUrl));
    Util.jsonPut(rspJson, "logoutUrl", User.createLogoutURL(continueUrl));
    httpOk(rspJson, rsp);
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse rsp)
      throws ServletException, IOException {
    throw new UnsupportedOperationException("Only GET is supported on this URL");
  }

  @Override
  public void doPut(HttpServletRequest req, HttpServletResponse rsp)
      throws ServletException, IOException {
    throw new UnsupportedOperationException("Only GET is supported on this URL");
  }
}

