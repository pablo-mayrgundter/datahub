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

import static com.google.appengine.api.prospectivesearch.FieldType.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.prospectivesearch.FieldType;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The SchemaManager class is a utility class to consolidate
 * conversion between the various low-level service entity
 * representations.
 *
 * @author Pablo Mayrgundter <pmy@google.com>
 */
class SchemaManager {

  static final Logger logger = Logger.getLogger(SchemaManager.class.getName());

  // TODO(pmy): probably not static.
  static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

  static void initSchemaForPsi(Path path) {
    Entity schema = findCreateSchema(path);
    mapFieldType(schema, Search.INTERNAL_QUERY_FIELD_PATH, Field.FieldType.TEXT);
    saveSchema(schema);
  }

  static void updateSchema(Path path, Document doc) {
    Entity schema = findCreateSchema(path);
    updateSchema(schema, doc);
    saveSchema(schema);
  }

  /**
   * Converts the given schema to the PSI schema format:
   *
   * <pre>
   * ATOM   -> STRING
   * DATE   -> NUMBER
   * HTML   -> TEXT
   * NUMBER -> NUMBER
   * TEXT   -> TEXT
   * </pre>
   */
  static Map<String, FieldType> getSchemaForPsi(Path path) {
    Entity schema = findCreateSchema(path);
    Map<String, FieldType> psiSchema = new HashMap<String, FieldType>();
    for (String name : schema.getProperties().keySet()) {
      final Field.FieldType ftsType = Field.FieldType.valueOf((String) schema.getProperty(name));
      switch (ftsType) {
        case ATOM: psiSchema.put(name, FieldType.STRING); break;
        case DATE: psiSchema.put(name, FieldType.DOUBLE); break;
        case HTML: psiSchema.put(name, FieldType.TEXT); break;
        case NUMBER: psiSchema.put(name, FieldType.DOUBLE); break;
        case TEXT: psiSchema.put(name, FieldType.TEXT); break;
      }
    }
    return psiSchema;
  }

  private static Entity findCreateSchema(Path path) {
    Key key = path.toKey();
    if (key.getParent() != null) {
      key = key.getParent();
    }
    Key schemaKey = KeyFactory.createKey("schema", Path.fromKey(key).toString());
    Entity schema;
    try {
      schema = datastore.get(schemaKey);
    } catch (EntityNotFoundException e) {
      schema = new Entity(schemaKey);
    }
    return schema;
  }

  /**
   * Updates the given schema to have the same field types as the
   * given doc, as well as any previous types.
   */
  private static void updateSchema(Entity schema, Document doc) {
    for (Field field : doc.getFields()) {
      final String fieldName = field.getName();
      switch (field.getType()) {
        case ATOM: mapFieldType(schema, fieldName, Field.FieldType.ATOM); break;
        case DATE: mapFieldType(schema, fieldName, Field.FieldType.DATE); break;
        case HTML: mapFieldType(schema, fieldName, Field.FieldType.HTML); break;
        case NUMBER: mapFieldType(schema, fieldName, Field.FieldType.NUMBER); break;
        case TEXT: mapFieldType(schema, fieldName, Field.FieldType.TEXT); break;
      }
    }
  }

  private static void saveSchema(Entity schema) {
    logger.fine("SchemaManager: saveSchema: " + schema);
    datastore.put(schema);
  }

  /** Helper to set field type proeprty on an entity in a single way. */
  private static void mapFieldType(Entity e, String fieldName, Field.FieldType f) {
    e.setProperty(fieldName, f.name());
  }
}
