package ai.apptuit.metrics.client.prometheus;

public class ResponseStatusException extends Exception {

  private int httpStatus;
  private String body;

  public ResponseStatusException(int httpStatus, String body) {
    this(httpStatus, body, null);
  }

  public ResponseStatusException(int httpStatus, String body, Throwable cause) {
    super(cause);
    this.httpStatus = httpStatus;
    this.body = body;
  }

  public String getMessage() {
    return this.httpStatus + (this.body != null ? " [" + this.body + "]" : "");
  }

  public String getResponseBody() {
    return body;
  }
}
