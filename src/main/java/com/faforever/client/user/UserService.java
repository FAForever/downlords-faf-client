package com.faforever.client.user;

import com.faforever.client.task.CompletableTask;

import java.util.concurrent.CompletableFuture;

public interface UserService {

  CompletableFuture<Void> login(String username, String password, boolean autoLogin);

  String getUsername();

  String getPassword();

  Integer getUserId();

  void cancelLogin();

  void logOut();

  CompletableTask<Void> changePassword(String currentPassword, String newPassword);

}
