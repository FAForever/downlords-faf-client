package com.faforever.client.game;

import com.faforever.client.fxml.FxmlLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

public class CreateGameDialogFactoryImpl implements CreateGameDialogFactory{

  public static final String BEAN_NAME = "createGameDialogController";

  @Autowired
  AutowireCapableBeanFactory applicationContext;

  @Autowired FxmlLoader fxmlLoader;


  @Override
  public CreateGameDialogController create() {
    CreateGameDialogController controller = fxmlLoader.loadAndGetController("create_game_dialog.fxml");
    applicationContext.destroyBean(BEAN_NAME);
    applicationContext.autowireBean(controller);
    applicationContext.initializeBean(controller, BEAN_NAME);
    return controller;
  }
}
