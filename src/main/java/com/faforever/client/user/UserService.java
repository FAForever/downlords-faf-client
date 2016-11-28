package com.faforever.client.user;

import com.faforever.client.task.CompletableTask;
import javafx.beans.property.ReadOnlyStringProperty;

import java.util.concurrent.CompletionStage;

public interface UserService {

  CompletionStage<Void> login(String username, String password, boolean autoLogin);

  String getUsername();

  String getPassword();

  Integer getUserId();

  void cancelLogin();

  void logOut();

  ReadOnlyStringProperty currentUserProperty();

  CompletableTask<Void> changePassword(String currentPassword, String newPassword);
}
