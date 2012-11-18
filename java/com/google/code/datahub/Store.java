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

/**
 * The Store interface defines common persistence methods for JSON
 * objects, according to Create, Retrieve, Update, Delete, Search
 * (CRUD+S) semantics.
 *
 * All methods on a path will throw NotFoundException if the given
 * path does not exist.
 *
 * @author Pablo Mayrgundter <pmy@google.com>
 */
public interface Store {

  /**
   * Thrown when the ACL for a given resource restricts the operation
   * for the requesting user.
   */
  public static class OperationRestrictedException extends SecurityException {
    OperationRestrictedException(Path path, User user, Op op) {
      super(String.format("path(%s) restricts user(%s) operation(%s)", path, user, op));
    }
  }

  /**
   * Thrown when a specified resource doesn't exist in the store.
   */
  public static class NotFoundException extends RuntimeException {
    NotFoundException(Path path) {
      super(path.toString());
    }
  }

  /**
   * Operators supported by a store.
   */
  static enum Op {
    CREATE(1), READ(2), UPDATE(3), DELETE(4);
    final int ord;
    Op(int ord) {
      this.ord = ord;
    }
  }

  /** The default number of of results to retrieve. */
  static final int DEFAULT_LIMIT = 10;

  /** The duration value to use to signal no persistent search. */
  static final int DURATION_UNDEFINED = -1;

  /** The duration value to use to signal unlimited duration. */
  static final int DURATION_UNLIMITED = 0;

  /** Max duration in seconds for persistent search. */
  static final long MAX_DURATION = 3600;

  /**
   * Stores a new entity with the given JSONObject properites as a
   * child of the given parent path.
   *
   * @return the path of the new entity, which will be composed of the
   * given parent path and a sub-path allocated by the parent
   * resource.
   */
  Path create(Path path, JSONObject obj, User user);

  /**
   * Stores a new entity with the given name and JSONObject properites
   * as a child of the given parent path.
   *
   * @return the path of the new entity, which is determined by the
   * given path and name.
   */
  Path create(Path path, String name, JSONObject obj, User user);

  /** Deletes the resources at the given paths. */
  void delete(User user, Path ... paths);

  /** Deletes the index associated with this store. */
  void deleteIndexes(Path path, User user);

  /** Deletes the queries with the given ids. */
  void deleteQueries(User user, String ... ids);

  /**
   * Lists the indexes associated with this search corpus from
   * api.search or api.prospectivesearch.
   *
   * @return a map of index type to index name.  Index types are API
   * Package names.
   */
  JSONObject getIndexMap(Path path, User user);

  /** @return the object or null if not found. */
  JSONObject retrieve(Path path, User user);

  /**
   * Retrieve the (possibly empty) collection of elements at the given
   * path prefix.
   *
   * @return A map of paths to collection elements.
   */
  JSONObject retrieve(Path path,
                      int offset, int limit, String [] fields, int [] order,
                      String endpointId, long duration,
                      User user);

  /**
   * Retrieve the persistent query with the given id.
   */
  JSONObject retrieveQuery(String id, User user);

  /**
   * Retrieve persistent queries by the given user.
   *
   * Equivalent to retrieveQueries(user, null, Integer.MAX_VALUE, 0);
   */
  JSONObject retrieveQueries(User user);

  /**
   * Retrieve persistent queries by the given user with constraints.
   *
   * @param path Restrict the results for queries to this path and its
   * subpaths.  May be null.
   * @param limit limits the number of queries to return.
   * @param expiresBefore Limits the returned subscriptions to those
   * that expire before the given time in seconds since epoch, or 0
   * for no expiration.
   * @return A map of query IDs to query properties.
   */
  JSONObject retrieveQueries(User user, Path path,
                             int limit, long expiresBefore);

  /**
   * Equivalent to the expanded search call with defaults,
   * i.e. search(path, query, 0, DEFAULT_LIMIT, null, null, null,
   * DURATION_UNDEFINED).
   */
  JSONObject search(Path path, String query, User user);

  /**
   * The given path is interpreted as a collection, and is used to
   * query for items of sub-kinds.
   *
   * TODO(pmy): is this correct?  How do path and query interact?
   * Maybe path is root and query is relative?  Query example: support
   * for XPaths.
   *
   * @param offset this many items into the matching items for the
   * return list.
   * @param limit how many items to return after the offset.
   * @param fields list of string field names to return.  May be null
   * to return all fields.
   * @param order Positive, Negative or Zero for ascending, descending
   * or unspecified order. TODO(pmy): Maybe generalize to "filtering",
   * e.g. to allow an exclusion of corresponding field, for efficient
   * inverses?
   * @param endpointId unique endpoint identifier which will be used
   * to synthesize a subscription to updated search results, or null
   * to not create a persistent update subscription.
   * @param the duration of the search update subscription.  May be a
   * postitive number, 0 for unlimited duration, or DURATION_UNDEFINED
   * to not create a persistent update subscription.  This value will
   * be limited by the server to MAX_DURATION.
   */
  JSONObject search(Path path, String query,
                    int offset, int limit, String [] fields, int [] order,
                    String endpointId, long duration,
                    User user);

  /**
   * Stores an entity with the given JSONObject properites at the
   * given path.
   */
  void update(Path path, JSONObject obj, User user);
}
