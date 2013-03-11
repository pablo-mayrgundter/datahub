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

  @Override
  public Path create(Path parent, JSONObject json, User user) {
    assertAllowed(parent, user, Op.CREATE);
    return super.create(parent, json, user);
  }  

  @Override
  public Path create(Path parent, String name, JSONObject json, User user) {
    assertAllowed(parent, user, Op.CREATE);
    return super.create(parent, name, json, user);
  }

  @Override
  public void delete(User user, Path ... paths) {
    for (Path path : paths) {
      assertAllowed(path, user, Op.DELETE);
    }
    super.delete(user, paths);
  }

  @Override
  public JSONObject list(Path path,
                         int offset, int limit, String [] fields, int [] order,
                         String reqEndpointId, long duration,
                         User user) {
    assertAllowed(path, user, Op.READ);
    return super.list(path, offset, limit, fields, order, reqEndpointId, duration, user);
  }

  @Override
  public JSONObject retrieve(Path path, User user) {
    assertAllowed(path, user, Op.READ);
    return super.retrieve(path, user);
  }

  @Override
  public void update(Path path, JSONObject json, User user) {
    try {
      assertAllowed(path, user, Op.UPDATE);
    } catch (NotFoundException e) {
      // TODO(pmy): OK?
    }
    super.update(path, json, user);
  }

  @Override
  public JSONObject search(Path path, String query, User user) {
    assertAllowed(path, user, Op.READ);
    return super.search(path, query, user);
  }

  @Override
  public JSONObject search(Path path,
                           String query,
                           int offset, int limit,
                           String [] fields, int [] order,
                           String endpointId, long duration,
                           User user) {
    assertAllowed(path, user, Op.READ);
    return super.search(path, query, offset, limit, fields, order, endpointId, duration, user);
  }

  // ACL API.

  static enum ControlType { RESTRICT, ALLOW };
  static final String ACL_KIND = "acl";
  static final String ACL_KEY_ALLOW = "allow";

  /**
   * @throws OperationRestrictedException If the given tuple is restricted.
   * @throws NotFoundException If the given path does not exist.
   */
  public void assertAllowed(Path path, User user, Op op) throws SecurityException {
    String euid = user.getEffectiveUID();
    logger.fine(String.format("path(%s) uid(%s) operation(%s)", path, euid, op));
    if (isRestricted(path, user, op)) {
      throw new OperationRestrictedException(path, user, op);
    }
  }

  public void clearAllowed(Path path, User user, Op op) throws SecurityException {
    clearControl(path, ControlType.ALLOW, user, op);
  }

  public void clearRestricted(Path path, User user, Op op) throws SecurityException {
    clearControl(path, ControlType.RESTRICT, user, op);
  }

  public boolean isAllowed(Path path, User user, Op op) throws SecurityException {
    // return isControlled(path, ControlType.ALLOW, user, op);
    return getControlLevel(path, ControlType.ALLOW, user, op)
      >= getControlLevel(path, ControlType.RESTRICT, user, op);
  }
  // getControlLevel(ALLOW) >= getControlLevel(RESTRICT).

  public boolean isRestricted(Path path, User user, Op op) throws SecurityException {
    // return isControlled(path, ControlType.RESTRICT, user, op);
    return getControlLevel(path, ControlType.ALLOW, user, op)
      < getControlLevel(path, ControlType.RESTRICT, user, op);
  }
  // getControlLevel(ALLOW) < getControlLevel(RESTRICT).

  public void setAllowed(Path path, User user, Op op) throws SecurityException {
    setControl(path, ControlType.ALLOW, user, op);
  }

  public void setRestricted(Path path, User user, Op op) throws SecurityException {
    setControl(path, ControlType.RESTRICT, user, op);
  }

  // Shared helpers for restrict/allow publics above.

  void clearControl(Path path, ControlType cType, User user, Op op) throws SecurityException {
    String euid = user.getEffectiveUID();
    logger.fine(String.format("path(%s) uid(%s) operation(%s)", path, euid, op));
    JSONObject acl = getAcl(path);
    if (acl == null) {
      return;
    }
    clearInAcl(acl, cType, euid, op);
    saveAcl(path, acl);
  }

  /**
   * Returns the length of the longest path that has the control type
   * set, or -1 if not set.
   */
  int getControlLevel(Path path, ControlType cType, User user, Op op) throws SecurityException {
    String euid = user.getEffectiveUID();
    logger.fine(String.format("path(%s) type(%s) uid(%s) operation(%s)", path, cType, euid, op));
    // TODO(pmy): iteration order is from path to root; could be reversed.
    Path curPath = path;
    do {
      JSONObject acl = getAcl(curPath);
      if (acl != null && isAssertedInAcl(acl, cType, euid, op)) {
        return curPath.getLength();
        // return true;
      }
    } while ((curPath = curPath.getParent()) != Path.ROOT);
    JSONObject acl = getAcl(Path.ROOT);
    if (acl != null && isAssertedInAcl(acl, cType, euid, op)) {
      return 0;
      // return true;
    }
    return -1;
    // return false;
  }

  void setControl(Path path, ControlType cType, User user, Op op) throws SecurityException {
    String euid = user.getEffectiveUID();
    logger.fine(String.format("path(%s) uid(%s) operation(%s)", path, euid, op));
    JSONObject acl = getAcl(path);
    if (acl == null) {
      acl = new JSONObject();
    }
    setControlInAcl(acl, cType, euid, op);
    saveAcl(path, acl);
  }

  // ACL manipulation.

  void clearInAcl(JSONObject acl, ControlType controlType, String uid, Op op) {
    // TODO(pmy): remove empty acl containers.
    JSONObject control = Util.jsonGet(acl, controlType.name());
    if (control == null) {
      return;
    }

    JSONArray controlledUids = getControlledAcls(control, op);
    if (controlledUids == null) {
      return;
    }

    findUid(controlledUids, uid, new UserVisitor() {
        boolean visit(JSONArray arr, int curNdx) {
          arr.remove(curNdx);
          return false; 
        }
      });
  }

  boolean isAssertedInAcl(JSONObject acl, ControlType controlType, String uid, Op op) {
    JSONObject control = Util.jsonGet(acl, controlType.name());
    if (control == null) {
      return false;
    }

    JSONArray controlledUids = getControlledAcls(control, op);
    if (controlledUids == null) {
      return false;
    }

    if (findUid(controlledUids, uid, new UserVisitor())) {
      return true;
    }

    logger.fine("Missing user in acl op list, so not controlled");
    return false;
  }

  void setControlInAcl(JSONObject acl, ControlType controlType, String uid, Op op) {
    JSONObject control = Util.jsonGet(acl, controlType.name());
    if (control == null) {
      Util.jsonPut(acl, controlType.name(), control = new JSONObject());
    }

    JSONArray controlledUids = getControlledAcls(control, op);
    if (controlledUids == null) {
      Util.jsonPut(control, op.toString(), controlledUids = new JSONArray());
    }

    if (findUid(controlledUids, uid, new UserVisitor())) {
      // Control already exists for this user, return.
      return;
    }

    controlledUids.put(uid);
  }

  // Visitor helpers for above.

  JSONArray getControlledAcls(JSONObject control, Op op) {
    final String opStr = op.toString();
    JSONArray controlledUids = null;
    try {
      controlledUids = control.getJSONArray(opStr);
    } catch (JSONException e) {
      logger.fine("Missing control in acl, so not controlled");
    }
    return controlledUids;
  }

  class UserVisitor {
    /** Called if the visit finds the user.
     * @return true if the visit should continue. */
    boolean visit(JSONArray arr, int curNdx) { return false; }
  }

  boolean findUid(JSONArray uids, String targetUid, UserVisitor visitor) {
    for (int i = 0; i < uids.length(); i++) {
      String uid;
      try {
        uid = uids.getString(i);
      } catch (JSONException e) {
        throw new IllegalStateException("Corrupted ACL");
      }
      if (uid.equals(targetUid)) {
        logger.fine("Found user in acl op list, so controlled");
        if (!visitor.visit(uids, i)) {
          return true;
        }
      }
    }
    return false;
  }

  // ACL storage.

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
}
