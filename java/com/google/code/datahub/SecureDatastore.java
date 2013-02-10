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

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Logger;

/**
 * The SecureDatastore extends Datastore to add a check for Access
 * permissions for the combination of (user,path+,operation) in an
 * Access Control List (ACL).
 *
 * ACLs are checked as followed:
 * <pre>
 *   1. Starting at the root and proceeding down a given path, each
 *   path is converted to an associated ACL key.
 *
 *   2. If the ACL for that key exists, it is checked for allowance of
 *   the operation for the user.  If allowed, then the check continues
 *   towards the final given path, otherwise it fails
 * </pre>
 *
 * Operations which affect multiple paths will be either completely
 * allowed or completely refused.
 */
class SecureDatastore extends Datastore {

  protected static final Logger logger = Logger.getLogger(Datastore.class.getName() + "_ACLs");

  static final String ACL_KIND = "acl";
  static final String ACL_KEY_RESTRICT = "restrict";
  static final String ACL_KEY_ALLOW = "allow";

  @Override
  public Path create(Path parent, JSONObject json, User user) {
    assertNotRestricted(parent, user, Op.CREATE);
    return super.create(parent, json, user);
  }  

  @Override
  public Path create(Path parent, String name, JSONObject json, User user) {
    assertNotRestricted(parent, user, Op.CREATE);
    return super.create(parent, name, json, user);
  }

  @Override
  public void delete(User user, Path ... paths) {
    for (Path path : paths) {
      assertNotRestricted(path, user, Op.DELETE);
    }
    super.delete(user, paths);
  }

  @Override
  public JSONObject list(Path path,
                         int offset, int limit, String [] fields, int [] order,
                         String reqEndpointId, long duration,
                         User user) {
    assertNotRestricted(path, user, Op.READ);
    return super.list(path, offset, limit, fields, order, reqEndpointId, duration, user);
  }

  @Override
  public JSONObject retrieve(Path path, User user) {
    assertNotRestricted(path, user, Op.READ);
    return super.retrieve(path, user);
  }

  @Override
  public void update(Path path, JSONObject json, User user) {
    try {
      assertNotRestricted(path, user, Op.UPDATE);
    } catch (NotFoundException e) {
      // TODO(pmy): OK?
    }
    super.update(path, json, user);
  }

  @Override
  public JSONObject search(Path path, String query, User user) {
    assertNotRestricted(path, user, Op.READ);
    return super.search(path, query, user);
  }

  @Override
  public JSONObject search(Path path,
                           String query,
                           int offset, int limit,
                           String [] fields, int [] order,
                           String endpointId, long duration,
                           User user) {
    assertNotRestricted(path, user, Op.READ);
    return super.search(path, query, offset, limit, fields, order, endpointId, duration, user);
  }

  // ACL API.

  /**
   * @throws OperationRestrictedException If the given tuple is restricted.
   * @throws NotFoundException If the given path does not exist.
   */
  public void assertNotRestricted(Path path, User user, Op op) throws SecurityException {
    logger.fine(String.format("path(%s) uid(%s) operation(%s)",
                              path, user.getEffectiveUID(), op));
    if (isRestricted(path, user, op)) {
      throw new OperationRestrictedException(path, user, op);
    }
  }

  public void clearRestricted(Path path, User user, Op op) throws SecurityException {
    logger.fine(String.format("path(%s) uid(%s) operation(%s)",
                              path, user.getEffectiveUID(), op));
    JSONObject acl = getAcl(path);
    if (acl == null) {
      acl = new JSONObject();
    }
    clearInAcl(acl, user, op);
    saveAcl(path, acl);
  }

  public boolean isRestricted(Path path, User user, Op op) throws SecurityException {
    logger.fine(String.format("path(%s) uid(%s) operation(%s)",
                              path, user.getEffectiveUID(), op));
    // TODO(pmy): iteration order is from path to root; could be reversed.
    Path curPath = path;
    do {
      JSONObject acl = getAcl(curPath);
      if (acl != null && isRestrictedInAcl(acl, user, op)) {
        return true;
      }
    } while ((curPath = curPath.getParent()) != Path.ROOT);
    JSONObject acl = getAcl(Path.ROOT);
    if (acl != null && isRestrictedInAcl(acl, user, op)) {
      return true;
    }
    return false;
  }

  public void setRestricted(Path path, User user, Op op) throws SecurityException {
    logger.fine(String.format("path(%s) uid(%s) operation(%s)",
                              path, user.getEffectiveUID(), op));
    JSONObject acl = getAcl(path);
    if (acl == null) {
      acl = new JSONObject();
    }
    restrictInAcl(acl, user, op);
    saveAcl(path, acl);
  }

  // ACL implementation.

  Key createAclKey(Path path) {
    Key pathKey = path.toKey();
    if (pathKey.getName() == null) {
      return KeyFactory.createKey(pathKey.getParent(), ACL_KIND, pathKey.getId());
    } else {
      return KeyFactory.createKey(pathKey.getParent(), ACL_KIND, pathKey.getName());
    }
  }

  // TODO(pmy): would be nice to hand back only the needed part of the
  // acl.
  // TODO(pmy): cache key ACLs.
  JSONObject getAcl(Path path) {
    Key aclKey = createAclKey(path);
    Entity aclEntity;
    try {
      aclEntity = service.get(aclKey);
    } catch (EntityNotFoundException e) {
      logger.finer(String.format("getAcl: path(%s), aclKey(%s): not found", path, aclKey));
      return null;
    }
    JSONObject acl = entityToJson(aclEntity);
    logger.finer(String.format("getAcl: path(%s): aclKey(%s) acl(%s)", path, aclKey, acl));
    return acl;
  }

  void saveAcl(Path path, JSONObject acl) {
    Key aclKey = createAclKey(path);
    Entity aclEntity = new Entity(aclKey);
    setProperties(aclEntity, acl);
    logger.finer(String.format("saveAcl: path(%s): aclKey(%s) acl(%s)",
                               path, aclKey, acl));
    service.put(aclEntity);
  }

  // ACL manipulation.

  void clearInAcl(JSONObject acl, User user, Op op) {
    // TODO(pmy): remove empty acl containers.
    JSONObject restrict = Util.jsonGet(acl, ACL_KEY_RESTRICT);
    if (restrict == null) {
      return;
    }
    final String opStr = op.toString();
    final String toBeRestrictedUid = user.getEffectiveUID();
    JSONArray restrictedUids;
    try {
      restrictedUids = restrict.getJSONArray(opStr);
    } catch (JSONException e) {
      return;
    }
    // iterate over length() since it may change.
    for (int i = 0; i < restrictedUids.length(); i++) {
      String uid;
      try {
        uid = restrictedUids.getString(i);
      } catch (JSONException e) {
        throw new IllegalStateException("Corrupted ACL");
      }
      if (uid.equals(toBeRestrictedUid)) {
        restrictedUids.remove(i);
        return;
      }
    }
  }

  void restrictInAcl(JSONObject acl, User user, Op op) {
    JSONObject restrict = Util.jsonGet(acl, ACL_KEY_RESTRICT);
    if (restrict == null) {
      Util.jsonPut(acl, ACL_KEY_RESTRICT, restrict = new JSONObject());
    }
    final String opStr = op.toString();
    final String toBeRestrictedUid = user.getEffectiveUID();
    JSONArray restrictedUids;
    try {
      restrictedUids = restrict.getJSONArray(opStr);
    } catch (JSONException e) {
      Util.jsonPut(restrict, opStr, restrictedUids = new JSONArray());
    }
    for (int i = 0, n = restrictedUids.length(); i < n; i++) {
      String uid;
      try {
        uid = restrictedUids.getString(i);
      } catch (JSONException e) {
        throw new IllegalStateException("Corrupted ACL");
      }
      if (uid.equals(toBeRestrictedUid)) {
        return;
      }
    }
    restrictedUids.put(toBeRestrictedUid);
  }

  boolean isRestrictedInAcl(JSONObject acl, User user, Op op) {
    JSONObject restrict = Util.jsonGet(acl, ACL_KEY_RESTRICT);
    if (restrict == null) {
      logger.fine("Missing restrict in acl, so not restricted");
      return false;
    }
    final String opStr = op.toString();
    final String toBeRestrictedUid = user.getEffectiveUID();
    JSONArray restrictedUids;
    try {
      restrictedUids = restrict.getJSONArray(opStr);
    } catch (JSONException e) {
      logger.fine("Missing restrict on op in acl, so not restricted");
      return false;
    }
    for (int i = 0, n = restrictedUids.length(); i < n; i++) {
      String uid;
      try {
        uid = restrictedUids.getString(i);
      } catch (JSONException e) {
        throw new IllegalStateException("Corrupted ACL");
      }
      if (uid.equals(toBeRestrictedUid)) {
        logger.fine("Found user in acl op list, so restricted");
        return true;
      }
    }
    logger.fine("Missing user in acl op list, so not restricted");
    return false;
  }
}
