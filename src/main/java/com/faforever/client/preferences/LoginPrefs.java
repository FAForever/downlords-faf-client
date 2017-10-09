package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j

public class LoginPrefs {

  private static final String KEY = System.getProperty("user.name");

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

  public LoginPrefs setUsername(String username) {
    this.username.set(username);
    return this;
  }

  public StringProperty usernameProperty() {
    return username;
  }

  public String getPassword() {
    // FIXME remove poor man's security once refresh tokens are implemented
    try {
      return deObfuscate(password.get());
    } catch (Exception e) {
      log.warn("Could not deobfuscate password", e);
      setAutoLogin(false);
      return null;
    }
  }

  public LoginPrefs setPassword(String password) {
    // FIXME remove poor man's security once refresh tokens are implemented
    this.password.set(obfuscate(password));
    return this;
  }

  public StringProperty passwordProperty() {
    return password;
  }

  public boolean getAutoLogin() {
    return autoLogin.get();
  }

  public LoginPrefs setAutoLogin(boolean autoLogin) {
    this.autoLogin.set(autoLogin);
    return this;
  }

  public BooleanProperty autoLoginProperty() {
    return autoLogin;
  }

  private String obfuscate(String string) {
    if (string == null) {
      return null;
    }
    char[] result = new char[string.length()];
    for (int i = 0; i < string.length(); i++) {
      result[i] = (char) (string.charAt(i) + KEY.charAt(i % KEY.length()));
    }

    return Base64.getEncoder().encodeToString(new String(result).getBytes(StandardCharsets.UTF_8));
  }

  private String deObfuscate(String string) {
    if (string == null) {
      return null;
    }
    String innerString = new String(Base64.getDecoder().decode(string), StandardCharsets.UTF_8);
    char[] result = new char[innerString.length()];
    for (int i = 0; i < innerString.length(); i++) {
      result[i] = (char) (innerString.charAt(i) - KEY.charAt(i % KEY.length()));
    }

    return new String(result);
  }
}
