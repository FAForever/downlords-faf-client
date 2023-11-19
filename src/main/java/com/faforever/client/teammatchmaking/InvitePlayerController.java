package com.faforever.client.teammatchmaking;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.IconButtonListCell;
import com.faforever.client.fx.IconButtonListCell.IconButtonListCellControllerAndItem;
import com.faforever.client.fx.IconButtonListCellController;
import com.faforever.client.fx.NodeController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.list.NoSelectionModelListView;
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
public class InvitePlayerController extends NodeController<Pane> {

  private final PlayerService playerService;
  private final UiService uiService;
  private final TeamMatchmakingService teamMatchmakingService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final ObservableList<String> playerList = FXCollections.observableArrayList();
  private final FilteredList<String> filteredPlayerList = new FilteredList<>(playerList, player -> true);
  private final SortedList<String> sortedPlayerList = new SortedList<>(filteredPlayerList, Comparator.naturalOrder());

  public Pane root;
  public TextField playerTextField;
  public ListView<String> playersListView;
  public ListView<String> invitedPlayersListView;

  @Override
  protected void onInitialize() {
    playerTextField.textProperty().addListener((observable, oldValue, newValue) ->
        playersListView.getSelectionModel().selectFirst()
    );
    playerList.setAll(getPlayerNames());

    filteredPlayerList.predicateProperty().bind(playerTextField.textProperty().map(text -> playerName -> {
      if (playerService.getCurrentPlayer().getUsername().equals(playerName)) {
        return false;
      }

      if (text.isBlank()) {
        return playerService.getPlayerByNameIfOnline(playerName)
            .map(player -> player.getSocialStatus() == SocialStatus.FRIEND)
            .orElse(false);
      } else {
        return playerName.toLowerCase().contains(playerTextField.getText().toLowerCase());
      }
    }));

    invitedPlayersListView.setSelectionModel(new NoSelectionModelListView<>());
    invitedPlayersListView.setCellFactory(param -> new IconButtonListCell<>(this::invitedPlayerListCellConfiguration, uiService, fxApplicationThreadExecutor));

    playersListView.setSelectionModel(new NoSelectionModelListView<>());
    playersListView.setCellFactory(param -> new IconButtonListCell<>(this::playerListCellConfiguration, uiService, fxApplicationThreadExecutor));
    playersListView.setItems(sortedPlayerList);
    playersListView.getSelectionModel().selectFirst();
    requestFocus();
  }

  private void invitedPlayerListCellConfiguration(IconButtonListCellControllerAndItem<String> iconButtonListCellControllerAndItem) {
    IconButtonListCellController iconButtonListCellController = iconButtonListCellControllerAndItem.iconButtonListCellController();
    String playerName = iconButtonListCellControllerAndItem.item();
    Button iconButton = iconButtonListCellController.getIconButton();
    iconButton.setDisable(true);
    iconButtonListCellController.getIconRegion().getStyleClass().add("added-person");
    iconButtonListCellController.getLabel().setText(playerName);
  }

  private void playerListCellConfiguration(IconButtonListCellControllerAndItem<String> iconButtonListCellControllerAndItem) {
    IconButtonListCellController iconButtonListCellController = iconButtonListCellControllerAndItem.iconButtonListCellController();
    String playerName = iconButtonListCellControllerAndItem.item();
    Button iconButton = iconButtonListCellController.getIconButton();
    iconButton.setOnMouseClicked(event -> invite(playerName));
    iconButtonListCellController.getIconRegion().getStyleClass().add("add-person");
    iconButtonListCellController.getLabel().setText(playerName);
  }

  public void requestFocus() {
    fxApplicationThreadExecutor.execute(() -> playerTextField.requestFocus());
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
