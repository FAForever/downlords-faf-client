package com.faforever.client.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The response of the OAuth 2.0 device authorization endpoint (RFC 8628). The client shows the {@code userCode} and
 * {@code verificationUri} to the user (or opens {@code verificationUriComplete} directly) and then polls the token
 * endpoint until the user approved the request.
 */
public record DeviceCodeResponse(
    @JsonProperty("device_code") String deviceCode,
    @JsonProperty("user_code") String userCode,
    @JsonProperty("verification_uri") String verificationUri,
    @JsonProperty("verification_uri_complete") String verificationUriComplete,
    @JsonProperty("expires_in") int expiresIn,
    @JsonProperty("interval") Integer interval
) {

  private static final int DEFAULT_INTERVAL_SECONDS = 5;

  /**
   * Compact constructor: normalize a missing interval to the RFC 8628 default (5s), and reject a server-provided
   * interval that is not positive since it is used directly as a polling delay.
   */
  public DeviceCodeResponse {
    if (interval == null) {
      interval = DEFAULT_INTERVAL_SECONDS;
    } else if (interval <= 0) {
      throw new IllegalArgumentException("interval must be greater than zero but was " + interval);
    }
  }
}
