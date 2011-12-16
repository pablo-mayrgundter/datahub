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

  String get(String dir, long id) {
    Entity entity;
    if (id == 0) {
      throw new IllegalArgumentException("Id must be greater than zero: " + id);
    }
    Key k = KeyFactory.createKey(rootKey, dir, id);
    try {
      entity = datastore.get(k);
    } catch (EntityNotFoundException e) {
      return null;
    }
    return Util.entityToJson(entity).toString();
  }

  String list(String dir) {
    Iterable<Entity> results = datastore.prepare(new Query(dir)).asIterable();
    if (!results.iterator().hasNext()) {
      return null;
    }
    return Util.entitiesToJson(results).toString();
  }

  String save(String kind, String jsonStr) {
    // TODO(pmy): txn
    List<Entity> entities = new ArrayList<Entity>();
    Util.entitiesFromJsonStr(rootKey, kind, jsonStr, entities);
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