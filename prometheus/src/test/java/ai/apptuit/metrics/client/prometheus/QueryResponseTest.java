package ai.apptuit.metrics.client.prometheus;

import com.google.gson.Gson;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

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
    assertEquals(Long.valueOf(tuple.getValue()), (Long)tuple.getValueAsLong());
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
    assertEquals(Double.valueOf(tuple.getValue()), (Double)tuple.getValueAsDouble());
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
    assertEquals(Double.valueOf(tuple.getValue()), (Double)tuple.getValueAsDouble());
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

  private QueryResponse loadQueryResponse(String name) {
    InputStream stream = ClassLoader.getSystemResourceAsStream(name);
    Gson gson = new Gson();
    return gson.fromJson(new InputStreamReader(stream), QueryResponse.class);
  }

}
