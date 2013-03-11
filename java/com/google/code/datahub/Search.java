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

import static com.google.appengine.api.prospectivesearch.FieldType.*;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.prospectivesearch.FieldType;
import com.google.appengine.api.prospectivesearch.ProspectiveSearchService;
import com.google.appengine.api.prospectivesearch.Subscription;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.ListRequest;
import com.google.appengine.api.search.Query;
import com.google.appengine.api.search.QueryOptions;
import com.google.appengine.api.search.Results;
import com.google.appengine.api.search.ScoredDocument;
import com.google.appengine.api.search.SearchServiceFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * JSON api combining to api.search and api.prospectivesearch.
 *
 * TODO(pmy): replace with, or promote to, a unified search API.
 *
 * TODO(pmy): User are required arguments for Store methods, but are
 * currently ignored by the Search system as they're expected to be
 * enforced in CompositeStore before calls to Search.
 *
 * @author pmy@google.com (Pablo Mayrgundter)
 */
public class Search extends AbstractStore {

  protected static final Logger logger = Logger.getLogger(Search.class.getName());

  // The following variables define a coordinated set of special
  // encodings for internal access and internal state for the combined
  // api.search and api.prospectivesearch API.  These are not
  // particularly elegant and the subsystems should probably be
  // redesigned to work seamlessly together.  Use only
  // double-underscores here, not single.  TODO(pmy): Use of
  // double-underscores here is coordinated with escaping rules in
  // Path.
  private static final String INTERNAL_PREFIX = "INTERNAL__";

  /**
   * " " (0x20) is used to prefix this query to move it before user
   * queries in listings.
   */
  private static final String INTERNAL_DEFAULT_SUBID = " " + INTERNAL_PREFIX + "DEFAULT_SUBID";
  private static final String INTERNAL_QUERY_SUBID_PREFIX = INTERNAL_PREFIX + "QUERY__SUBID__";
  private static final String INTERNAL_QUERY_FIELD_PREFIX = INTERNAL_PREFIX + "QUERY__FIELD__";
  static final String INTERNAL_QUERY_FIELD_PATH = INTERNAL_QUERY_FIELD_PREFIX + "path";

  /**
   * TODO(pmy): I think this is being used as an always-satisfied
   * query and as an empty partial query.
   */
  static final String EMPTY_QUERY = "";

  /**
   * An non-trivial unsatisfiable query is created with the
   * conjunction of a complex predicate and its negation.  A trivial
   * query, such as the conjunction of a simple predicate and its
   * negation would be optimized away.
   */
  static final String UNSATISFIABLE_QUERY = "(a OR b) AND NOT (a OR b)";

  /**
   * Wrapper class for ProspectiveSearchService to encapsulate topic
  * reference.
   */
  static class ProspectiveSearchIndex {
    static final ProspectiveSearchService pss =
        com.google.appengine.api.prospectivesearch.ProspectiveSearchServiceFactory
        .getProspectiveSearchService();

    static List<String> listTopics() {
      return pss.listTopics("", 100);
    }

    final String topic;
    private final QueryId defaultQueryId;

    ProspectiveSearchIndex(Path path) {
      topic = path.toDocId();
      SchemaManager.initSchemaForPsi(path);
      defaultQueryId = new QueryId(INTERNAL_DEFAULT_SUBID, topic, UNSATISFIABLE_QUERY);
      pss.subscribe(topic,
                    defaultQueryId.toString(),
                    0,
                    UNSATISFIABLE_QUERY,
                    SchemaManager.getSchemaForPsi(path));
    }

    void add(Entity e) {
      pss.match(e, topic);
    }

    List<Subscription> listQueries() {
      return pss.listSubscriptions(topic, "!", Integer.MAX_VALUE, 0);
    }

    List<Subscription> listQueries(String queryStartId, int limit, long expiresBefore) {
      if (queryStartId.length() == 0 || queryStartId.charAt(0) <= ' ') {
        throw new IllegalArgumentException(
            "Endpoint IDs must start with characters after the space character (0x20).");
      }
      return pss.listSubscriptions(topic, queryStartId, limit, expiresBefore);
    }

    QueryId subscribe(String endpointId, long duration, String query, Map<String, FieldType> schema) {
      if (endpointId.startsWith(" ")) {
        throw new IllegalArgumentException(
            "Endpoint IDs must start with characters after the space character (0x20).");
      }
      QueryId queryId = new QueryId(endpointId, topic, query);
      System.err.printf("ProspectiveSearchIndex: subscribe: topic(%s)\n", topic);
      pss.subscribe(topic, queryId.toString(), duration, query, schema);
      return queryId;
    }

    void unsubscribe(QueryId queryId) {
      System.err.printf("ProspectiveSearchIndex: unsubscribe: topic(%s), queryId(%s)\n",
                        topic, queryId);
      pss.unsubscribe(topic, queryId.toString());
    }
  }

  static final Map<Path, Search> CORPORA_BY_NAME = new LinkedHashMap<Path, Search>();

  final Index docIndex;
  final ProspectiveSearchIndex queryIndex;
  final Path corpusPath;
  final Search parent;

  /**
   * Create a new search store that is rooted at the given corpusPath,
   * which is itself prefixed by the (optional) given
   * parentCorpusPath.  Adding documents to the corpusPath will also
   * add them to the parentCorpusPath, recursively until the root.
   *
   * A static map of path to Search objects is maintained to lookup
   * the parent search object referenced by the given
   * parentCorpusPath, so it is necessary to first construct the
   * parent; otherwise an IllegalArgumentException will be thrown if
   * the referenced parent is not found in the map.
   *
   * @param parentCorpusPath may be null.
   * @throws IllegalArgumentException if the given parentCorpusPath
   * references a Search object that has not yet been constructed.
   */
  public Search(Path corpusPath, Path parentCorpusPath) {
    this.corpusPath = corpusPath;
    Search parent = CORPORA_BY_NAME.get(parentCorpusPath);
    if (parentCorpusPath != null && parent == null) {
      throw new IllegalArgumentException(String.format("No such parent(%s) for corpusPath(%s)\n",
                                                       parentCorpusPath, corpusPath));
    }
    this.parent = parent;
    String indexName = corpusPath.toDocId();
    docIndex = SearchServiceFactory.getSearchService()
          .getIndex(IndexSpec.newBuilder()
                    .setName(indexName));
    queryIndex = new ProspectiveSearchIndex(corpusPath);
    CORPORA_BY_NAME.put(corpusPath, this);
  }

  /**
   * Equivalent to create(new Path(path.toString() + Path.SEP + name), obj);
   *
   * @throws ServiceException to wrap low-level checked exceptions.
   */
  @Override
  public Path create(Path path, String name, JSONObject obj, User user) {
    return create(Path.fromString(path + Path.SEP + name), obj, user);
  }

  /**
   * TODO(pmy): returns the given path instead of the newly allocated
   * child path, so incompatible with the Store interface.
   *
   * @throws ServiceException to wrap low-level checked exceptions.
   */
  @Override
  public Path create(Path path, JSONObject obj, User user) {
    System.out.printf("%s.isParentOf(%s)\n", corpusPath, path);
    if (!corpusPath.isParentOf(path)) {
      throw new IllegalArgumentException(
          String.format("path(%s) must be a sub-path of this corpusPath(%s)",
                        path, corpusPath));
    }
    try {
      create(path, jsonToDocument(path, obj), Datastore.jsonToEntity(path, obj));
    } catch (com.google.appengine.api.search.AddException e) {
      throw new ServiceException(e);
    }
    return path;
  }

  @Override
  public void delete(User user, Path ... paths) {
    String [] docIds = new String[paths.length];
    for (int i = 0; i < paths.length; i++) {
      docIds[i] = paths[i].toDocId();
    }
    docIndex.remove(docIds);
    if (parent != null) {
      parent.delete(user, paths);
    }
  }

  @Override
  public void deleteQueries(User user, String ... queryIds) {
    for (String queryId : queryIds) {
      queryIndex.unsubscribe(QueryId.fromString(queryId));
    }
  }

  /**
   * Adds a scalable delete implementation which is lacking in both
   * api.search or api.prospectivesearch.
   */
  @Override
  public void deleteIndexes(Path path, User user) {

    // TODO(pmy): lots of plumbing here. Backends for both systems
    // should implement efficient delete.

    Iterator<String> allDocIdsItr = new Tasks.IteratorChain<String>() {
      Iterator<String> partialIterator() {
        ListRequest.Builder listReq = ListRequest.newBuilder().setReturningIdsOnly(true);
        if (this.lastIterated != null) {
          listReq.setStartId(this.lastIterated).setIncludeStart(false);
        }
        Iterator<Document> docItr;
        try {
          docItr = docIndex.listDocuments(listReq.build()).getResults().iterator();
        } catch (IllegalArgumentException e) {
          // This happens if there are not any documents indexed.
          return new java.util.ArrayList<String>().iterator();
        }
        return new Tasks.ToStringIterator<Document>(docItr) {
          public String next() {
            return innerItr.next().getId();
          }
        };
      }
    };

    Tasks.Processor deletor = new Tasks.Processor("deleteIndex") {
        void process(String [] docIds) {
          docIndex.remove(docIds);
        }
      };

    final int batchSize = 100;
    Tasks.getInstance().enqueueProcess(allDocIdsItr, batchSize, deletor);

    // Delete non-internal PSI subs for this path.
    // TODO(pmy): split this up into an iterator chain.
    Iterator<String> allQueryIdsItr =
        new Tasks.ToStringIterator<Subscription>(queryIndex.listQueries().iterator()) {
      public String next() {
        return innerItr.next().getId();
      }
    };

    Tasks.Processor psiDeletor = new Tasks.Processor("deleteIndexPsi") {
        void process(String [] queryIds) {
          for (String queryId : queryIds) {
            queryIndex.unsubscribe(QueryId.fromString(queryId));
          }
        }
      };
    Tasks.getInstance().enqueueProcess(allQueryIdsItr, batchSize, psiDeletor);
  }

  @Override
  public JSONObject getIndexMap(Path path, User user) {
    JSONObject idxMap = new JSONObject();
    Util.jsonPut(idxMap, "com.google.appengine.api.search", docIndex.getName());
    Util.jsonPut(idxMap, "com.google.appengine.api.prospectivesearch", queryIndex.topic);
    return idxMap;
  }

  @Override
  public JSONObject list(Path path,
                         int offset, int limit, String [] fields, int [] order,
                         String endpointId, long duration, User user) {
    return search(path, Search.EMPTY_QUERY, offset, limit, fields, order, endpointId, duration,
                  user);
  }

  @Override
  public JSONObject retrieve(Path path, User user) {
    List<Document> listOfOneDoc = docIndex.listDocuments(
        ListRequest.newBuilder().setLimit(1).setStartId(path.toDocId()).build()).getResults();
    return listOfOneDoc.size() == 0 ? null : documentToJson(listOfOneDoc.get(0));
  }

  /** TODO(pmy): fields and order currently ignored. */
  @Override
  public JSONObject search(Path path, String query,
                           int offset, int limit,
                           String [] fields, int [] order,
                           String endpointId, long duration,
                           User user) {

    query = fixupQuery(query);

    if (!query.equals(EMPTY_QUERY)) {
      query = " AND " + query;
    }

    String pathId = path.toDocId();
    query = INTERNAL_QUERY_FIELD_PATH + ":" + pathId + query;

    debug("search: path(%s) pathId(%s) query(%s) duration(%s)", path, pathId, query, duration);

    // Save query before back-fill to prevent gaps.  TODO(pmy): ensure
    // this in backends.
    QueryId queryId = null;
    if (endpointId != null && duration >= 0) {
      // This conversion of corpusPath toDocId requires reversal in
      // MatchResponseServlet for the slash-delimited format expected
      // by the client.  TODO(pmy): shouldn't need to know path/docId
      // here.  Perhaps merge QueryId into ProspectiveSearchIndex.
      String topic = corpusPath.toDocId();
      duration = Math.min(duration, MAX_DURATION);
      // TODO(pmy): this schema will potentially be empty if addQuery
      // is called before any documents have been added to this path.
      // However, fieldless queries will still work, which is
      // sufficient for basic document delivery and keyword matching.
      Map<String, FieldType> schema = SchemaManager.getSchemaForPsi(path);
      // TODO(pmy): redundant? since jsonToDocument already adds this
      // before fts schema is saved.
      debug("search: query(%s) queryId(%s), duration(%s) schema(%s)",
            query, queryId, duration, schema);
      queryId = queryIndex.subscribe(endpointId, duration, query, schema);
    }

    QueryOptions.Builder queryOptions = QueryOptions.newBuilder();
    queryOptions.setOffset(offset);
    queryOptions.setLimit(limit);
    Results<ScoredDocument> results;
    try {
      results = docIndex.search(Query.newBuilder().setOptions(queryOptions.build()).build(query));
    } catch (com.google.appengine.api.search.SearchException e) {
      throw new ServiceException(e);
    }
    JSONObject rspJson = new JSONObject();
    JSONArray resultsJson = new JSONArray();
    for (ScoredDocument doc : results) {
      final JSONObject result = new JSONObject();
      final JSONObject obj = documentToJson(doc);
      Util.jsonPut(result, Path.fromDocId(doc.getId()).toString(), obj);
      resultsJson.put(result);
    }
    Util.jsonPut(rspJson, "results", resultsJson);
    Util.jsonPut(rspJson, "offset", offset);
    Util.jsonPut(rspJson, "limit", Math.min(results.getNumberReturned(), limit));

    if (queryId != null) {
      Util.jsonPut(rspJson, "queryId", queryId);
    }

    return rspJson;
  }

  @Override
  public void update(Path path, final JSONObject objUpdates, User user) {
    throw new UnsupportedOperationException();
  }

  // Helpers

  /**
   * Add to search and prospective and tail-recursively call
   * parent.create.
   */
  void create(Path path, Document objAsDoc, Entity objAsEnt) throws ServiceException {

    debug("create: path(%s), objAsDoc(%s), objAsEnt(%s), corpusPath(%s)",
          path, objAsDoc, objAsEnt, corpusPath);

    docIndex.add(objAsDoc);

    // Update schema at this path to include the fields of the given
    // doc.
    SchemaManager.updateSchema(path, objAsDoc);

    try {
      queryIndex.add(objAsEnt);
    } catch (RuntimeException e) {
      // TODO(pmy):
      e.printStackTrace();
      throw new IllegalStateException("FIXME: not sure what leads here.");
    }

    if (parent != null) {
      parent.create(path, objAsDoc, objAsEnt);
    }
  }

  public JSONObject retrieveQueries(User user, String queryIdStart, int limit, long expiration) {
    JSONObject json = new JSONObject();
    for (Subscription sub : queryIndex.listQueries(queryIdStart, limit, expiration)) {
      Util.jsonPut(json, sub.getId(), "\"" + sub + "\"");
    }
    return json;
  }

  // Document/JSON helpers.

  // TODO(pmy): consolidate jsonToEntity mapping with Search and
  // delegate for customziation.

  static Document jsonToDocument(Path path, JSONObject json) {
    final Document.Builder docBuilder = Document.newBuilder();
    Util.visitJson(json, new Util.Visitor() {
        // TODO(pmy): handle different val types.
        public void visit(String key, Object val) {
          docBuilder.addField(Field.newBuilder().setName(key).setText(val.toString()));
        }
      });

    docBuilder.setId(path.toDocId());
    docBuilder.addField(Field.newBuilder().setName(INTERNAL_QUERY_FIELD_PATH)
                        .setText(makePathTokens(path)));
    return docBuilder.build();
  }

  static JSONArray documentsToJson(Iterable<Document> docs) {
    JSONArray json = new JSONArray();
    for (Document doc : docs) {
      json.put(documentToJson(doc));
    }
    return json;
  }

  /**
   * Creates a JSONObject with the same fields as the given document,
   * except internal fields are not mapped.
   */
  static JSONObject documentToJson(Document doc) {
    JSONObject json = new JSONObject();
    for (Field field : doc.getFields()) {
      String fieldName = field.getName();
      if (fieldName.startsWith(INTERNAL_QUERY_FIELD_PATH)) {
        continue;
      }
      Object val = null;
      switch (field.getType()) {
        case TEXT: val = field.getText(); break;
        case HTML: val = field.getHTML(); break;
        case ATOM: val = field.getAtom(); break;
        case DATE: val = field.getDate(); break;
      }
      Util.jsonPut(json, fieldName, val);
    }
    // Order matters.. override any field called "id".
    return json;
  }

  // Misc helpers.
  static String fixupQuery(String query) {
    if (query == null) {
      query = EMPTY_QUERY;
    }

    query = query.trim();
    return query;
  }

  /**
   * Helper to generate parent path prefixes.
   */
  static String makePathTokens(Path path) {
    String toks = "";
    do {
      toks += " " + path.toDocId();
    } while ((path = path.getParent()) != Path.ROOT);
    return toks.trim();
  }

  void debug(String format, Object ... args) {
    logger.fine(this + "." + String.format(format + "\n", args));
  }

  public String toString() {
    return String.format("Search(corpusPath:'%s')", corpusPath);
  }
}
