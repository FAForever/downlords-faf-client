package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;



public class LoginPrefs {
  private final StringProperty endpoint = new SimpleStringProperty();
  private final StringProperty refreshToken = new SimpleStringProperty();
  private final BooleanProperty rememberMe = new SimpleBooleanProperty(true);

  public String getEndpoint() {
    return endpoint.get();
  }

  public StringProperty endpointProperty() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint.set(endpoint);
  }

  public String getRefreshToken() {
    return refreshToken.get();
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken.set(refreshToken);
  }

  public StringProperty refreshTokenProperty() {
    return refreshToken;
  }

  public boolean isRememberMe() {
    return rememberMe.get();
  }

  public BooleanProperty rememberMeProperty() {
    return rememberMe;
  }

  public void setRememberMe(boolean rememberMe) {
    this.rememberMe.set(rememberMe);
  }
}
