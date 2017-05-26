package com.faforever.client.user;

import com.faforever.client.task.CompletableTask;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;

import java.util.concurrent.CompletableFuture;

public interface UserService {

  BooleanProperty loggedInProperty();

  CompletableFuture<Void> login(String username, String password, boolean autoLogin);

  String getUsername();

  String getPassword();

  Integer getUserId();

  void cancelLogin();

  void logOut();

  CompletableTask<Void> changePassword(String currentPassword, String newPassword);

}
