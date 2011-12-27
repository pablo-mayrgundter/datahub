package hub;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Reader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
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
      str.append(buf, 0, len);
    }
    return str.toString();
  }

  // Entity factories.

  static void entitiesFromJsonReader(Key parentKey, Resource rsrc, Reader r, List<Entity> entities) throws IOException {
    entitiesFromJsonStr(parentKey, rsrc, readFully(r), entities);
  }

  static void entitiesFromJsonStr(Key parentKey, Resource rsrc, String jsonStr, List<Entity> entities) {
    entitiesFromJson(parentKey, rsrc, jsonStrToObj(jsonStr), entities);
  }

  static void entitiesFromJson(Key parentKey, Resource rsrc, JSONObject json, List<Entity> entities) {
    Entity entity = rsrc.hasId() ?
      new Entity(rsrc.getDir(), rsrc.getId(), parentKey)
      : new Entity(rsrc.getDir(), parentKey);
    Iterator itr = json.keys();
    try {
      while (itr.hasNext()) {
        String key = (String) itr.next();
        Object val = json.get(key);
        if (val instanceof JSONObject) {
          System.err.println("Found nested JSON object (and will recursively parse it) at key:"
                             + key + ", object is: " + val);
          int childNdx = entities.size();
          entitiesFromJson(parentKey, new Resource(key), (JSONObject) val, entities);
          Entity child = entities.get(childNdx);
          entities.add(child);
          val = child.getKey();
        }
        entity.setProperty(key, val);
      }
    } catch (JSONException e) {
      throw new IllegalArgumentException(e);
    }
    entities.add(entity);
  }

  // JSON factories.

  static JSONObject jsonStrToObj(String jsonStr) {
    System.err.println("JSON STR: " + jsonStr);
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