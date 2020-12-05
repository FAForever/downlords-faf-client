package com.faforever.client.teammatchmaking;

import com.faforever.client.fx.Controller;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
  private final TeamMatchmakingService teamMatchmakingService;
  private final ObservableList<String> playerList = FXCollections.observableArrayList();
  private final FilteredList<String> filteredPlayerList = new FilteredList<>(playerList, p -> true);
  private final SortedList<String> sortedPlayerList = new SortedList<>(filteredPlayerList, Comparator.naturalOrder());

  public Pane root;
  public TextField playerTextField;
  public ListView<String> playersListView;
  public ListView<String> invitedPlayersListView;

  @Override
  public void initialize() {
    playerTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      playersListView.getSelectionModel().selectFirst();
    });
    playerList.setAll(getPlayerNames());

    filteredPlayerList.predicateProperty().bind(Bindings.createObjectBinding(() -> p -> {
          if (playerService.getCurrentPlayer()
              .map(Player::getUsername)
              .map(n -> n.equals(p))
              .orElse(true)) {
            return false;
          }

          if (playerTextField.getText().isBlank()) {
            return playerService.getPlayerForUsername(p)
                .map(player -> player.getSocialStatus() == SocialStatus.FRIEND)
                .orElse(false);
          } else {
            return p.toLowerCase().contains(playerTextField.getText().toLowerCase());
          }
        }, playerTextField.textProperty()
    ));

    playersListView.setItems(sortedPlayerList);
    playersListView.getSelectionModel().selectFirst();
    requestFocus();
  }

  public void requestFocus() {
    Platform.runLater(() -> {
      if (!playerTextField.isFocused()) {
        playerTextField.requestFocus();
        requestFocus(); // Yes, this is a loop, because it often fails on the first try
      }
    });
  }

  private Collection<String> getPlayerNames() {
    return playerService.getPlayerNames();
  }

  @Override
  public Pane getRoot() {
    return root;
  }

  public void onInviteButtonClicked(ActionEvent event) {
    invite();
  }

  private void invite() {
    playersListView.getSelectionModel().getSelectedItems().forEach(player -> {
      teamMatchmakingService.invitePlayer(player);
      invitedPlayersListView.getItems().add(player);
    });
    playerTextField.setText("");
    Platform.runLater(() -> playersListView.getSelectionModel().selectFirst());
  }

  public void onKeyPressed(KeyEvent keyEvent) {
    if (keyEvent.getCode() == KeyCode.ENTER) {
      invite();
    }
    if (keyEvent.getCode() == KeyCode.UP) {
      playersListView.getSelectionModel().selectPrevious();
    }
    if (keyEvent.getCode() == KeyCode.DOWN) {
      playersListView.getSelectionModel().selectNext();
    }
  }
}
