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

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

/**
 * Minimal wrapper for URI paths, e.g. "/a/b/c".
 *
 * @author Pablo Mayrgundter <pmy@google.com>
 */
public class Path {

  static final String SEP = "/";
  static final Pattern REGEX_SPECIAL = Pattern.compile("__(.+)__");
  static final Pattern REGEX_SERIAL = Pattern.compile("\\d+");
  static final String PATH_KIND = "path";
  static final String ROOT_NAME = "ROOT";

  final String [] path;

  Path(String [] path) {
    this.path = path;
  }

  /**
   * Splits the given {@code pathStr} on SEP and stores the parts
   * as the path.
   */
  public Path(String pathStr) {
    if (pathStr == null) {
      throw new NullPointerException("Path string may not be null.");
    }
    if (pathStr.contains("#")) {
      throw new IllegalArgumentException("Path may not contains #");
    }
    pathStr = pathStr.trim();
    if (pathStr.startsWith(SEP)) {
      pathStr = pathStr.substring(1);
    }
    if (pathStr.endsWith(SEP)) {
      pathStr = pathStr.substring(0, pathStr.length() - 1);
    }
    path = pathStr.equals("") ? new String[0] : pathStr.split(SEP);
  }

  static String getRequestURIServletPathRemoved(HttpServletRequest req) {
    String uri = req.getRequestURI();
    String srvPath = req.getServletPath();
    String relPath = uri.substring(srvPath.length());
    System.err.printf(">>> uri(%s) srvPath(%s) relPath(%s)\n", uri, srvPath, relPath);
    return relPath;
  }

  /**
   * Construct a Path object for the given REST-ful request and
   * URI.  The given request URI must have the request servletPath as
   * its prefix.  This prefix is removed from the URI and subsequent
   * characters are interpreted.
   */
  public Path(HttpServletRequest req) {
    this(getRequestURIServletPathRemoved(req));
  }

  // TODO(pmy): handle incomplete keys.
  /**
   * @throws IllegalArgumentException if !key.isComplete().
   */
  public Path(Key key) {
    this(toStringPath(key));
  }

  public Path getParent() {
    if (path.length == 0) {
      return null;
    }
    String [] sub = new String[path.length - 1];
    System.arraycopy(path, 0, sub, 0, sub.length);
    return new Path(sub);
  }

  /**
   * Returns the last path part, or null if this is the root.
   */
  public String getFilename() {
    if (path.length == 0) {
      return null;
    }
    return path[path.length - 1];
  }

  /**
   * True iff this.getFilename() is defined and starts with __.
   */
  public boolean isSpecial() {
    return isSpecial(getFilename());
  }

  boolean isSpecial(String part) {
    if (part != null && part.startsWith("__")) {
      return true;
    }
    return false;
  }

  /**
   * @throws IllegalStateException if the given path part has no
   * matching special value.
   */
  String getSpecial(String part) {
    Matcher m = REGEX_SPECIAL.matcher(part);
    if (m.matches()) {
      return m.group(1);
    }
    throw new IllegalStateException("Given path part has no special encoding: " + part);
  }

  /**
   * Equivalent to isPartSerial(getFilename());
   */
  public boolean isSpecialSerial() {
    return isPartSpecialSerial(getFilename());
  }

  boolean isPartSpecialSerial(String part) {
    if (!isSpecial(part)) {
      return false;
    }
    return REGEX_SERIAL.matcher(getSpecial(part)).matches();
  }

  public boolean isParentOf(Path other) {
    if (path.length >= other.path.length) {
      return false;
    }
    boolean eq = true;
    for (int i = 0; i < path.length; i++) {
      eq &= path[i].equals(other.path[i]);
    }
    return eq;
  }

  /**
   * Converts the given key's inheritance hierarchy to a
   * slash-delimited representation.
   *
   * @throws IllegalArgumentException if the given key.isComplete() returns false.
   */
  static final String toStringPath(Key key) {
    if (!key.isComplete()) {
      throw new IllegalArgumentException("Key is incomplete, cannot create string representation.");
    }
    String path = "";

    do {
      String name = key.getName();
      if (name == null) {
        name = "__" + Long.toString(key.getId()) + "__";
      }

      if (name.equals(ROOT_NAME) && key.getParent() == null) {
        if (path.length() == 0) {
          path = SEP;
        }
        break;
      }

      if (!name.equals(SEP)) {
        path = SEP + name + path;
      }
    } while ((key = key.getParent()) != null);

    return path;
  }

  // TODO(pmy): does the kind/name scheme make sense?
  /**
   * Converts this path to a Key by creating intermediate parent keys
   * for each path element, and linking them together to the last key,
   * which is returned.  Each key's kind is its parent's name.  The
   * root key's name is defined in {@link #ROOT_NAME}
   */
  public Key toKey() {
    // Construct key by defining the first element and then
    // iteratively adding the rest of the path parts.  TODO(pmy):
    // ensure ROOT is escaped/unique; should this be == "ALL"?
    Key key = KeyFactory.createKey(null, PATH_KIND, ROOT_NAME);
    if (path.length == 0) {
      return key;
    }
    for (int i = 0; i < path.length; i++) {
      String part = path[i];
      long id = 0;

      if (isPartSpecialSerial(part)) {
        id = Long.parseLong(getSpecial(part));
      }
      if (id == 0) {
        key = KeyFactory.createKey(key, Path.PATH_KIND, part);
      } else {
        key = KeyFactory.createKey(key, Path.PATH_KIND, id);
      }
    }
    return key;
  }

  /**
   * @return The form "/a/b/c".
   */
  public String toString() {
    if (path.length == 0) {
      return SEP;
    }
    String s = "";
    for (String part : path) {
      s += SEP + part;
    }
    return s;
  }

  public int hashCode() {
    return Arrays.hashCode(path);
  }

  public boolean equals(Object o) {
    if (o instanceof Path) {
      return Arrays.equals(path, ((Path) o).path);
    }
    return false;
  }

  /**
   * Replacment strings for strings disallowed in api.search that are
   * not already escaped by URLEncoder.  The replaced characters are
   * {@code _%.-*+} and the replacements are mnemonic letter for each,
   * prefixed by an underscore.
   */
  static final String [][] REPL_PAIRS = {
    {"_", "_U"},
    {"%", "_P"},
    {"\\.", "_D"},
    {"-", "_M"},
    {"\\*", "_S"},
    {"\\+", "_L"}
  };

  static final String ENC_CHARSET = "UTF-8";

  // TODO(pmy): en/de-coding could be single-pass.

  /**
   * This encoding makes paths usable as index names and document
   * fields in api.search, which accpets alphanum + underscore (though
   * no leading underscore).
   *
   * The Path is converted {@code toString}
   * and the leading slash is replaced with ROOT_NAME. The string is
   * then URL-encoded and all remaining special characters are
   * replaced with safe mnemonics defined in REPL_PAIRS.  The original
   * underscores in the path are replaced first to escape any
   * occurences of the other replacement patterns in the original
   * string.  This is followed by replacement of the other special
   * characters.
   */
  String toDocId() {
    String id = toString();
    if (id.startsWith(SEP)) {
      id = ROOT_NAME + id.substring(SEP.length());
    }
    try {
      id = URLEncoder.encode(id, ENC_CHARSET);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("UTF-8 string encoding not supported.", e);
    }
    for (int i = 0; i < REPL_PAIRS.length; i++) {
      String [] repl = REPL_PAIRS[i];
      id = id.replaceAll(repl[0], repl[1]);
    }
    return id;
  }

  /**
   * The reverse of the encoding process described in
   * {@link toDocId()}.
   */
  static Path fromDocId(String id) {
    for (int i = REPL_PAIRS.length - 1; i >= 0; i--) {
      String [] repl = REPL_PAIRS[i];
      id = id.replaceAll(repl[1], repl[0]);
    }
    try {
      id = URLDecoder.decode(id, ENC_CHARSET);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("UTF-8 string decoding not supported.", e);
    }
    if (id.startsWith(ROOT_NAME)) {
      id = SEP + id.substring(ROOT_NAME.length());
    }
    return new Path(id);
  }
}
