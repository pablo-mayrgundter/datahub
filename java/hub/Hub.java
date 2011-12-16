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
    String uriPath = req.getRequestURI();
    String objPath = uriPath.substring(srvPath.length());
    String [] parts = objPath.split("/");
    String jsonStr;

    String dir = parts[1];
    if (parts.length == 2) {
      jsonStr = store.list(dir);
    } else if (parts.length == 3) {
      long id = Long.parseLong(parts[2]);
      try {
        jsonStr = store.get(dir, id);
      } catch (IllegalArgumentException e) {
        rsp.setStatus(rsp.SC_BAD_REQUEST);
        rsp.getWriter().println(e);
        return;
      }
    } else {
      rsp.setStatus(rsp.SC_BAD_REQUEST);
      rsp.getWriter().println("bad object path: "+ objPath);
      return;
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
    String uriPath = req.getRequestURI();
    String idPath = uriPath.substring(srvPath.length());
    String [] parts = idPath.split("/");
    if (parts.length != 2) {
      // TODO(pablo): Is 404 correct here?
      rsp.setStatus(rsp.SC_NOT_FOUND);
      return;
    }

    String kind = parts[1];
    rsp.setStatus(rsp.SC_CREATED);
    String jsonStr = Util.readFully(req.getReader());
    String newObjPath = srvPath + "/" + store.save(kind, jsonStr);
    rsp.setHeader("Location", newObjPath);
  }

  //public void doPut(HttpServletRequest req, HttpServletResponse rsp)
  //  throws ServletException, IOException {
  //}
}