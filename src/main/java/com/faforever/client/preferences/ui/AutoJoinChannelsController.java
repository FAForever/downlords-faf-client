package com.faforever.client.preferences.ui;

import com.faforever.client.fx.Controller;
import com.faforever.client.preferences.PreferencesService;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AutoJoinChannelsController implements Controller<Node> {
  private final PreferencesService preferencesService;

  public TextField channelTextField;
  public Button addChannelButton;
  public ListView<String> channelListView;
  public GridPane autoJoinChannelsSettingsRoot;
  public ListView<String> removeListView;
  public Button removeButton;

  public AutoJoinChannelsController(PreferencesService preferencesService) {
    this.preferencesService = preferencesService;
  }

  public void initialize() {
    channelListView.setItems(preferencesService.getPreferences().getChat().getAutoJoinChannels());
    removeButton.visibleProperty().bind(channelListView.getSelectionModel().selectedItemProperty().isNotNull());
  }

  public Node getRoot() {
    return autoJoinChannelsSettingsRoot;
  }

  public void onAddChannelButtonPressed() {
    if (channelTextField.getText().isEmpty() || channelListView.getItems().contains(channelTextField.getText())) {
      return;
    }
    preferencesService.getPreferences().getChat().getAutoJoinChannels().add(channelTextField.getText());
    preferencesService.storeInBackground();
    channelTextField.clear();
  }

  public void onRemoveChannelPressed() {
    preferencesService.getPreferences().getChat().getAutoJoinChannels().remove((channelListView.getSelectionModel().getSelectedItem()));
    channelListView.getSelectionModel().clearSelection();
    preferencesService.storeInBackground();
  }
}
