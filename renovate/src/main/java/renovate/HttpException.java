package renovate;


import static renovate.Utils.checkNotNull;

/** Exception for an unexpected, non-2xx HTTP response. */
public class HttpException extends RuntimeException {
  private static String getMessage(Response<?> response) {
    checkNotNull(response, "response == null");
    return "HTTP " + response.code() + " " + response.message();
  }

  private final int code;
  private final String message;
  private final transient Response<?> response;

  public HttpException(Response<?> response) {
    super(getMessage(response));
    this.code = response.code();
    this.message = response.message();
    this.response = response;
  }

  /** HTTP status code. */
  public int code() {
    return code;
  }

  /** HTTP status message. */
  public String message() {
    return message;
  }

  /**
   * The full HTTP response. This may be null if the exception was serialized.
   */
  public Response<?> response() {
    return response;
  }
}
