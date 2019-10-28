package ai.apptuit.metrics.client.prometheus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PrometheusClientTest {
  @Test
  public void testGetStepSize() throws Exception {
    assertEquals(1, PrometheusClient.getStepSize(1));
    assertEquals(1, PrometheusClient.getStepSize(1000));
    assertEquals(1, PrometheusClient.getStepSize(1000000));
    assertEquals(2, PrometheusClient.getStepSize(2000000));
    assertEquals(3, PrometheusClient.getStepSize(3000000));
    assertEquals(5, PrometheusClient.getStepSize(5000000));
    assertEquals(5, PrometheusClient.getStepSize(7000000));
    assertEquals(15, PrometheusClient.getStepSize(17000000));
    assertEquals(45, PrometheusClient.getStepSize(59000000));
    assertEquals(60, PrometheusClient.getStepSize(60000000));
    assertEquals(60, PrometheusClient.getStepSize(61000000));
    assertEquals(300, PrometheusClient.getStepSize(300000000));
    assertEquals(300, PrometheusClient.getStepSize(301000000));
    assertEquals(3600, PrometheusClient.getStepSize(3600000000L));
    assertEquals(3600, PrometheusClient.getStepSize(3601000000L));
    assertEquals(7200, PrometheusClient.getStepSize(7501000000L));
  }
}
