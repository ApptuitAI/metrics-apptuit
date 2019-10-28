package ai.apptuit.metrics.client.prometheus;

import java.util.Collections;
import java.util.List;

public abstract class AbstractResponse {
  private STATUS status;
  private String errorType;
  private String error;
  private List<String> warnings;

  public STATUS getStatus() {
    return status;
  }

  public String getErrorType() {
    return errorType;
  }

  public String getError() {
    return error;
  }

  public List<String> getWarnings() {
    return warnings == null ? Collections.emptyList() : Collections.unmodifiableList(warnings);
  }

  public enum STATUS {success, error}
}
