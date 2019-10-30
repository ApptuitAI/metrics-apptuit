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
import ai.apptuit.metrics.dropwizard.ApptuitReporter.ReportingMode;
import ai.apptuit.metrics.dropwizard.BaseMockClient.DataListener;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import org.junit.After;
import org.junit.Before;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * @author Rajiv Shivane
 */
public class ApptuitReporterTest extends BaseReporterTest {

  private int period = 1;

  @Before
  public void setUp() throws Exception {
    registry = new MetricRegistry();
  }

  @After
  public void tearDown() throws Exception {
  }


  @Override
  protected void testMetric(ReportingMode reportingMode, Runnable metricUpdate, DataListener listener,
                            Callable<Boolean> awaitUntil) throws Exception {

    MockDataPointsSender mockClient = new MockDataPointsSender();
    ScheduledReporter reporter = new ApptuitReporter(registry, (name, metric) -> true, TimeUnit.SECONDS,
        TimeUnit.MILLISECONDS, mockClient);
    reporter.start(period, TimeUnit.SECONDS);
    try {
      mockClient.addPutListener(listener);
      metricUpdate.run();
      await().atMost(period * 15, TimeUnit.SECONDS).until(awaitUntil);
      mockClient.removePutListener(listener);
    } finally {
      reporter.close();
    }
  }

  private static class MockDataPointsSender extends BaseMockClient implements ApptuitReporter.DataPointsSender {
    @Override
    public void send(Collection<DataPoint> dataPoints) {
      notifyListeners(dataPoints);
    }
  }
}
