package ai.apptuit.metrics.client.prometheus;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class QueryResponse extends AbstractResponse {

  @JsonAdapter(QueryResponse.QueryResultDeserializer.class)
  private QueryResult data;

  public QueryResult getResult() {
    return data;
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }

  static class QueryResultDeserializer implements JsonDeserializer<QueryResult>, JsonSerializer<QueryResult> {
    @Override
    public QueryResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      JsonObject jsonObject = json.getAsJsonObject();
      JsonElement type = jsonObject.get("resultType");
      if (type != null && type.getAsString().equals("matrix")) {
        JsonElement result = jsonObject.get("result");
        Type listType = new TypeToken<ArrayList<TimeSeries>>() {
        }.getType();
        List<TimeSeries> series = new Gson().fromJson(result, listType);
        return new MatrixResult(series);
      }
      throw new UnsupportedOperationException("Unsupported type: [" + type + "]");
    }

    @Override
    public JsonElement serialize(QueryResult queryResult, Type type, JsonSerializationContext jsonSerializationContext) {
      QueryResult.TYPE resultType = queryResult.getType();
      if(resultType== QueryResult.TYPE.matrix){
        JsonObject dataJson = new JsonObject();
        dataJson.add("resultType", new JsonPrimitive("matrix"));
        JsonElement resultJson = new Gson().toJsonTree(((MatrixResult)queryResult).getSeries());
        dataJson.add("result", resultJson);
        return dataJson;
      }
      return null;
    }
  }
}
