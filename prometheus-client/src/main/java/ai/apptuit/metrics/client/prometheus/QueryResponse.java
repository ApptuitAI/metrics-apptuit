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
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rajiv Shivane
 */
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
    public QueryResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
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
    public JsonElement serialize(QueryResult queryResult, Type type,
                                 JsonSerializationContext jsonSerializationContext) {
      QueryResult.TYPE resultType = queryResult.getType();
      if (resultType == QueryResult.TYPE.matrix) {
        JsonObject dataJson = new JsonObject();
        dataJson.add("resultType", new JsonPrimitive("matrix"));
        JsonElement resultJson = new Gson().toJsonTree(((MatrixResult) queryResult).getSeries());
        dataJson.add("result", resultJson);
        return dataJson;
      }
      return null;
    }
  }
}
