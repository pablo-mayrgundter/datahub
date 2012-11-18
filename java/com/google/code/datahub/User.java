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

import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Wrapper for session user.
 *
 * @author Pablo Mayrgundter <pmy@google.com>
 */
public class User {

  public static final User TEST_USER = new User();

  public boolean isSignedIn;
  // TODO(pmy): allow role change?
  public final boolean isAdmin;
  public final String id, name;
  String channel = null;

  public User() {
    isSignedIn = false;
    isAdmin = false;
    id = name = "TEST_USER(anonymous)";
  }

  public User(final HttpServletRequest req) {
    UserService service = UserServiceFactory.getUserService();
    com.google.appengine.api.users.User user = service.getCurrentUser();
    isSignedIn = user != null;
    isAdmin = isSignedIn && service.isUserAdmin();
    if (isSignedIn) {
      id = user.getUserId() == null ? user.getEmail() : user.getUserId();
      name = user.getNickname();
    } else {
      // TODO(pmy): better sessionless handling.
      HttpSession ses = req.getSession();
      if (ses == null) {
        id = req.getRemoteHost();
      } else {
        id = ses.getId();
      }
      name = "Anonymous at " + req.getRemoteHost();
    }
  }

  public boolean signedIn() {
    return isSignedIn;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public String getChannel() {
    if (channel == null) {
      channel = ChannelServiceFactory.getChannelService().createChannel(id);
    }
    return channel;
  }

  public String toString() {
    return String.format("User@%d{id: %s, name: %s, isAdmin: %b, isSignedIn: %b, channel: %s}",
                         System.identityHashCode(this),
                         id, name, isAdmin, isSignedIn, getChannel());
  }

  public static String createLoginURL(String destUrl) {
    return UserServiceFactory.getUserService().createLoginURL(destUrl);
  }

  public static String createLogoutURL(String destUrl) {
    return UserServiceFactory.getUserService().createLogoutURL(destUrl);
  }
}
