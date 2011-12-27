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

  Store store;

  public void init() {
    store = new Store();
  }

  public void doGet(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException {
    String srvPath = req.getServletPath();
    Resource rsrc = new Resource(req);
    String jsonStr;

    if (!rsrc.hasId()) {
      jsonStr = store.list(rsrc);
    } else {
      try {
        jsonStr = store.get(rsrc);
      } catch (IllegalArgumentException e) {
        rsp.setStatus(rsp.SC_BAD_REQUEST);
        rsp.getWriter().println(e);
        return;
      }
    }
    if (jsonStr == null) {
      rsp.setStatus(rsp.SC_NOT_FOUND);
    } else {
      rsp.getWriter().write(jsonStr);
    }
  }

  public void doPost(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException {
    String srvPath = req.getServletPath();
    Resource rsrc = new Resource(req);
    if (rsrc.hasId()) {
      // TODO(pablo): Is 404 correct w.r.t. REST here?
      rsp.setStatus(rsp.SC_NOT_FOUND);
      return;
    }

    rsp.setStatus(rsp.SC_CREATED);
    String jsonStr = Util.readFully(req.getReader());
    String newObjPath = srvPath + "/" + store.save(rsrc, jsonStr);
    rsp.setHeader("Location", newObjPath);
  }

  public void doPut(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException {
    Resource rsrc = new Resource(req);
    if (!rsrc.hasId()) {
      rsp.setStatus(rsp.SC_BAD_REQUEST);
      return;
    }

    store.save(rsrc, Util.readFully(req.getReader()));
    rsp.setStatus(rsp.SC_OK);
  }
}