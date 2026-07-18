package com.faforever.client.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The response of the OAuth 2.0 device authorization endpoint (RFC 8628). The client shows the {@code userCode} and
 * {@code verificationUri} to the user (or opens {@code verificationUriComplete} directly) and then polls the token
 * endpoint until the user approved the request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeviceCodeResponse(
    @JsonProperty("device_code") String deviceCode,
    @JsonProperty("user_code") String userCode,
    @JsonProperty("verification_uri") String verificationUri,
    @JsonProperty("verification_uri_complete") String verificationUriComplete,
    @JsonProperty("expires_in") int expiresIn,
    @JsonProperty("interval") Integer interval
) {

  /** Polling interval in seconds. Defaults to 5 as mandated by RFC 8628 when the server omits it. */
  public int intervalOrDefault() {
    return interval != null ? interval : 5;
  }
}
