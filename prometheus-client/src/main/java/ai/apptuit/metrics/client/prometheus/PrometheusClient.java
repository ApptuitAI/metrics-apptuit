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
import com.google.gson.JsonSyntaxException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Rajiv Shivane
 */
public class PrometheusClient {

  public static final String API_V_1_QUERY_RANGE = "api/v1/query_range";

  private static final Logger LOGGER = Logger.getLogger(PrometheusClient.class.getName());

  private static final int[] STEPS = new int[]{5, 15, 60, 300, 900, 3600};
  private static final int CONNECT_TIMEOUT_MS = 5000;
  private static final int SOCKET_TIMEOUT_MS = 15000;

  private static final URL DEFAULT_PROMQL_API_URI;

  static {
    try {
      DEFAULT_PROMQL_API_URI = new URL("https://api.apptuit.ai/prometheus/");
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
  }

  private String userId;
  private final String token;
  private final URL prometheusEndpoint;

  public PrometheusClient(String token) {
    this(token, DEFAULT_PROMQL_API_URI);
  }

  public PrometheusClient(String token, URL prometheusEndpoint) {
    this(null, token, prometheusEndpoint);
  }

  public PrometheusClient(String userId, String token, URL prometheusEndpoint) {
    this.userId = userId;
    this.token = token;
    this.prometheusEndpoint = prometheusEndpoint;
  }

  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT", justification = "Findbugs limitation https://sourceforge.net/p/findbugs/bugs/1298/")
  public QueryResponse query(long startEpochMillis, long endEpochMillis, String promQueryString)
      throws IOException, ResponseStatusException, URISyntaxException {

    URL queryEndpoint = prometheusEndpoint.toURI().resolve(API_V_1_QUERY_RANGE).toURL();
    HttpURLConnection urlConnection = (HttpURLConnection) queryEndpoint.openConnection();

    urlConnection.setConnectTimeout(CONNECT_TIMEOUT_MS);
    urlConnection.setReadTimeout(SOCKET_TIMEOUT_MS);
    urlConnection.setRequestMethod("POST");

    urlConnection.setRequestProperty("Authorization", getAuthHeader());

    String urlParameters = "start=" + (startEpochMillis / 1000)
        + "&end=" + (endEpochMillis / 1000)
        + "&step=" + getStepSize(endEpochMillis - startEpochMillis)
        + "&query=" + URLEncoder.encode(promQueryString, "UTF-8");

    urlConnection.setDoOutput(true);
    try (DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream())) {
      wr.writeBytes(urlParameters);
      wr.flush();
    }

    StringBuilder sb = new StringBuilder();
    int responseCode = urlConnection.getResponseCode();
    LOGGER.log(Level.FINE, "Sending 'POST' request to URL : " + queryEndpoint);
    LOGGER.log(Level.FINE, "Post parameters : " + urlParameters);

    InputStream inputStr = (responseCode < HttpURLConnection.HTTP_BAD_REQUEST) ? urlConnection.getInputStream()
        : urlConnection.getErrorStream();
    try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStr, StandardCharsets.UTF_8))) {
      String line;
      while ((line = in.readLine()) != null) {
        sb.append(line);
      }
    }

    if (sb.indexOf("server returned HTTP status 401 Unauthorized") > 0) {
      responseCode = 401;
    }

    String response = sb.toString();
    switch (responseCode) {
      case HttpURLConnection.HTTP_OK:
        return new Gson().fromJson(response, QueryResponse.class);
      case HttpURLConnection.HTTP_BAD_REQUEST:
      case HttpURLConnection.HTTP_UNAVAILABLE:
      case 422:
        try {
          return new Gson().fromJson(response, QueryResponse.class);
        } catch (JsonSyntaxException e) {
          // NOT in prom format, fall through and raise an exception
        }
      default:
        throw new ResponseStatusException(responseCode, response);
    }
  }

  private String getAuthHeader() {
    if (userId == null) {
      return "Bearer " + token;
    } else {
      byte[] userPass = (userId + ":" + token).getBytes(StandardCharsets.UTF_8);
      return "Basic " + Base64.getEncoder().encodeToString(userPass);
    }
  }

  static long getStepSize(long queryRangeMillis) {
    long step = queryRangeMillis / 1000; //convert to seconds
    step = step / 1000; //desired points per series

    if (step < 1) {
      return 1;
    }
    int lowerStep = 1;
    for (int predefinedStep : STEPS) {
      if (step <= predefinedStep) {
        break;
      }
      lowerStep = predefinedStep;
    }
    return lowerStep * (step / lowerStep);
  }
}
