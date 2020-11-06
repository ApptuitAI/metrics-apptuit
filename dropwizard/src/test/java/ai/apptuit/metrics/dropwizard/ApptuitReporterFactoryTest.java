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

import ai.apptuit.metrics.client.Sanitizer;
import ai.apptuit.metrics.dropwizard.ApptuitReporter.ReportingMode;
import ai.apptuit.metrics.dropwizard.BaseMockClient.DataListener;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

/**
 * @author Rajiv Shivane
 */
@PrepareForTest({ApptuitReporter.class})
@PowerMockIgnore({"org.jboss.byteman.*", "javax.net.ssl.*",
        "com.sun.management.*", "javax.management.*"})
@RunWith(PowerMockRunner.class)
public class ApptuitReporterFactoryTest extends BaseReporterTest {

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

    BaseMockClient mockClient =
            reportingMode == ReportingMode.API_PUT ? MockApptuitPutClient.getInstance()
                    : MockXCollectorForwarder.getInstance();

    try (ScheduledReporter ignored = createReporter(reportingMode)) {
      mockClient.addPutListener(listener);
      metricUpdate.run();
      await().atMost(period * 15, TimeUnit.SECONDS).until(awaitUntil);
      mockClient.removePutListener(listener);
    }
  }

  @Test
  public void testErrorHandler() throws Exception {

    ApptuitReporterFactory factory = new ApptuitReporterFactory();
    factory.setRateUnit(TimeUnit.SECONDS);
    factory.setDurationUnit(TimeUnit.MILLISECONDS);
    factory.addGlobalTag("globalTag1", "globalValue1");
    factory.setReportingMode(ReportingMode.API_PUT);
    factory.setApiKey("dummy");

    factory.setSanitizer(Sanitizer.NO_OP_SANITIZER);
    assertEquals(Sanitizer.NO_OP_SANITIZER, factory.getSanitizer());

    AtomicBoolean gotError = new AtomicBoolean(false);
    SendErrorHandler errorHandler = e -> gotError.set(true);
    factory.setErrorHandler(errorHandler);
    assertEquals(errorHandler, factory.getErrorHandler());

    try (ScheduledReporter reporter = factory.build(registry)) {
      reporter.start(period, TimeUnit.SECONDS);
      await().atMost(period * 15, TimeUnit.SECONDS).until(gotError::get);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMalformedUrl() throws Exception {
    ApptuitReporterFactory factory = new ApptuitReporterFactory();
    factory.setApiUrl("dummy://cause.failure");
    factory.build(registry);
  }

  private ScheduledReporter createReporter(ReportingMode mode) {
    ApptuitReporterFactory factory = new ApptuitReporterFactory();
    factory.setRateUnit(TimeUnit.SECONDS);
    factory.setDurationUnit(TimeUnit.MILLISECONDS);
    factory.addGlobalTag("globalTag1", "globalValue1");
    if (mode == ReportingMode.API_PUT) {
      factory.setApiKey("dummy");
    }

    factory.setReportingMode(mode);
    factory.setSanitizer(Sanitizer.NO_OP_SANITIZER);

    ScheduledReporter reporter = factory.build(registry);
    reporter.start(period, TimeUnit.SECONDS);
    return reporter;
  }
}
