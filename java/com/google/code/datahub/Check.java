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
 * Assert utilities.
 *
 * @author Pablo Mayrgundter
 */
public final class Check {
  public static final void CHECK(String message, boolean condition) {
    if (!condition) {
      throw new IllegalStateException(message);
    }
  }

  public static final void CHECK_NOT_NULL(String message, Object obj) {
    if (obj == null) {
      throw new NullPointerException(message);
    }
  }
}
