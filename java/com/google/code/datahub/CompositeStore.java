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
 * The CompositeStore class uses the datastore as the primary object
 * storage, for direct object retrieval and also as the only source of
 * hierarchical path information and permissions.  The search store is
 * used for queries and flexible collection retrival.
 *
 * All operations on objects will first be submitted to the datastore
 * and then to the search store, so that permissions failures will
 * fail fast without search store modification.
 *
 * TODO(pmy): transaction support?
 *
 * @author Pablo Mayrgundter <pmy@google.com>
 */
public class CompositeStore extends AbstractStore {

  public final Store datastore;
  public final Datastore datastoreAsAclService;
  public final Store search;

  /**
   * @param parentCorpusPath may be null.
   */
  CompositeStore(Path corpusPath, Path parentCorpusPath) {
    datastoreAsAclService = new Datastore();
    datastore = datastoreAsAclService;
    search = new Search(corpusPath, parentCorpusPath);
  }

  @Override
  public Path create(Path parentPath, JSONObject json, User user) {
    Path path = datastore.create(parentPath, json, user);
    search.create(path, json, user);
    return path;
  }

  @Override
  public Path create(Path parentPath, String name, JSONObject json, User user) {
    Path path = datastore.create(parentPath, name, json, user);
    search.create(path, name, json, user);
    return path;
  }

  /** TODO(pmy): delete sub-paths? */
  @Override
  public void delete(User user, Path ... paths) {
    datastore.delete(user, paths);
    search.delete(user, paths);
  }

  @Override
  public void deleteIndexes(Path path, User user) {
    // TODO(pmy): include datastore indexes.
    if (!user.isAdmin()) {
      throw new OperationRestrictedException(path, user, Op.DELETE);
    }
    search.deleteIndexes(path, user);
  }

  @Override
  public void deleteQueries(User user, String ... queryIds) {
    search.deleteQueries(user, queryIds);
  }

  @Override
  public JSONObject getIndexMap(Path path, User user) {
    // TODO(pmy): include datastore indexes.
    // TODO(pmy): datastoreAsAclService.check(new Path("/"), user, Datastore.Op.READ);
    if (!user.isAdmin()) {
      throw new OperationRestrictedException(path, user, Op.READ);
    }
    return search.getIndexMap(path, user);
  }

  @Override
  public JSONObject list(Path path,
                         int offset, int limit, String [] fields, int [] order,
                         String endpointId, long duration,
                         User user) {
    return datastore.list(path, offset, limit, fields, order,
                          endpointId, duration, user);
  }

  @Override
  public JSONObject retrieve(Path path, User user) {
    return datastore.retrieve(path, user);
  }

  @Override
  public JSONObject retrieveQueries(User user, Path path, int limit, long expiresBefore) {
    datastoreAsAclService.check(path, user, Datastore.Op.READ);
    return search.retrieveQueries(user, path, limit, expiresBefore);
  }

  @Override
  public JSONObject search(Path path, String query,
                           int offset, int limit, String [] fields, int [] order,
                           String endpointId, long duration,
                           User user) {
    datastoreAsAclService.check(path, user, Datastore.Op.READ);
    return search.search(path, query, offset, limit, fields, order, endpointId, duration, user);
  }

  /** TODO(pmy): updates applied to sub-paths? */
  @Override
  public void update(Path path, JSONObject json, User user) {
    datastore.update(path, json, user);
    //search.update(path, json, user);
  }
}
