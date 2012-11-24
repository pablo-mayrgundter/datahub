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

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.prospectivesearch.ProspectiveSearchServiceFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The MatchResponseServlet receives matches and stores them for later
 * retrival.
 *
 * @author Pablo Mayrgundter
 */
public class MatchResponseServlet extends AbstractServlet {

  static final long serialVersionUID = 6406691785085268942L;

  static final Logger logger = Logger.getLogger(MatchResponseServlet.class.getName());

  /**
   * Handle Prospective Search match callbacks.  Use Channel API to
   * synchronously send matching entity to matching query endpoints.
   */
  @Override
  public void doPost(final HttpServletRequest req, final HttpServletResponse rsp)
      throws ServletException, IOException {
    int reqResultsOffset = paramToInt("results_offset");
    int reqResultsCount = paramToInt("results_count");
    if (!paramsOk("Post to matches must specify results_offset and results_count parameters.",
                  rsp)) {
      return;
    }
    String [] reqQueryIds = req.getParameterValues("id");
    logger.fine("MatchResponseServlet: doPost: reqQueryIds: "
                + (java.util.Arrays.toString(reqQueryIds)));
    if (reqQueryIds == null || reqQueryIds.length == 0) {
      badRequest("Post to matches without a valid list of matching ids: " + reqQueryIds,
                 rsp);
      return;
    }

    // Plumbing for the next few lines: recode the match from req to
    // proto to entity to json.
    Entity matchedEntity =
      ProspectiveSearchServiceFactory.getProspectiveSearchService().getDocument(req);
    if (matchedEntity == null) {
      badRequest("Must submit entity for match.", rsp);
      return;
    }
    JSONObject matchedObject = Datastore.entityToJson(matchedEntity);
    Path matchPath = Path.fromKey(matchedEntity.getKey());
    Util.jsonPut(matchedObject, "path", matchPath);

    // Setup the lists of queries to notify.  Each client could have
    // multiple queries that matched this object.
    Map<String, List<QueryId>> queryIdMatchesByEndpoints =
        new HashMap<String, List<QueryId>>();
    for (String reqQueryId : reqQueryIds) {
      QueryId queryId = QueryId.fromString(reqQueryId);
      List<QueryId> matchingQueryIds = queryIdMatchesByEndpoints.get(queryId.endpointId);
      if (matchingQueryIds == null) {
        queryIdMatchesByEndpoints.put(queryId.endpointId,
                                      matchingQueryIds = new ArrayList<QueryId>());
      }
      matchingQueryIds.add(queryId);
    }

    logger.fine(
        String.format("MatchResponseServlet: doPost: queryIds(%s), matchedObject(%s)\n",
                      java.util.Arrays.asList(reqQueryIds), matchedObject));

    for (Map.Entry<String, List<QueryId>> entry : queryIdMatchesByEndpoints.entrySet()) {
      String endpointId = entry.getKey();
      List<QueryId> queryIds = entry.getValue();

      JSONObject matchMsg = new JSONObject();
      Util.jsonPut(matchMsg, "object", matchedObject);
      Util.jsonPut(matchMsg, "queryIds", new JSONArray(queryIds));
      logger.fine("MatchResponseServlet: delivering json: " + matchMsg);

      ChannelServiceFactory.getChannelService()
          .sendMessage(new ChannelMessage(endpointId, "(" + matchMsg + ")"));
    }
  }
}