/*
 * Copyright 2017 Agilx, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.apptuit.metrics.client;

public interface Sanitizer {

  Sanitizer PROMETHEUS_SANITIZER = new PrometheusSanitizer();
  Sanitizer APPTUIT_SANITIZER = new ApptuitSanitizer();
  Sanitizer NO_OP_SANITIZER = new NoOpSanitizer();
  Sanitizer DEFAULT_SANITIZER = PROMETHEUS_SANITIZER;

  String sanitizer(String unSanitizedString);

  class PrometheusSanitizer implements Sanitizer {
    private PrometheusSanitizer() {
    }

    public String sanitizer(String unSanitizedString) {
      return ((Character.isDigit(unSanitizedString.charAt(0)) ? "_" : "")
              + unSanitizedString).replaceAll("[^a-zA-Z0-9_]", "_")
              .replaceAll("[_]+", "_");

    }
  }

  class ApptuitSanitizer implements Sanitizer {
    private ApptuitSanitizer() {
    }

    public String sanitizer(String unSanitizedString) {
      return unSanitizedString.replaceAll("[^\\p{L}\\-./_0-9]+", "_")
              .replaceAll("[_]+", "_");
    }
  }

  class NoOpSanitizer implements Sanitizer {
    private NoOpSanitizer() {
    }

    public String sanitizer(String unSanitizedString) {
      return unSanitizedString;
    }
  }
}
