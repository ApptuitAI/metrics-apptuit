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

import com.google.gson.Gson;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Rajiv Shivane
 */
public class QueryResponseTest {
  @Test
  public void testBasicQueryResponse() throws Exception {
    QueryResponse queryResponse = loadQueryResponse("query-result-basic.json");
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
    Map<String, String> labels = ts.getLabels();
    assertEquals(7, labels.size());
    assertNotNull(labels.get("__name__"));
    assertNotNull(labels.get("app"));
    assertNotNull(labels.get("cluster"));
    assertNotNull(labels.get("container"));
    assertNotNull(labels.get("deployment"));
    assertNotNull(labels.get("pod"));
    assertNotNull(labels.get("source"));
    assertEquals(1196, ts.getValues().size());
    TimeSeries.Tuple tuple = ts.getValues().get(0);
    assertTrue(tuple.getTimestamp() >= 1570573740000L && tuple.getTimestamp() <= 1570577325000L);
    assertNotNull(tuple.getValue());
    assertEquals(Long.valueOf(tuple.getValue()), (Long) tuple.getValueAsLong());
  }

  @Test
  public void testAggregateByQueryResponse() throws Exception {
    QueryResponse queryResponse = loadQueryResponse("query-result-aggregate-by.json");
    assertNull(queryResponse.getError());
    assertNull(queryResponse.getErrorType());
    assertEquals(0, queryResponse.getWarnings().size());
    assertEquals(AbstractResponse.STATUS.success, queryResponse.getStatus());
    QueryResult result = queryResponse.getResult();
    assertNotNull(result);
    assertEquals(QueryResult.TYPE.matrix, result.getType());
    List<TimeSeries> series = ((MatrixResult) result).getSeries();
    assertEquals(10, series.size());
    TimeSeries ts = series.get(0);
    Map<String, String> labels = ts.getLabels();
    assertEquals(1, labels.size());
    assertNotNull(labels.get("app"));
    assertEquals(1196, ts.getValues().size());
    TimeSeries.Tuple tuple = ts.getValues().get(0);
    assertTrue(tuple.getTimestamp() >= 1570573740000L && tuple.getTimestamp() <= 1570577325000L);
    assertNotNull(tuple.getValue());
    assertEquals(Double.valueOf(tuple.getValue()), (Double) tuple.getValueAsDouble());
  }


  @Test
  public void testAggregateQueryResponse() throws Exception {
    QueryResponse queryResponse = loadQueryResponse("query-result-aggregate.json");
    assertNull(queryResponse.getError());
    assertNull(queryResponse.getErrorType());
    assertEquals(0, queryResponse.getWarnings().size());
    assertEquals(AbstractResponse.STATUS.success, queryResponse.getStatus());
    QueryResult result = queryResponse.getResult();
    assertNotNull(result);
    assertEquals(QueryResult.TYPE.matrix, result.getType());
    List<TimeSeries> series = ((MatrixResult) result).getSeries();
    assertEquals(1, series.size());
    TimeSeries ts = series.get(0);
    Map<String, String> labels = ts.getLabels();
    assertEquals(0, labels.size());
    assertEquals(1196, ts.getValues().size());
    TimeSeries.Tuple tuple = ts.getValues().get(0);
    assertTrue(tuple.getTimestamp() >= 1570573740000L && tuple.getTimestamp() <= 1570577325000L);
    assertNotNull(tuple.getValue());
    assertEquals(Double.valueOf(tuple.getValue()), (Double) tuple.getValueAsDouble());
  }


  @Test
  public void testMissingMetricQueryResponse() throws Exception {
    QueryResponse queryResponse = loadQueryResponse("query-result-missing-metric.json");
    assertNull(queryResponse.getError());
    assertNull(queryResponse.getErrorType());
    assertEquals(0, queryResponse.getWarnings().size());
    assertEquals(AbstractResponse.STATUS.success, queryResponse.getStatus());
    QueryResult result = queryResponse.getResult();
    assertNotNull(result);
    assertEquals(QueryResult.TYPE.matrix, result.getType());
    List<TimeSeries> series = ((MatrixResult) result).getSeries();
    assertEquals(0, series.size());
  }


  @Test
  public void testMissingQueryResponseWithWarning() throws Exception {
    QueryResponse queryResponse = loadQueryResponse("query-result-warnings.json");
    assertNull(queryResponse.getError());
    assertNull(queryResponse.getErrorType());
    assertEquals(1, queryResponse.getWarnings().size());
    assertEquals("server returned HTTP status 401 Unauthorized", queryResponse.getWarnings().get(0));
    assertEquals(AbstractResponse.STATUS.success, queryResponse.getStatus());
    QueryResult result = queryResponse.getResult();
    assertNotNull(result);
    assertEquals(QueryResult.TYPE.matrix, result.getType());
    List<TimeSeries> series = ((MatrixResult) result).getSeries();
    assertEquals(0, series.size());
  }


  @Test
  public void testMissingQueryResponseWithError() throws Exception {
    QueryResponse queryResponse = loadQueryResponse("query-result-parse-error.json");
    assertEquals("parse error at char 21: unclosed left parenthesis", queryResponse.getError());
    assertEquals("bad_data", queryResponse.getErrorType());
    assertEquals(0, queryResponse.getWarnings().size());
    assertEquals(AbstractResponse.STATUS.error, queryResponse.getStatus());
    QueryResult result = queryResponse.getResult();
    assertNull(result);
  }

  @Test
  public void testQueryResponseToString() throws Exception {
    QueryResponse queryResponse = loadQueryResponse("query-result-basic.json");
    QueryResponse clone = new Gson().fromJson(queryResponse.toString(), QueryResponse.class);
    assertEquals(queryResponse.getStatus(), clone.getStatus());
    assertEquals(((MatrixResult) queryResponse.getResult()).getSeries().size(),
        ((MatrixResult) clone.getResult()).getSeries().size());
  }

  private QueryResponse loadQueryResponse(String name) {
    InputStream stream = ClassLoader.getSystemResourceAsStream(name);
    Gson gson = new Gson();
    return gson.fromJson(new InputStreamReader(stream), QueryResponse.class);
  }

}
