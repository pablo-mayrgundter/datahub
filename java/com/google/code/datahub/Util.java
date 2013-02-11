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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utilities for streams and JSON.
 *
 * @author Pablo Mayrgundter
 */
public final class Util {

  /**
   * Visitor that recognized the primitives json.org's JSONObject
   * supports.  The default handling is to call visit(String, Object)
   * for all types.
   *
   * @see #visitJson(JSONObject, Visitor)
   * @see http://www.json.org/javadoc/org/json/JSONObject.html
   */
  public static abstract class Visitor {
    abstract void visit(String key, Object val);
    void visit(String key, Boolean val) { visit(key, (Object) val); }
    void visit(String key, Double val) { visit(key, (Object) val); }
    void visit(String key, Integer val) { visit(key, (Object) val); }
    void visit(String key, JSONArray val) { visit(key, (Object) val); }
    void visit(String key, JSONObject val) { visit(key, (Object) val); }
    void visit(String key, Long val) { visit(key, (Object) val); }
    void visit(String key, String val) { visit(key, (Object) val); }
  }

  /**
   * vistor.visit will be called for each key,val field of the given
   * json object.
   */
  public static void visitJson(JSONObject obj, Visitor visitor) {
    java.util.Iterator itr = obj.keys();
    while (itr.hasNext()) {
      String key = (String) itr.next();
      Object val;
      try {
        val = obj.get(key);
      } catch (JSONException e) {
        // This is only possible if the object is being modified while
        // we iterate its keys.
        throw new java.util.ConcurrentModificationException(e.getMessage());
      }
      if (val instanceof Boolean) {
        visitor.visit(key, (Boolean) val);
      } else if (val instanceof Double) {
        visitor.visit(key, (Double) val);
      } else if (val instanceof Integer) {
        visitor.visit(key, (Integer) val);
      } else if (val instanceof JSONArray) {
        visitor.visit(key, (JSONArray) val);
      } else if (val instanceof JSONObject) {
        visitor.visit(key, (JSONObject) val);
      } else if (val instanceof Long) {
        visitor.visit(key, (Long) val);
      } else if (val instanceof String) {
        visitor.visit(key, (String) val);
      } else {
        visitor.visit(key, val);
      }
    }
  }

  /**
   * Wrapper to convert JSONObject.put calls from checked to unchecked
   * exception handling, and return the json object for chaining.
   *
   * @throws IllegalArgumentException if JSONObject.put throws JSONException.
   * @throws NullPointerException if fieldName is null.
   */
  public static JSONObject jsonPut(JSONObject obj, String fieldName, Object val) {
    if (fieldName == null) {
      throw new NullPointerException("JSONObject doesn't allow null keys.");
    }
    try {
      obj.put(fieldName, val);
    } catch (JSONException e) {
      throw new IllegalArgumentException(e);
    }
    return obj;
  }

  public static JSONObject jsonPut(JSONObject obj, String fieldName, boolean val) {
    if (fieldName == null) {
      throw new NullPointerException("JSONObject doesn't allow null keys.");
    }
    try {
      obj.put(fieldName, val);
    } catch (JSONException e) {
      throw new Error("JSON library is not behaving as expected.", e);
    }
    return obj;
  }

  /**
   * Wrapper to convert JSONObject.get call to return null instead of
   * throw exception on missing value.
   *
   * @throws NullPointerException if fieldName is null.
   */
  public static JSONObject jsonGet(JSONObject obj, String fieldName) {
    if (fieldName == null) {
      throw new NullPointerException("JSONObject doesn't allow null keys.");
    }
    try {
      return obj.getJSONObject(fieldName);
    } catch (JSONException e) {
      return null;
    }
  }

  /**
   * Append the value to the array at the given object fieldName.
   *
   * @throws IllegalArgumentException if JSONObject.put throws JSONException.
   */
  public static JSONObject jsonAppend(JSONObject obj, String fieldName, Object val) {
    try {
      obj.append(fieldName, val);
    } catch (JSONException e) {
      throw new IllegalArgumentException(e);
    }
    return obj;
  }

  public static JSONArray jsonObjectValues(JSONObject obj) {
    try {
      return obj.toJSONArray(obj.names());
    } catch (JSONException e) {
      throw new Error("JSON library is not behaving as expected.", e);
    }
  }

  public static String jsonPretty(JSONObject obj) {
    try {
      return obj.toString(2);
    } catch (JSONException e) {
      return obj.toString();
    }
  }


  // TODO(pmy): Can't directly send array:
  //   https://github.com/angular/angular.js/issues/334
  public static JSONArray stringsToJson(Iterable<String> strings) {
    JSONArray arr = new JSONArray();
    for (String s : strings) {
      JSONObject obj = new JSONObject();
      jsonPut(obj, "str", s);
      arr.put(obj);
    }
    return arr;
  }

  public static JSONObject jsonStrToObj(String jsonStr) {
    try {
      return new JSONObject(jsonStr);
    } catch (JSONException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
