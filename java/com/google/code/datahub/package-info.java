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

/**
 * The REST package provides servlets implementing REST/CRUD plus
 * streaming-search HTTP endpoints for some public GAE services.  This
 * design is intended to enable a consistent Create, Read, Update,
 * Delete (CRUD) plus Search interface to the resource-oriented GAE
 * services.  Service objects are managed using a consistent path
 * system that is intended to be compatible with Representational
 * State Transfer (REST)-ful client-server protocols.  Service objects
 * are encoded using JSON to provide a consistent cross-service object
 * representation.
 *
 * @see com.google.appengine.demos.rest.Resource for the main
 * Servlet defining the REST protocol.
 *
 * @see A good discussion about REST-to-CRUD mapping can be found at
 * http://blog.punchbarrel.com/2008/10/31/rest-is-not-crud-and-heres-why/
 */
package com.google.code.datahub;
