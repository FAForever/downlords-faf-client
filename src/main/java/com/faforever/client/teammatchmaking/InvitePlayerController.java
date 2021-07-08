package com.faforever.client.teammatchmaking;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.IconButtonListCell;
import com.faforever.client.fx.IconButtonListCell.IconButtonListCellControllerAndItem;
import com.faforever.client.fx.IconButtonListCellController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.list.NoSelectionModel;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class InvitePlayerController implements Controller<Pane> {

  private final PlayerService playerService;
  private final UiService uiService;
  private final TeamMatchmakingService teamMatchmakingService;
  private final ObservableList<String> playerList = FXCollections.observableArrayList();
  private final FilteredList<String> filteredPlayerList = new FilteredList<>(playerList, player -> true);
  private final SortedList<String> sortedPlayerList = new SortedList<>(filteredPlayerList, Comparator.naturalOrder());

  public Pane root;
  public TextField playerTextField;
  public ListView<String> playersListView;
  public ListView<String> invitedPlayersListView;

  @Override
  public void initialize() {
    playerTextField.textProperty().addListener((observable, oldValue, newValue) ->
        playersListView.getSelectionModel().selectFirst()
    );
    playerList.setAll(getPlayerNames());

    filteredPlayerList.predicateProperty().bind(Bindings.createObjectBinding(() -> playerName -> {
      if (playerService.getCurrentPlayer().getUsername().equals(playerName)) {
        return false;
      }

      if (playerTextField.getText().isBlank()) {
        return playerService.getPlayerByNameIfOnline(playerName)
            .map(player -> player.getSocialStatus() == SocialStatus.FRIEND)
            .orElse(false);
          } else {
        return playerName.toLowerCase().contains(playerTextField.getText().toLowerCase());
          }
        }, playerTextField.textProperty()
    ));

    invitedPlayersListView.setSelectionModel(new NoSelectionModel<>());
    invitedPlayersListView.setCellFactory(param -> new IconButtonListCell<>(this::invitedPlayerListCellConfiguration, uiService));

    playersListView.setSelectionModel(new NoSelectionModel<>());
    playersListView.setCellFactory(param -> new IconButtonListCell<>(this::playerListCellConfiguration, uiService));
    playersListView.setItems(sortedPlayerList);
    playersListView.getSelectionModel().selectFirst();
    requestFocus();
  }

  private void invitedPlayerListCellConfiguration(IconButtonListCellControllerAndItem<String> iconButtonListCellControllerAndItem) {
    IconButtonListCellController iconButtonListCellController = iconButtonListCellControllerAndItem.getIconButtonListCellController();
    String playerName = iconButtonListCellControllerAndItem.getItem();
    Button iconButton = iconButtonListCellController.getIconButton();
    iconButton.setDisable(true);
    iconButtonListCellController.getIconRegion().getStyleClass().add("added-person");
    iconButtonListCellController.getLabel().setText(playerName);
  }

  private void playerListCellConfiguration(IconButtonListCellControllerAndItem<String> iconButtonListCellControllerAndItem) {
    IconButtonListCellController iconButtonListCellController = iconButtonListCellControllerAndItem.getIconButtonListCellController();
    String playerName = iconButtonListCellControllerAndItem.getItem();
    Button iconButton = iconButtonListCellController.getIconButton();
    iconButton.setOnMouseClicked(event -> invite(playerName));
    iconButtonListCellController.getIconRegion().getStyleClass().add("add-person");
    iconButtonListCellController.getLabel().setText(playerName);
  }

  public void requestFocus() {
    JavaFxUtil.runLater(() -> playerTextField.requestFocus());
  }

  private Collection<String> getPlayerNames() {
    return playerService.getPlayerNames();
  }

  @Override
  public Pane getRoot() {
    return root;
  }

  private void invite(String playerName) {
    teamMatchmakingService.invitePlayer(playerName);
    invitedPlayersListView.getItems().add(playerName);
  }
}
