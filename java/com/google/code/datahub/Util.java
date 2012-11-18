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
 * Utilities for streams and Entity/JSON conversion.
 *
 * @author Pablo Mayrgundter
 */
public final class Util {

  /**
   * @see #visitJson(JSONObject, Visitor)
   */
  public static interface Visitor {
    void visit(String key, String val);
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
      visitor.visit(key, "" + val);
    }
  }

  /**
   * Wrapper to convert JSONObject.put calls from checked to unchecked
   * exception handling, and return the json object for chaining.
   */
  public static JSONObject jsonPut(JSONObject obj, String field, Object val) {
    try {
      obj.put(field, val);
    } catch (JSONException e) {
      throw new IllegalArgumentException(e);
    }
    return obj;
  }

  /**
   * Append the value to the array at the given object field.
   */
  public static JSONObject jsonAppend(JSONObject obj, String field, Object val) {
    try {
      obj.append(field, val);
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
