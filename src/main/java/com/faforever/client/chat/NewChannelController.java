package com.faforever.client.chat;


import com.faforever.client.fx.Controller;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class NewChannelController implements Controller {
  @FXML
  public VBox root;
  @FXML
  public TextField channelNameTextField;

  @Override
  public Object getRoot() {
    return root;
  }

  public TextField getChannelNameTextField() {
    return channelNameTextField;
  }
}
