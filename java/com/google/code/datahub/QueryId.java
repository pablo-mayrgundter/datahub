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

/**
 * The QueryId class is an adaptor to map from (endpoint,topic,query)
 * for prospectivesearch subscription IDs.
 *
 * @author Pablo Mayrgundter <pmy@google.com>
 */
class QueryId {

  // TODO(pmy): escape delimiter in the code below to preclude
  // collisions.
  static final String DELIM = "-->8--";

  final String endpointId;
  final String topic;
  final String query;

  QueryId(String endpointId, String topic, String query) {
    this.endpointId = endpointId;
    this.topic = topic;
    this.query = query;
  }

  public String toString() {
    return endpointPrefix(endpointId) + topic + DELIM + query;
  }

  static QueryId fromString(String internalQueryid) {
    String [] parts = internalQueryid.split(DELIM);
    if (parts.length != 3 && parts.length != 2) {
      // TODO(pmy): security
      throw new IllegalStateException("Invalid query Id: " + internalQueryid);
    }
    return new QueryId(parts[0], parts[1], parts.length == 2 ? "" : parts[2]);
  }

  /**
   * The returned string can is used to search for all queries
   * including and lexicographically after this prefix.
   */
  static String endpointPrefix(String endpointId) {
    return endpointId + DELIM;
  }
}
