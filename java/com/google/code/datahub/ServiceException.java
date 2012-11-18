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

/**
 * The ServiceException class is a simple wrapper for checked
 * exceptions in backend services.  Runtime exceptions are preferred
 * in order to propagate the backend exceptions through to services
 * users via a general exception handler in the response.
 *
 * TODO(pmy): add JSP exception response handler.
 *
 * @author pmy@google.com (Pablo Mayrgundter)
 */
class ServiceException extends RuntimeException {

  static final long serialVersionUID = -5876917688417302908L;

  ServiceException() {
  }

  ServiceException(String msg) {
    super(msg);
  }

  ServiceException(String msg, Throwable cause) {
    super(msg, cause);
  }

  ServiceException(Throwable cause) {
    super(cause);
  }
}