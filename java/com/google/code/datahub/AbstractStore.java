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
 * The AbstractStore class is a base implementation of the Store
 * methods to simplify extension.
 *
 * @author Pablo Mayrgundter <pmy@google.com>
 */
abstract class AbstractStore implements Store {

  @Override
  public void deleteIndexes(Path path, User user) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteQueries(User user, String ... ids) {
    throw new UnsupportedOperationException();
  }

  @Override
  public JSONObject getIndexMap(Path path, User user) {
    throw new UnsupportedOperationException();
  }

  @Override
  public JSONObject retrieveQuery(String id, User user) {
    throw new UnsupportedOperationException();
  }

  @Override
  public JSONObject retrieveQueries(User user) {
    return retrieveQueries(user, null, Integer.MAX_VALUE, 0);
  }

  @Override
  public JSONObject retrieveQueries(User user, Path path, int limit, long expiresBefore) {
    throw new UnsupportedOperationException();
  }

  @Override
  public JSONObject search(Path path, String query, User user) {
    return search(path, query, 0, DEFAULT_LIMIT, null, null, null, DURATION_UNDEFINED,
                  user);
  }

  @Override
  public void update(Path path, final JSONObject objUpdates, User user) {
    create(path, objUpdates, user);
  }
}
