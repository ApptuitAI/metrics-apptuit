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

import java.io.IOException;

/**
 * @author Joseph Danthikolla
 */
public class ResponseStatusException extends IOException {

    private int httpStatus;
    private String body;

    public ResponseStatusException(int httpStatus, String body) {
        this(httpStatus, body, null);
    }

    public ResponseStatusException(int httpStatus, String body, Throwable cause) {
        super(cause);
        this.httpStatus = httpStatus;
        this.body = body;
    }

    public String getMessage() {
        return this.httpStatus + (this.body != null ? " [" + this.body + "]" : "");
    }

    public String getResponseBody() {
        return body;
    }

    public int getResponseStatus() {
        return httpStatus;
    }
}
