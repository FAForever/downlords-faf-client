package com.faforever.client.chat;

import com.faforever.client.fxml.FxmlLoader;
import org.springframework.beans.factory.annotation.Autowired;

public class UserInfoWindowFactoryImpl implements UserInfoWindowFactory {

  @Autowired
  FxmlLoader fxmlLoader;

  @Override
  public UserInfoWindowController create(PlayerInfoBean playerInfoBean) {
    UserInfoWindowController controller = fxmlLoader.loadAndGetController("user_info_window.fxml");
    controller.setPlayerInfoBean(playerInfoBean);
    return controller;
  }
}

