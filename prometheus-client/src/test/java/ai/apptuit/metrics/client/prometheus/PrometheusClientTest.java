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

package ai.apptuit.metrics.client.prometheus;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import static ai.apptuit.metrics.client.prometheus.PrometheusClient.API_V_1_QUERY_RANGE;
import static org.junit.Assert.*;

/**
 * @author Rajiv Shivane
 */
public class PrometheusClientTest {

  private static MockServer httpServer;

  @BeforeClass
  public static void setUpClass() throws Exception {
    httpServer = new MockServer();
    httpServer.start();
  }

  @After
  public void tearDown() throws Exception {
    httpServer.resetCapturedData();
  }

  @Test
  public void testBearerAuthQuery() throws Exception {
    PrometheusClient client = new PrometheusClient(MockServer.TOKEN, httpServer.getUrl());
    testQueryRange(client, (authorizationHeader) -> assertEquals(MockServer.BEARER_AUTH_HEADER, authorizationHeader));
  }

  @Test
  public void testBasicAuthQuery() throws Exception {
    PrometheusClient client = new PrometheusClient(MockServer.USER_ID, MockServer.TOKEN, httpServer.getUrl());
    testQueryRange(client, (authorizationHeader) -> assertEquals(MockServer.BASIC_AUTH_HEADER, authorizationHeader));
    tearDown();
    testStepSize(client, (authorizationHeader) -> assertEquals(MockServer.BASIC_AUTH_HEADER, authorizationHeader));
    tearDown();
    testUnauthorizedStepSize(client, (authorizationHeader) -> assertEquals(MockServer.BASIC_AUTH_HEADER, authorizationHeader), 0);
    tearDown();
    testUnauthorizedStepSize(client, (authorizationHeader) -> assertEquals(MockServer.BASIC_AUTH_HEADER, authorizationHeader), -10);

  }

  private void testQueryRange(PrometheusClient client, Consumer<String> authHeaderValidator) throws IOException, ResponseStatusException, URISyntaxException {
    long end = System.currentTimeMillis();
    long start = end - 5 * 60 * 1000;
    QueryResponse queryResponse = client.query(start, end, "MOCK QUERY");
    assertNotNull(queryResponse);

    List<HttpExchange> exchanges = httpServer.getExchanges();
    assertEquals(1, exchanges.size());
    HttpExchange exchange = exchanges.get(0);
    assertEquals("/" + API_V_1_QUERY_RANGE, exchange.getRequestURI().getPath());
    assertEquals("POST", exchange.getRequestMethod());
    String authorizationHeader = exchange.getRequestHeaders().getFirst("Authorization");
    authHeaderValidator.accept(authorizationHeader);

    List<String> bodies = httpServer.getRequestBodies();
    assertEquals(1, bodies.size());
    String body = bodies.get(0);
    assertTrue(body.matches("^start=" + (start / 1000) + "&end=" + (end / 1000) + "&step=\\d+&query=MOCK\\+QUERY$"));

    //Additional validation of response
    assertNull(queryResponse.getError());
    assertNull(queryResponse.getErrorType());
    assertEquals(0, queryResponse.getWarnings().size());
    assertEquals(AbstractResponse.STATUS.success, queryResponse.getStatus());
    QueryResult result = queryResponse.getResult();
    assertNotNull(result);
    assertEquals(QueryResult.TYPE.matrix, result.getType());
    List<TimeSeries> series = ((MatrixResult) result).getSeries();
    assertEquals(20, series.size());
    TimeSeries ts = series.get(0);
    assertEquals(7, ts.getLabels().size());
    assertEquals(1196, ts.getValues().size());
    TimeSeries.Tuple tuple = ts.getValues().get(0);
    assertTrue(tuple.getTimestamp() >= 1570573740000L && tuple.getTimestamp() <= 1570577325000L);
    assertNotNull(tuple.getValue());
    assertEquals(Long.valueOf(tuple.getValue()), (Long) tuple.getValueAsLong());
  }

  private void testStepSize(PrometheusClient client, Consumer<String> authHeaderValidator) throws IOException, ResponseStatusException, URISyntaxException {
    long end = System.currentTimeMillis();
    long start = end - 5 * 60 * 1000;
    long stepSizeSeconds = 15 ;
    QueryResponse queryResponse = client.query(start, end, "MOCK QUERY", stepSizeSeconds);;
    assertNotNull(queryResponse);

    List<HttpExchange> exchanges = httpServer.getExchanges();
    assertEquals(1, exchanges.size());
    HttpExchange exchange = exchanges.get(0);
    assertEquals("/" + API_V_1_QUERY_RANGE, exchange.getRequestURI().getPath());
    assertEquals("POST", exchange.getRequestMethod());
    String authorizationHeader = exchange.getRequestHeaders().getFirst("Authorization");
    authHeaderValidator.accept(authorizationHeader);

    List<String> bodies = httpServer.getRequestBodies();
    assertEquals(1, bodies.size());
    String body = bodies.get(0);
    assertTrue(body.matches("^start=" + (start / 1000) + "&end=" + (end / 1000) + "&step="+ stepSizeSeconds +"+&query=MOCK\\+QUERY$"));

    //Additional validation of response
    assertNull(queryResponse.getError());
    assertNull(queryResponse.getErrorType());
    assertEquals(0, queryResponse.getWarnings().size());
    assertEquals(AbstractResponse.STATUS.success, queryResponse.getStatus());
    QueryResult result = queryResponse.getResult();
    assertNotNull(result);
    assertEquals(QueryResult.TYPE.matrix, result.getType());
    List<TimeSeries> series = ((MatrixResult) result).getSeries();
    assertEquals(20, series.size());
    TimeSeries ts = series.get(0);
    assertEquals(7, ts.getLabels().size());
    assertEquals(1196, ts.getValues().size());
    TimeSeries.Tuple tuple = ts.getValues().get(0);
    assertTrue(tuple.getTimestamp() >= 1570573740000L && tuple.getTimestamp() <= 1570577325000L);
    assertNotNull(tuple.getValue());
    assertEquals(Long.valueOf(tuple.getValue()), (Long) tuple.getValueAsLong());
  }

  private void testUnauthorizedStepSize(PrometheusClient client, Consumer<String> authHeaderValidator, long stepSizeSeconds) throws IOException, ResponseStatusException, URISyntaxException {
    long end = System.currentTimeMillis();
    long start = end - 5 * 60 * 1000;
    QueryResponse queryResponse = client.query(start, end, "MOCK QUERY", stepSizeSeconds);
    assertNotNull(queryResponse);
    List<HttpExchange> exchanges = httpServer.getExchanges();
    assertEquals(1, exchanges.size());
    HttpExchange exchange = exchanges.get(0);
    assertEquals("/" + API_V_1_QUERY_RANGE, exchange.getRequestURI().getPath());
    assertEquals("POST", exchange.getRequestMethod());
    String authorizationHeader = exchange.getRequestHeaders().getFirst("Authorization");
    authHeaderValidator.accept(authorizationHeader);
    List<String> bodies = httpServer.getRequestBodies();
    assertEquals(1, bodies.size());
    String body = bodies.get(0);
    assertEquals("zero or negative query resolution step widths are not accepted. Try a positive integer",queryResponse.getError());
    assertEquals("bad_data",queryResponse.getErrorType());
    assertEquals(AbstractResponse.STATUS.error, queryResponse.getStatus());
    assertNull(queryResponse.getResult());
  }

  @Test(expected = ResponseStatusException.class)
  public void testUnauthorizedQuery() throws Exception {
    URL url = httpServer.getUrl();
    PrometheusClient client = new PrometheusClient("WRONG_TOKEN", url);
    long end = System.currentTimeMillis();
    long start = end - 300000;
    QueryResponse queryResponse = null;
    try {
      queryResponse = client.query(start, end, "MOCK QUERY");
    } catch (ResponseStatusException e) {
      assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, e.getResponseStatus());
      throw e;
    }
  }

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

  private static class MockServer {

    private static final int port = 9797;
    private static final String path = API_V_1_QUERY_RANGE;
    public static final String USER_ID = "MOCK_USER_ID";
    private static final String TOKEN = "MOCK_APPTUIT_TOKEN";
    private static final String BASIC_AUTH_HEADER = "Basic TU9DS19VU0VSX0lEOk1PQ0tfQVBQVFVJVF9UT0tFTg==";
    private static final String BEARER_AUTH_HEADER = "Bearer " + TOKEN;

    private HttpServer httpServer;
    private List<HttpExchange> exchanges = new ArrayList<>();
    private List<String> requestBodies = new ArrayList<>();

    public MockServer() throws IOException {
      httpServer = HttpServer.create(new InetSocketAddress(port), 0);
      httpServer.createContext("/" + path, this::handleExchange);
    }

    public void start() {
      httpServer.start();
    }

    public void stop(int i) {
      httpServer.stop(i);
    }

    private URL getUrl() throws MalformedURLException {
      return getUrl(HttpURLConnection.HTTP_OK);
    }

    private URL getUrl(int code) throws MalformedURLException {
      String url = "http://localhost:" + port + "/";
      return new URL(url);
    }

    public List<HttpExchange> getExchanges() {
      return exchanges;
    }

    public List<String> getRequestBodies() {
      return requestBodies;
    }

    public void resetCapturedData() {
      exchanges.clear();
      requestBodies.clear();
    }

    private void handleExchange(HttpExchange exchange) throws IOException {
      exchanges.add(exchange);
      requestBodies.add(streamToString(exchange.getRequestBody()));

      String authorizationHeader = exchange.getRequestHeaders().getFirst("Authorization");
      if (!BASIC_AUTH_HEADER.equals(authorizationHeader) && !BEARER_AUTH_HEADER.equals(authorizationHeader)) {
        sendResponse(exchange, HttpURLConnection.HTTP_UNAUTHORIZED, "query-result-warnings.json");
        return;
      }
      if (requestBodies.get(0).matches("^start=[0-9]\\d*&end=[0-9]\\d*&step=(0|(-[1-9]\\d*))&query=MOCK\\+QUERY$")){
        sendResponse(exchange, HttpURLConnection.HTTP_OK, "query-result-unauthorised-step.json");
        return;
      }
      sendResponse(exchange, HttpURLConnection.HTTP_OK, "query-result-basic.json");
    }

    private void sendResponse(HttpExchange exchange, int status, String name) throws IOException {
      String response = streamToString(ClassLoader.getSystemResourceAsStream(name));
      exchange.sendResponseHeaders(status, response.length());
      exchange.getResponseBody().write(response.getBytes());
      exchange.close();
    }

    private static String streamToString(InputStream inputStream) throws IOException {
      return streamToString(inputStream, false);
    }

    private static String streamToString(InputStream inputStream, boolean unzip) throws IOException {
      if (unzip) {
        inputStream = new GZIPInputStream(inputStream);
      }
      return new Scanner(inputStream).useDelimiter("\0").next();
    }
  }

}
