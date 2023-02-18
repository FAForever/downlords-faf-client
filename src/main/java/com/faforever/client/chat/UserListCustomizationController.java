package com.faforever.client.chat;

import com.faforever.client.fx.Controller;
import com.faforever.client.preferences.ChatPrefs;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class UserListCustomizationController implements Controller<VBox> {

  private final ChatPrefs chatPrefs;

  public VBox root;
  public CheckBox showMapNameCheckBox;
  public CheckBox showMapPreviewCheckBox;

  @Override
  public void initialize() {
    showMapNameCheckBox.selectedProperty().bindBidirectional(chatPrefs.showMapNameProperty());
    showMapPreviewCheckBox.selectedProperty().bindBidirectional(chatPrefs.showMapPreviewProperty());
  }

  @Override
  public VBox getRoot() {
    return root;
  }
}
