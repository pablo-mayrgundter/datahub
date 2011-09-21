package hub;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Reader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Utility factories for Entities and JSON object and arrays.
 * 
 * @author Pablo Mayrgundter
 */
final class Util {

  // Stream utils.

  static String readFully(Reader r) throws IOException {
    StringBuffer str = new StringBuffer();
    char [] buf = new char[200];
    int len;
    while ((len = r.read(buf)) != -1) {
      str.append(buf);
    }
    return str.toString();
  }

  // Entity factories.

  static Entity entityFromJsonReader(String type, Reader r) throws IOException {
    return entityFromJsonStr(type, readFully(r));
  }

  static Entity entityFromJsonStr(String type, String jsonStr) {
    return entityFromJson(type, jsonStrToObj(jsonStr));
  }

  static Entity entityFromJson(String type, JSONObject json) {
    Entity entity = new Entity(type);
    Iterator itr = json.keys();
    try {
      while (itr.hasNext()) {
        String key = (String) itr.next();
        entity.setProperty(key, json.get(key));
      }
    } catch (JSONException e) {
      throw new IllegalArgumentException(e);
    }
    return entity;
  }

  // JSON factories.

  static JSONObject jsonStrToObj(String jsonStr) {
    try {
      return new JSONObject(jsonStr);
    } catch (JSONException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Wrapper to convert JSONObject.put calls from checked to unchecked
   * exception handling, and return the json object for chaining.
   */
  static JSONObject jsonPut(JSONObject json, String field, Object val) {
    try {
      json.put(field, val);
    } catch (JSONException e) {
      throw new IllegalArgumentException(e);
    }
    return json;
  }

  // Can't directly send array:
  //   https://github.com/angular/angular.js/issues/334
  static JSONArray stringsToJson(Iterable<String> strings) {
    JSONArray json = new JSONArray();
    for (String s : strings) {
      JSONObject obj = new JSONObject();
      jsonPut(obj, "str", s);
      json.put(obj);
    }
    return json;
  }

  static JSONArray entitiesToJson(Iterable<Entity> entities) {
    JSONArray json = new JSONArray();
    for (Entity e : entities) {
      json.put(entityToJson(e));
    }
    return json;
  }

  static JSONObject entityToJson(Entity entity) {
    JSONObject json = new JSONObject();
    for (Map.Entry<String, Object> property : entity.getProperties().entrySet()) {
      Object val = property.getValue();
      if (val instanceof Text) {
        val = ((Text) val).getValue();
      }
      jsonPut(json, property.getKey(), val);
    }
    return json;
  }
}