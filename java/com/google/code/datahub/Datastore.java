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
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PropertyContainer;
import com.google.appengine.api.datastore.Query;
import static com.google.appengine.api.datastore.FetchOptions.Builder.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
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
    final Key parentKey = parent.toKey();
    logger.fine("create, parent key: " + parentKey);
    return create(new Entity(Path.PATH_KIND, parentKey), json, user);
  }

  @Override
  public Path create(Path parent, String name, JSONObject json, User user) {
    logger.fine("create, parent path: " + parent);
    final Key parentKey = parent.toKey();
    logger.fine("create, parent key: " + parentKey);
    return create(new Entity(Path.resolvePart(name, parentKey)), json, user);
  }

  @Override
  public void delete(User user, Path ... paths) {
    Key [] keys = new Key[paths.length];
    for (int i = 0; i < keys.length; i++) {
      keys[i] = paths[i].toKey();
      assertExists(paths[i]);
    }
    service.delete(keys);
  }

  @Override
  public JSONObject list(Path path,
                         int offset, int limit, String [] fields, int [] order,
                         String reqEndpointId, long duration,
                         User user) {
    Key key = path.toKey();
    // TODO(pmy): Shouldn't need to store the parent key in every
    // entity to do this, but couldn't figure out how to limit results
    // otherwise.
    Query q = new Query(Path.PATH_KIND, Path.ROOT_KEY)
      .setFilter(new Query.FilterPredicate(INTERNAL_PARENT_PROP,
                                           Query.FilterOperator.EQUAL,
                                           key));
    // TODO(pmy): just .setKeysOnly(); ?
    return entitiesToJson(service.prepare(q).asList(withLimit(10)));
  }

  @Override
  public JSONObject retrieve(Path path, User user) {
    Key key = path.toKey();
    logger.fine("service.get: " + key);
    // TODO(pmy): remove this try/catch when ACLs check is actually
    // performed, as it will throw for missing..
    try {
      return entityToJson(service.get(key));
    } catch (EntityNotFoundException e) {
      throw new NotFoundException(path);
    }
  }

  /** Named create uses this path. */
  @Override
  public void update(Path path, JSONObject json, User user) {
    service.put(jsonToEntity(path, json));
  }

  @Override
  public JSONObject search(Path path, String query, User user) {
    return search(path, query, 0, DEFAULT_LIMIT, null, null, null, DURATION_UNDEFINED, user);
  }

  @Override
  public JSONObject search(Path path,
                           String query,
                           int offset, int limit,
                           String [] fields, int [] order,
                           String endpointId, long duration,
                           User user) {
    return entitiesToJson(service.prepare(parseQuery(path, query))
                          .asQueryResultList(FetchOptions.Builder
                                             .withOffset(offset)
                                             .limit(limit)));
  }

  // Helpers.

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

  void assertExists(Path path) {
    try {
      service.get(path.toKey());
    } catch (EntityNotFoundException e) {
      throw new NotFoundException(path);
    }
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
    setProperties((PropertyContainer) entity, json);
    Key key = entity.getKey();
    Key parent = key.getParent();
    // TODO(pmy): parent may be null: does this matter?
    entity.setProperty(INTERNAL_PARENT_PROP, parent);
    // TODO(pmy): search
    //entity.setProperty(Search.INTERNAL_QUERY_FIELD_PATH,
    // Search.makePathTokens(Path.fromKey(key)));
    return entity;
  }

  /** Recursive callee of setProperties(Entity, JSONObject). */
  static PropertyContainer setProperties(final PropertyContainer entity, JSONObject json) {
    Util.visitJson(json, new Util.Visitor() {
        void visit(String key, Object val) {
          // Datastore will complain about storing JSONObject.NULL, so
          // explicitly convert to java NULL.  JSONObject will convert
          // back to json's null type on the way back out.
          if (val == JSONObject.NULL) {
            entity.setProperty(key, null);
          } else {
            entity.setProperty(key, val);
          }
        }
        void visit(String key, Boolean val) { entity.setProperty(key, val); }
        void visit(String key, Double val) { entity.setProperty(key, val); }
        void visit(String key, Integer val) { entity.setProperty(key, val); }
        void visit(String key, Long val) { entity.setProperty(key, val); }
        void visit(String key, String val) { entity.setProperty(key, val); }
        void visit(String key, JSONObject val) {
          EmbeddedEntity embedded = new EmbeddedEntity();
          setProperties(embedded, val);
          entity.setProperty(key, embedded);
        }
        void visit(String key, JSONArray val) {
          Collection<Object> objs = new ArrayList<Object>(val.length());
          for (int i = 0; i < val.length(); i++) {
            Object arrVal;
            try {
              arrVal = val.get(i);
            } catch(JSONException e) {
              throw new ArrayIndexOutOfBoundsException("" + e);
            }
            if (arrVal instanceof JSONObject) {
              EmbeddedEntity embedded = new EmbeddedEntity();
              setProperties(embedded, (JSONObject) arrVal);
              objs.add(embedded);
            } else {
              objs.add(arrVal);
            }
          }
          entity.setProperty(key, objs);
        }
      });
    return entity;
  }

  public static JSONObject entitiesToJson(Iterable<Entity> entities) {
    JSONObject json = new JSONObject();
    for (Entity e : entities) {
      Util.jsonPut(json, Path.fromKey(e.getKey()).getFilename(), entityToJson(e));
    }
    return json;
  }

  public static JSONObject entityToJson(PropertyContainer entity) {
    JSONObject json = new JSONObject();
    for (java.util.Map.Entry<String, Object> property : entity.getProperties().entrySet()) {
      String keyName = property.getKey();
      if (keyName.equals(INTERNAL_PARENT_PROP)
          || keyName.startsWith(Search.INTERNAL_QUERY_FIELD_PATH)) {
        continue;
      }
      Object val = property.getValue();
      if (keyName == null) {
        throw new NullPointerException("json.org's JSON doesn't allow null keys.");
      }
      if (val == null) {
        val = JSONObject.NULL;
      } else if (val instanceof EmbeddedEntity) {
        val = entityToJson((EmbeddedEntity) val);
      } else if (val instanceof Collection) {
        JSONArray arr = new JSONArray();
        int i = 0;
        for (Object o : (Collection) val) {
          if (o instanceof EmbeddedEntity) {
            o = entityToJson((EmbeddedEntity) o);
          }
          try {
            arr.put(i++, o);
          } catch (JSONException e) {
            // "if the value is a non-finite number." - json.org
            throw new RuntimeException(e);
          }
        }
        val = arr;
      } else if (val instanceof com.google.appengine.api.datastore.Text) {
        val = ((com.google.appengine.api.datastore.Text) val).getValue();
      }
      try {
        json.put(keyName, val);
      } catch (JSONException e) {
        // "if the value is a non-finite number." - json.org
        throw new RuntimeException(e);
      }
    }
    return json;
  }
}
