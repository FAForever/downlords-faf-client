package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LoginPrefs {

  private final StringProperty username;
  private final StringProperty password;
  private final BooleanProperty autoLogin;

  public LoginPrefs() {
    username = new SimpleStringProperty();
    password = new SimpleStringProperty();
    autoLogin = new SimpleBooleanProperty();
  }

  public String getUsername() {
    return username.get();
  }

  public StringProperty usernameProperty() {
    return username;
  }

  public LoginPrefs setUsername(String username) {
    this.username.set(username);
    return this;
  }

  public String getPassword() {
    return password.get();
  }

  public StringProperty passwordProperty() {
    return password;
  }

  public LoginPrefs setPassword(String password) {
    this.password.set(password);
    return this;
  }

  public boolean getAutoLogin() {
    return autoLogin.get();
  }

  public BooleanProperty autoLoginProperty() {
    return autoLogin;
  }

  public LoginPrefs setAutoLogin(boolean autoLogin) {
    this.autoLogin.set(autoLogin);
    return this;
  }
}
