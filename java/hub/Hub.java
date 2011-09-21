package hub;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The Hub class is the main entry point for the datahub.  It provides
 * RESTful access to Datastore Entities.
 *
 * @author Pablo Mayrgundter
 */
@SuppressWarnings("serial")
public class Hub extends HttpServlet {

  DatastoreService datastore;

  public void init() {
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  public void doGet(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException {
    String srvPath = req.getServletPath();
    String uriPath = req.getRequestURI();
    String id = uriPath.substring(srvPath.length());
    String [] parts = id.split("/");
    String jsonStr;

    if (parts.length == 2) {
      Iterable<Entity> results = datastore.prepare(new Query(parts[1])).asIterable();
      jsonStr = Util.entitiesToJson(results).toString();
    } else if (parts.length != 3) {
      rsp.setStatus(rsp.SC_BAD_REQUEST);
      rsp.getWriter().println("bad id: "+ id);
      return;
    } else {
      Entity entity;
      try {
        entity = datastore.get(KeyFactory.createKey(parts[1], Long.parseLong(parts[2])));
      } catch (EntityNotFoundException e) {
        rsp.setStatus(rsp.SC_NOT_FOUND);
        return;
      }
      jsonStr = Util.entityToJson(entity).toString();
    }
    rsp.getWriter().write(jsonStr);
  }

  public void doPost(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException {
    String srvPath = req.getServletPath();
    String uriPath = req.getRequestURI();
    String idPath = uriPath.substring(srvPath.length());
    String [] parts = idPath.split("/");
    if (parts.length != 2) {
      System.err.println("bad path: "+ idPath);
      return;
    }

    String type = parts[1];
    Entity entity = Util.entityFromJsonReader(type, req.getReader());
    datastore.put(entity);
    rsp.setStatus(rsp.SC_CREATED);
    Key k = entity.getKey();
    rsp.setHeader("Location", srvPath + "/" + k.getKind() + "/" + k.getId());
  }
}