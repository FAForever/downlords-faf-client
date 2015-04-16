package com.faforever.client.chat;

import com.faforever.client.fxml.FxmlLoader;
import org.springframework.beans.factory.annotation.Autowired;

public class UserInfoWindowFactoryImpl implements UserInfoWindowFactory {

  @Autowired
  FxmlLoader fxmlLoader;

  @Override
  public UserInfoWindow create() {
    return fxmlLoader.loadAndGetController("user_info_window.fxml");
  }
}
