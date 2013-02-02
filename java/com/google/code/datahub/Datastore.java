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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import static com.google.appengine.api.datastore.FetchOptions.Builder.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Logger;

/**
 * The Datastore class provides a JSON-based interface to the GAE
 * Datastore API.  Use of Entity is replaced by JSONObject and Key is
 * replaced by its String representation.
 *
 * TODO(pmy): document Entity/JSONObject transcoding scheme.
 * TODO(pmy): @see KeyFactory string methods.
 * TODO(pmy): update(path, json, currentVersion)
 *
 * @author Pablo Mayrgundter
 */
public class Datastore extends AbstractStore {

  protected static final Logger logger = Logger.getLogger(Resource.class.getName());
  protected static final Logger aclLogger = Logger.getLogger(Datastore.class.getName() + "_ACLs");

  static final int DEFAULT_QUERY_LIMIT = 10;

  /** The key name for the ACL property. */
  static final String PROP_ACL_KEY = "##ACL##";

  /** The key name for the internal-only parent-key pointer. */
  static final String INTERNAL_PARENT_PROP = "##PARENT##";

  final DatastoreService service;

  public Datastore() {
    service = DatastoreServiceFactory.getDatastoreService();
    Entity root = new Entity(Path.ROOT_KEY);
    service.put(root);
  }

  @Override
  public Path create(Path parent, JSONObject json, User user) {
    logger.fine("create, parent path: " + parent);
    check(parent, user, Op.CREATE);
    final Key parentKey = parent.toKey();
    logger.fine("create, parent key: " + parentKey);
    return create(new Entity(Path.PATH_KIND, parentKey), json, user);
  }

  @Override
  public Path create(Path parent, String name, JSONObject json, User user) {
    logger.fine("create, parent path: " + parent);
    check(parent, user, Op.CREATE);
    final Key parentKey = parent.toKey();
    logger.fine("create, parent key: " + parentKey);
    return create(new Entity(Path.resolvePart(name, parentKey)), json, user);
  }

  @Override
  public void delete(User user, Path ... paths) {
    Key [] keys = new Key[paths.length];
    for (int i = 0; i < keys.length; i++) {
      check(paths[i], user, Op.DELETE);
      keys[i] = paths[i].toKey();
    }
    service.delete(keys);
  }

  @Override
  public JSONObject list(Path path,
                         int offset, int limit, String [] fields, int [] order,
                         String reqEndpointId, long duration,
                         User user) {
    check(path, user, Op.READ);
    Key key = path.toKey();
    // TODO(pmy): Shouldn't need to store the parent key in every
    // entity to do this, but couldn't figure out how to limit results
    // otherwise.
    Query q = new Query(Path.PATH_KIND, Path.ROOT_KEY)
      .setFilter(new Query.FilterPredicate(INTERNAL_PARENT_PROP,
                                           Query.FilterOperator.EQUAL,
                                           key));
    //.setKeysOnly();
    return entitiesToJson(service.prepare(q).asList(withLimit(10)));
  }

  @Override
  public JSONObject retrieve(Path path, User user) {
    check(path, user, Op.READ);
    // TODO(pmy): remove this try/catch when ACLs check is actually
    // performed, as it will throw for missing..
    Key key = path.toKey();
    logger.fine("service.get: " + key);
    try {
      return entityToJson(service.get(key));
    } catch (EntityNotFoundException e) {
      throw new NotFoundException(path);
    }
  }

  @Override
  public void update(Path path, JSONObject json, User user) {
    // Named create uses this path.
    try {
      check(path, user, Op.UPDATE);
    } catch (NotFoundException e) {
      // TODO(pmy): OK?
    }
    service.put(jsonToEntity(path, json));
  }

  @Override
  public JSONObject search(Path path, String query, User user) {
    check(path, user, Op.READ);
    return search(path, query, 0, DEFAULT_LIMIT, null, null, null, DURATION_UNDEFINED, user);
  }

  @Override
  public JSONObject search(Path path,
                           String query,
                           int offset, int limit,
                           String [] fields, int [] order,
                           String endpointId, long duration,
                           User user) {
    check(path, user, Op.READ);
    return entitiesToJson(service.prepare(parseQuery(path, query))
                          .asQueryResultList(FetchOptions.Builder
                                             .withOffset(offset)
                                             .limit(limit)));
  }

  /**
   * Helper for the two public add methods, once the entity has been
   * constructed.
   */
  Path create(Entity entity, JSONObject json, User user) {
    // TODO(pmy): Util.jsonPut(json, PROP_ACL_KEY, new JSONObject());
    Key key = service.put(setProperties(entity, json));
    logger.fine("create helper, inner key: " + key);
    return Path.fromKey(key);
  }

  /**
   * @throws OperationRestrictedException If the given tuple is restricted.
   */
  final void check(Path path, User user, Op op) throws SecurityException {
    // TODO(pmy): cache key ACLs?
    Key key = path.toKey();
    aclLogger.fine(String.format("path(%s) key(%s) operation(%s) check for user(%s)",
                                 path, key, op, user));
    Entity entity;
    try {
      entity = service.get(key);
    } catch (EntityNotFoundException e) {
      aclLogger.fine("check: not found!");
      throw new NotFoundException(path);
    }
    aclLogger.fine("check: found!");
    String aclStr = (String) entity.getProperty(PROP_ACL_KEY);
    if (aclStr == null) {
      aclLogger.fine("Got to getProperty(ACL); the rest is unimplemented");
      return;
    }
    JSONObject acl;
    // TODO(pmy): don't use JSON here.
    try {
      acl = new JSONObject(aclStr);
    } catch (JSONException e) {
      throw new IllegalStateException("Invalid ACL string: " + aclStr);
    }
    if (isRestrictedInAcl(acl, user, op)) {
      aclLogger.warning(String.format("path(%s) operation(%s) check for user(%s)", path, op, user));
      throw new OperationRestrictedException(path, user, op);
    }
  }

  boolean isRestrictedInAcl(JSONObject acl, User user, Op op) {
    return false;
  }

  // Query support.

  Query parseQuery(Path path, String queryString) {
    // TODO(pmy): quick and dirty query parser, or maybe use search.query.QueryParser?
    Query query = new Query(path.toKey());
    /*
    for (String [] filter : exprToTriples(filterExpr)) {
      q.addFilter(filter[0], strToOp(filter[1]), filter[2]);
    }
    */
    //q.addSort("created", Query.SortDirection.DESCENDING);
    return query;
  }

  String [][] filterExprToFilterTriples(String s) {
    return new String[0][0];
  }

  // TODO(pmy)
  static String [][] exprToTriples(String filterExpr) {
    return new String[0][0];
  }

  static Query.FilterOperator strToOp(String opStr) {
    Query.FilterOperator op;
    if (opStr.equals("=")) {
      op = Query.FilterOperator.EQUAL;
    } else if (opStr.equals("!=")) {
      op = Query.FilterOperator.NOT_EQUAL;
    } else if (opStr.equals(">")) {
      op = Query.FilterOperator.GREATER_THAN;
    } else if (opStr.equals(">=")) {
      op = Query.FilterOperator.GREATER_THAN_OR_EQUAL;
    } else if (opStr.equals("<")) {
      op = Query.FilterOperator.LESS_THAN;
    } else if (opStr.equals("<=")) {
      op = Query.FilterOperator.LESS_THAN_OR_EQUAL;
    } else {
      throw new UnsupportedOperationException("Unknown filter: " + opStr);
    }
    return op;
  }

  // Entity/JSON helpers.

  // TODO(pmy): consolidate jsonToEntiy mapping with Search and
  // delegate for customziation.  Maybe move this up to
  // ResourceServlet?

  /**
   * Creates a new Entity with the given path and JSONObject's
   * properties.
   */
  public static Entity jsonToEntity(Path path, JSONObject json) {
    Key k = path.toKey();
    Entity entity = new Entity(k);
    setProperties(entity, json);
    return entity;
  }

  /**
   * For each property in the given JSONObject, set the corresponding
   * property in the given Entity.
   *
   * @return the given Entity.
   */
  static Entity setProperties(final Entity entity, JSONObject json) {
    Util.visitJson(json, new Util.Visitor() {
        public void visit(String key, String val) {
          entity.setProperty(key, val);
        }
      });
    Key key = entity.getKey();
    Key parent = key.getParent();
    // TODO(pmy): parent may be null: does this matter?
    entity.setProperty(INTERNAL_PARENT_PROP, parent);
    // TODO(pmy): search
    //entity.setProperty(Search.INTERNAL_QUERY_FIELD_PATH,
    // Search.makePathTokens(Path.fromKey(key)));
    return entity;
  }

  public static JSONObject entitiesToJson(Iterable<Entity> entities) {
    JSONObject json = new JSONObject();
    for (Entity e : entities) {
      Util.jsonPut(json, Path.fromKey(e.getKey()).getFilename(), entityToJson(e));
    }
    return json;
  }

  public static JSONObject entityToJson(Entity entity) {
    JSONObject json = new JSONObject();
    for (java.util.Map.Entry<String, Object> property : entity.getProperties().entrySet()) {
      String keyName = property.getKey();
      if (keyName.equals(INTERNAL_PARENT_PROP)
          || keyName.startsWith(Search.INTERNAL_QUERY_FIELD_PATH)) {
        continue;
      }
      Object val = property.getValue();
      // TODO(pmy): JSONObject does not accept null values.
      if (val == null) {
        continue;
      }
      if (val instanceof com.google.appengine.api.datastore.Text) {
        val = ((com.google.appengine.api.datastore.Text) val).getValue();
      } else if (val instanceof Number) {
        val = "" + val;
      }
      Util.jsonPut(json, keyName, val);
    }
    return json;
  }
}
