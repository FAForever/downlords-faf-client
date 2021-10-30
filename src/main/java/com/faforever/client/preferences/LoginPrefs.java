package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LoginPrefs {
  StringProperty refreshToken = new SimpleStringProperty();
  BooleanProperty rememberMe = new SimpleBooleanProperty(true);

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
