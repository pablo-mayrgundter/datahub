package hub;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

  public void doGet(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException {
    InputStream is = req.getInputStream();
    System.out.println("avail: "+ is.available());
    System.out.println("read: "+ is.read(new byte[10], 0, 10));
  }

  public void doPost(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException {
    InputStream is = req.getInputStream();
    System.out.println("avail: "+ is.available());
    System.out.println("read: "+ is.read(new byte[10], 0, 10));
  }
}