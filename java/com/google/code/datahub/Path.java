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
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import static com.google.code.datahub.Check.*;

/**
 * Minimal wrapper for URI paths, e.g. "/a/b/c".
 *
 * @author Pablo Mayrgundter <pmy@google.com>
 */
public class Path {

  static final String PATH_KIND = "path";
  static final String ROOT_NAME = "ROOT";
  static final Key ROOT_KEY = KeyFactory.createKey(PATH_KIND, ROOT_NAME);
  static final Path ROOT = new Path();

  static final String SEP = "/";
  static final Pattern REGEX_SPECIAL = Pattern.compile("__(.+)__");
  static final Pattern REGEX_SERIAL = Pattern.compile("\\d+");

  final Key [] path;

  /** Root constructor. */
  private Path() {
    path = new Key[0];
  }

  Path(Key [] path) {
    CHECK("Path must contain at least one valid directory.", path.length > 0);
    this.path = path;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(path);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Path) {
      return Arrays.equals(path, ((Path) o).path);
    }
    return false;
  }

  /**
   * @return The form "/a/b/c".
   */
  @Override
  public String toString() {
    if (path.length == 0) {
      return SEP;
    }
    String s = "";
    for (Key key : path) {
      s += SEP + getNameOrId(key);
    }
    return s;
  }

  public int getLength() {
    return path.length;
  }

  /**
   * @return a List of the paths on the way from the root to this
   * path.
   */
  public List<Path> pathList() {
    LinkedList<Path> paths = new LinkedList<Path>();
    Path cur = this;
    do {
      paths.push(cur);
    } while ((cur = cur.getParent()) != ROOT);
    return paths;
  }

  public String getFilename() {
    return getNameOrId(toKey());
  }

  public Path getParent() {
    if (path.length <= 1) {
      return ROOT;
    }
    Key [] sub = new Key[path.length - 1];
    System.arraycopy(path, 0, sub, 0, sub.length);
    return new Path(sub);
  }

  // TODO(pmy): perhaps int compareTo(Path) instead?
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

  public boolean isSpecial() {
    if (isSpecialSerial()) {
      return true;
    }
    // If not a serial id, then must have a name.
    return REGEX_SPECIAL.matcher(toKey().getName()).matches();
  }

  public boolean isSpecialSerial() {
    return toKey().getName() == null;
  }

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
   * Converts this path to a Key by creating intermediate parent keys
   * for each path element, and linking them together to the last key,
   * which is returned.  Each key's kind is its parent's name.  The
   * root key's name is defined in {@link #ROOT_NAME}
   */
  Key toKey() {
    // Construct key by defining the first element and then
    // iteratively adding the rest of the path parts.  TODO(pmy):
    // ensure ROOT is escaped/unique; should this be == "ALL"?
    if (path.length == 0) {
      return ROOT_KEY;
    }
    return path[path.length - 1];
  }

  /**
   * Converts the given key's inheritance hierarchy to an equivalent
   * Path.
   *
   * @throws IllegalArgumentException if the given key.isComplete() returns false.
   */
  static final Path fromKey(Key key) {
    if (!key.isComplete()) {
      throw new IllegalArgumentException("Key is incomplete, cannot create string representation.");
    }

    if (key.equals(ROOT_KEY)) {
      return ROOT;
    }

    LinkedList<Key> path = new LinkedList<Key>();
    do {
      // Don't add root to path.
      if (key.getParent() == null) {
        break;
      }

      path.push(key);
    } while ((key = key.getParent()) != null);

    return new Path(path.toArray(new Key[path.size()]));
  }

  /**
   * Splits the given {@code pathStr} on SEP and stores the parts
   * as the path.
   */
  static Path fromString(String pathStr) {
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
    if (pathStr.equals("")) {
      return ROOT;
    }
    return new Path(resolveParts(pathStr.split(SEP)));
  }

  /**
   * Construct a Path object for the given REST-ful request and
   * URI.  The given request URI must have the request servletPath as
   * its prefix.  This prefix is removed from the URI and subsequent
   * characters are interpreted.
   */
  static Path fromRequest(HttpServletRequest req) {
    String uri = req.getRequestURI();
    String srvPath = req.getServletPath();
    String relPath = uri.substring(srvPath.length());
    return fromString(relPath);
  }

  static final Pattern PART_PATTERN = Pattern.compile("(\\w+)(?:[(](\\w+)[)])?");

  /**
   * @throws NullPointerException if the given parent is null.
   */
  static Key resolvePart(String part, Key parent) {
    if (parent == null) {
      throw new NullPointerException("Given null parent, use ROOT singleton instead.");
    }
    Matcher m = PART_PATTERN.matcher(part);
    if (!m.find()) {
      throw new IllegalArgumentException(String.format("Path part(%s) must match: %s",
                                                       part, PART_PATTERN));
    }
    String kind = PATH_KIND, name;
    if (m.group(2) == null) {
      name = m.group(1);
    } else {
      kind = m.group(1);
      name = m.group(2);
    }
    long id = -1;
    if (name.startsWith("__") && name.endsWith("__")) {
      try {
        id = Long.parseLong(name.substring(2, name.length() - 2));
      } catch (NumberFormatException e) {}
    }
    return id == -1 ? KeyFactory.createKey(parent, kind, name) : KeyFactory.createKey(parent, kind, id);
  }

  static Key[] resolveParts(String [] parts) {
    Key [] keys = new Key[parts.length];
    Key parent = ROOT_KEY;
    for (int i = 0; i < parts.length; i++) {
      keys[i] = resolvePart(parts[i], parent);
      parent = keys[i];
    }
    return keys;
  }

  static String getNameOrId(Key key) {
    if (key.getName() == null) {
      return "__" + key.getId() + "__";
    } else {
      return key.getName();
    }
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
    return Path.fromString(id);
  }
}
