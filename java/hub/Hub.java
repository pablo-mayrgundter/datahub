package hub;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * The Hub class is the main entry point for the datahub.  It provides
 * RESTful access to Datastore Entities.
 */
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
      jsonStr = entitiesToJSON(datastore.prepare(new Query(parts[1])).asIterable()).toString();
    } else if (parts.length != 3) {
      rsp.setStatus(rsp.SC_BAD_REQUEST);
      rsp.getWriter().println("bad id: "+ id);
      return;
    } else {
      try {
        jsonStr = entityToJSON(datastore.get(KeyFactory.createKey(parts[1], Long.parseLong(parts[2])))).toString();
      } catch (EntityNotFoundException e) {
        rsp.setStatus(rsp.SC_NOT_FOUND);
        return;
      }
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
    JSONObject json;
    try {
      json = new JSONObject(readFully(req.getReader()));
    } catch (JSONException e) {
      System.err.println(e);
      return;
    }
    Entity entity = new Entity(type);
    Iterator itr = json.keys();
    try {
      while (itr.hasNext()) {
        String key = (String) itr.next();
        entity.setProperty(key, json.get(key));
      }
    } catch (JSONException e) {
      System.err.println(e);
      return;
    }
    datastore.put(entity);
    rsp.setStatus(rsp.SC_CREATED);
    Key k = entity.getKey();
    rsp.setHeader("Location", "/" + k.getKind() + "/" + k.getId());
  }

  JSONArray entitiesToJSON(Iterable<Entity> entities) {
    JSONArray jsonArr = new JSONArray();
    for (Entity entity : entities) {
      jsonArr.put(entityToJSON(entity));
    }
    return jsonArr;
  }

  JSONObject entityToJSON(Entity entity) {
    JSONObject json = new JSONObject();
    try {
      for (Map.Entry<String,Object> prop : entity.getProperties().entrySet()) {
        json.put(prop.getKey(), prop.getValue());
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return json;
  }

  String readFully(BufferedReader r) throws IOException {
    StringBuffer str = new StringBuffer();
    char [] buf = new char[200];
    int len;
    while ((len = r.read(buf)) != -1) {
      str.append(buf);
    }
    return str.toString();
  }
}