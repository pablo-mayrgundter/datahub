package hub;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;

import java.util.ArrayList;
import java.util.List;

class Store {

  DatastoreService datastore;
  Key rootKey = null;

  Store() {
    datastore = DatastoreServiceFactory.getDatastoreService();
    rootKey = KeyFactory.createKey("data", "root"); // TODO(pablo): init via config.
  }

  /**
   * Retrieve the object with the given id from the given directory.
   *
   * @return A JSON formatted string of the requested entity.
   */
  String get(Resource rsrc) {
    Entity entity;
    if (rsrc.getId() == 0) {
      throw new IllegalArgumentException("Id must be greater than zero: " + rsrc.getId());
    }
    Key k = KeyFactory.createKey(rootKey, rsrc.getDir(), rsrc.getId());
    try {
      entity = datastore.get(k);
    } catch (EntityNotFoundException e) {
      return null;
    }
    return Util.entityToJson(entity).toString();
  }

  /**
   * @return A JSON formatted list of entities in the given directory.
   */
  String list(Resource rsrc) {
    Iterable<Entity> results = datastore.prepare(new Query(rsrc.getDir())).asIterable();
    if (!results.iterator().hasNext()) {
      return null;
    }
    return Util.entitiesToJson(results).toString();
  }

  /**
   * Stores the given json object as an entity of the given kind and
   * returns the path to the object.
   *
   * @return The URL host path of the stored object.
   */
  String save(Resource rsrc, String jsonStr) {
    // TODO(pmy): txn
    List<Entity> entities = new ArrayList<Entity>();
    Util.entitiesFromJsonStr(rootKey, rsrc, jsonStr, entities);
    System.err.println("Entities created: " + entities);
    datastore.put(entities);
    Entity head = entities.get(0);
    System.err.println("Head entity: " + head);
    Key k = head.getKey();
    System.err.println("Head entity key: " + k);
    System.err.println("Head entity key kind: " + k.getKind());
    System.err.println("Head entity key name (unused): " + k.getName());
    System.err.println("Head entity key id: " + k.getId());
    String newObjPath = k.getKind() + "/" + k.getId();
    System.err.println("New object path: " + newObjPath);
    return newObjPath;
  }
}