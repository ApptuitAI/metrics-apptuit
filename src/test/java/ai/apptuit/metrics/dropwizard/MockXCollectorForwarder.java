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
import ai.apptuit.metrics.client.Sanitizer;
import ai.apptuit.metrics.client.XCollectorForwarder;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;

/**
 * @author Rajiv Shivane
 */
public class MockXCollectorForwarder extends BaseMockClient {

  private static final MockXCollectorForwarder instance = new MockXCollectorForwarder();

  private MockXCollectorForwarder() {
  }

  public static MockXCollectorForwarder getInstance() throws Exception {
    initialize();
    return instance;
  }

  public static void initialize() throws Exception {

    XCollectorForwarder forwarder = mock(XCollectorForwarder.class);
    PowerMockito.whenNew(XCollectorForwarder.class).withAnyArguments().thenReturn(forwarder);

    doAnswer((Answer<Void>) invocation -> {
      Object[] args = invocation.getArguments();
      getInstance().notifyListeners(getDataPoints(args));
      return null;
    }).when(forwarder).forward(anyCollectionOf(DataPoint.class), any(Sanitizer.class));
  }

  @SuppressWarnings("unchecked")
  private static <T> T getDataPoints(Object[] args) {
    return (T) args[0];
  }

}
