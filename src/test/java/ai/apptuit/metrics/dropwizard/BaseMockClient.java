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
package ai.apptuit.metrics.dropwizard;

import ai.apptuit.metrics.client.DataPoint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Rajiv Shivane
 */
public abstract class BaseMockClient {

  protected final List<DataListener> listeners = new ArrayList<>();

  public void addPutListener(DataListener listener) {
    listeners.add(listener);
  }

  public boolean removePutListener(DataListener listener) {
    return listeners.remove(listener);
  }

  public static interface DataListener {

    public void onData(Collection<DataPoint> dataPoints);
  }
}
