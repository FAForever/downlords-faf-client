package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class LoginPrefs {

  private final StringProperty username;
  @Deprecated
  private final StringProperty password;
  @Deprecated
  private final BooleanProperty autoLogin;
  private final StringProperty refreshToken;
  private final ObjectProperty<RememberMeType> rememberMeType;

  public LoginPrefs() {
    username = new SimpleStringProperty();
    password = new SimpleStringProperty();
    autoLogin = new SimpleBooleanProperty(false);
    refreshToken = new SimpleStringProperty();
    rememberMeType = new SimpleObjectProperty<>(RememberMeType.NEVER);
  }

  public LoginPrefs setUsername(String username) {
    this.username.set(username);
    return this;
  }

  public String getUsername() {
    return username.get();
  }

  @Deprecated
  public String getPassword() {
    return password.get();
  }

  public LoginPrefs setPassword(String password) {
    this.password.set(password);
    return this;
  }

  public StringProperty usernameProperty() {
    return username;
  }

  @Deprecated
  public StringProperty passwordProperty() {
    return password;
  }

  @Deprecated
  public boolean isAutoLogin() {
    return autoLogin.get();
  }

  @Deprecated
  public LoginPrefs setAutoLogin(boolean autoLogin) {
    this.autoLogin.set(autoLogin);
    return this;
  }

  @Deprecated
  public BooleanProperty autoLoginProperty() {
    return autoLogin;
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

  public RememberMeType getRememberMeType() {
    return rememberMeType.get();
  }

  public void setRememberMeType(RememberMeType rememberMeType) {
    this.rememberMeType.set(rememberMeType);
  }

  public ObjectProperty<RememberMeType> rememberMeTypeProperty() {
    return rememberMeType;
  }

  public enum RememberMeType {
    NEVER,
    SHORT,
    LONG
  }
}
