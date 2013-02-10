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

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The Resource servlet is an abstract implementation CRUD+Search
 * semantics over RESTful HTTP.  Subclasses must provide a top-level
 * resource type which will be required in the prefix of resource
 * references in URIs as well as in internal service object IDs.  For
 * example, a type of "foo" would result in URI paths of the form
 * "/foo/..." and the internal service object IDs:
 *
 * If the request path ends in a slash "/" character, it is
 * interpreted as an existing collection.  Otherwise, it is
 * interpreted as an element of a collection.  All collections are
 * themselves elements of a parent collection, except for the root,
 * which has no parent.
 *
 * @see <a
 * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html">HTTP -
 * Method Definitions</a>.
 * @see <a
 * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">HTTP
 * - Status Code Definitions</a>.
 * @see http://en.wikipedia.org/wiki/Representational_state_transfer
 * @author pmy@google.com (Pablo Mayrgundter)
 */
public class Resource extends AbstractServlet {

  protected static final Logger logger = Logger.getLogger(Resource.class.getName());

  static final long serialVersionUID = 4216342727693173863L;

  static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

  /**
   * The path to the current request, equivalent ot
   * request.getRequestURI().
   */
  protected Path reqPath = null;

  /**
   * The parsed JSON request object, supplied in POST and PUT methods.
   */
  protected JSONObject reqJson = null;

  /**
   * The user making the request, possibly anonymous.
   */
  protected User reqUser = null;

  /**
   * The backing searchable datastore.
   */
  protected CompositeStore store;

  public Resource() {}

  // Servlet initialization.

  @Override
  public void init() throws ServletException {
    String path = getServletConfig().getInitParameter("path");
    String parentPath = getServletConfig().getInitParameter("parentPath");
    if (path == null) {
      throw new IllegalStateException(String.format("web.xml config for servlet(%s) requires"
                                                    + " init param 'path'",
                                                    getServletConfig().getServletName()));
    }
    store = new CompositeStore(Path.fromString(path),
                               parentPath == null ? null : Path.fromString(parentPath));
  }

  // HTTP method delegation: service, DELETE, GET, POST, PUT

  /**
   * Acquires member references to the request and response objects for
   * use by the param checking methods, and sets the paramsOk flag to
   * true, before invoking the superclass service method.
   */
  @Override
  public void service(final HttpServletRequest req, final HttpServletResponse rsp)
      throws ServletException, IOException {
    // Setup for handling this request.
    reqPath = Path.fromRequest(req);
    reqUser = new User(req);

    // Trigger delegated handling (via super) in this servlet and
    // subclasses.
    try {
      super.service(req, rsp);
    } catch (Store.OperationRestrictedException secEx) {
      rsp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      rsp.getWriter().println(secEx.getMessage());
    } catch (Store.NotFoundException missingEx) {
      rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    // Setup for next request.
    reqPath = null;
    reqUser = null;
    reqJson = null;
  }

  /**
   * Deletes the resource at the requested path, or the search indexes
   * for this resoruce if path is of the form /[this rsrc]/__index__.
   */
  @Override
  public void doDelete(final HttpServletRequest req, final HttpServletResponse rsp)
      throws ServletException, IOException {

    if (reqPath.isSpecial()
        && !reqPath.isSpecialSerial()) {
      String filename = reqPath.getFilename();
      if (filename.equals("__index__")) {
        String [] queryIds = req.getParameterValues("queryId");
        if (queryIds == null) {
          store.deleteIndexes(reqPath, reqUser);
        } else {
          store.deleteQueries(reqUser, queryIds);
        }
        return;
      }
      notFound("Special file not found", rsp);
      return;
    }

    store.delete(reqUser, reqPath);
  }

  /**
   * The requested resource is writen to the response stream as a
   * JSON-encoded object.
   *
   * Returned items may be modified by the following parameters:
   *
   * <ol>
   *   <li>field selection: <code>select=[field_path](,[field_path])*</code></li>
   * </ol>
   *
   * If the requested resource is a collection, a listing of its
   * contents is returned, which may be constrained by the following
   * parameters:
   *
   * <ol>
   *   <li>query: <code>q=[see query grammar link below]</code></li>
   *   <li>field ordering: <code>order=(asc||desc)(,(asc||desc))*</code></li>
   *   <li>result set offset: <code>offset=\d+</code></li>
   *   <li>result set limit: <code>limit=\d+</code></li>
   * </ol>
   *
   * TODO(pmy): this adds side-effects to GET.
   *
   * Both request forms may be monitored for future modifications by
   * establishing a watch:
   *
   * <ol>
   *   <li>watch: <code>watch=[watch_id]</code></li>
   *   <li>watch duration: <code>watch_duration=\d+</code></li>
   * </ol>
   *
   * TODO(pmy): link to query grammar.
   *
   * <code>field_path</code> may be interpreted as a simple name or as
   * a path to a field within a hierarchical document, as in XPath.
   *
   * <code>stream_id</code> TODO(pmy)
   *
   * <code>stream_duration</code> in seconds, 0 for no expiration.
   *
   * Example:
   * <pre>
   * /foo/bar?select=title,name&order=asc,desc&offset=100&limit=3
   * </pre>
   *
   * @see http://en.wikipedia.org/wiki/Representational_state_transfer#RESTful_web_services
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse rsp)
      throws ServletException, IOException {

    if (reqPath.isSpecial()
        && !reqPath.isSpecialSerial()) {
      String filename = reqPath.getFilename();
      if (filename.equals("__acl__")) {
        if (!reqUser.isAdmin() && false) {
          throw new IllegalStateException("Only the admin may query restrictions.");
        }
        // __acl__ requests are on the parent.
        reqPath = reqPath.getParent();
        final String reqRestrictUser = param("user");
        final String reqRestrictOp = param("op");

        final User restrictUser = new User(reqRestrictUser);
        final boolean restricted =
          store.datastoreAsAclService.isRestricted(reqPath, restrictUser,
                                                   Store.Op.valueOf(reqRestrictOp.toUpperCase()));
        httpOk(restricted ? "restricted" : "allowed", rsp);
        return;
      } else if (filename.equals("__index__")) {
        String queries = paramAllowNull("queries");
        if (queries == null) {
          httpOk(store.getIndexMap(reqPath, reqUser), rsp);
        } else {
          httpOk(store.retrieveQueries(reqUser), rsp);
        }
        return;
      } else if (filename.equals("__bbqsauce__")) {
        // This is a gross hack to expose a global administrative
        // interface to the low-level PSI subscription state.  This is
        // needed because Search.queryIndex (rightly) restricts access
        // to internal subscriptions from its API.  TODO(pmy): A
        // better way to do this would be to provide the
        // administrative interface expicitly there.

        ((Search) store.search)
            .queryIndex.pss.unsubscribe(req.getParameter("topic"),
                                        req.getParameter("queryId"));
        httpOk("Queries deleted", rsp);
        return;
      }
      notFound("Special file not found", rsp);
      return;
    }

    final String reqQuery = req.getParameter("q");
    final int reqOffset = Math.abs(paramToInt("offset", 0));
    final int reqLimit = Math.abs(paramToInt("limit", 10));
    final long reqDuration = paramToInt("duration", -1);

    if (!paramsOk("offset, limit and duration must be integer values", rsp)) {
      return;
    }

    // http://en.wikipedia.org/wiki/Representational_state_transfer#RESTful_web_services
    JSONObject rspJson;
    if (req.getRequestURI().endsWith("/")) {
      // Colleciton request.
      rspJson = store.list(reqPath, reqOffset, reqLimit,
                           null, null, reqUser.id, reqDuration, reqUser);
    } else {
      // Item request or Search.
      if (reqQuery == null) {
        // Just retrieve the item.
        rspJson = store.retrieve(reqPath, reqUser);
      } else {
        // Search its index.
        rspJson = store.search(reqPath, reqQuery,
                               reqOffset, reqLimit,
                               null, null,
                               reqUser.id,
                               reqDuration,
                               reqUser);
      }
    }

    httpOk(rspJson, rsp);
  }

  /**
   * The given JSON-encoded resource is created in the collection
   * specified by the given address.
   */
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse rsp)
      throws ServletException, IOException {
    reqJson = readJsonOrBadRequest("The request must include a JSON-encoded object.",
                                   rsp);
    if (reqJson == null) {
      return;
    }

    Path path = store.create(reqPath, reqJson, reqUser);
    rsp.setHeader("Location", req.getServletPath() + path.toString());
  }

  /**
   * The given JSON-encoded resource is stored at the given address.
   *
   * TODO(pmy): If a new resource is created, the response code will
   * be 201 (Created). If an existing resource is modified, either the
   * 200 (OK) or 204 (No Content) response codes will be sent to
   * indicate successful completion of the request: 204 with metadata
   * (entity header) updates or 200 without.
   */
  @Override
  public void doPut(HttpServletRequest req, HttpServletResponse rsp)
      throws ServletException, IOException {
    String filename = reqPath.getFilename();
    if (filename.equals("__acl__")) {
      if (!reqUser.isAdmin() && false) {
        throw new IllegalStateException("Only the admin may modify restrictions.");
      }
      // __acl__ requests are on the parent.
      reqPath = reqPath.getParent();
      final String reqRestrictUser = param("user");
      final String reqRestrictOp = param("op");
      final String reqRestrictClear = paramAllowNull("clear");

      final User restrictUser = new User(reqRestrictUser);
      final boolean restrictClear = reqRestrictClear != null;
      if (restrictClear) {
        store.datastoreAsAclService.clearRestricted(reqPath, restrictUser,
                                                    Store.Op.valueOf(reqRestrictOp.toUpperCase()));
      } else {
        store.datastoreAsAclService.setRestricted(reqPath, restrictUser,
                                                  Store.Op.valueOf(reqRestrictOp.toUpperCase()));
      }
      return;
    }

    reqJson = readJsonOrBadRequest("The request must include a JSON-encoded object.",
                                   rsp);
    if (reqJson == null) {
      return;
    }

    // The datastore API doesn't directly report whether create or
    // update happened, so this will require a fetch then set for the
    // extended semantics of 204 updates.  This may be useful anyways
    // to know whether the modification was content-changing and so
    // requires new entity headers, e.g. E-Tag.
    store.update(reqPath, reqJson, reqUser);
  }

  // Protocol helpers.

  protected JSONObject readJsonOrBadRequest(String errorMsg,
                                            HttpServletResponse rsp) throws IOException {
    String postBody = readPostBody(req.getReader());
    try {
      System.out.println("POST BODY: " + postBody);
      JSONObject o = new JSONObject(postBody);
      System.out.println("\nPARSED OBJECT: " + o);
      return o;
    } catch (JSONException e) {
      badRequest(errorMsg, rsp);
      return null;
    }
  }

  /**
   * Sets the response Content-Type header to "application/json;
   * charset=UTF-8" and then calls httpOk(String, HttpServletResponse).
   *
   * @param msg optional response message, may be null.
   */
  protected void httpOk(JSONObject rspJson, HttpServletResponse rsp) throws IOException {
    rsp.setContentType(JSON_CONTENT_TYPE);
    httpOk(Util.jsonPretty(rspJson), rsp);
  }
}
