package com.faforever.client.teammatchmaking;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowLadderMapsEvent;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.teammatchmaking.Party.PartyMember;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXListView;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static javafx.beans.binding.Bindings.createBooleanBinding;

@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class TeamMatchmakingController extends AbstractViewController<Node> {

  private final PlayerService playerService;
  private final I18n i18n;
  private final UiService uiService;
  private final TeamMatchmakingService teamMatchmakingService;
  private final EventBus eventBus;
  @FXML
  public JFXButton invitePlayerButton;

  @FXML
  public StackPane teamMatchmakingRoot;
  @FXML
  public JFXListView<PartyMember> playerListView;
  @FXML
  public JFXButton leavePartyButton;
  @FXML
  public JFXButton readyButton;
  @FXML
  public Label refreshingLabel;

  @Override
  public void initialize() {
    playerListView.setCellFactory(listView -> new PartyMemberItemListCell(uiService));
    playerListView.setItems(teamMatchmakingService.getParty().getMembers());

    invitePlayerButton.managedProperty().bind(invitePlayerButton.visibleProperty());
    invitePlayerButton.visibleProperty().bind(createBooleanBinding(
        () -> teamMatchmakingService.getParty().getOwner().getId() == playerService.getCurrentPlayer().map(Player::getId).orElse(-1),
        teamMatchmakingService.getParty().ownerProperty(),
        playerService.currentPlayerProperty()
    ));
    leavePartyButton.disableProperty().bind(createBooleanBinding(() -> teamMatchmakingService.getParty().getMembers().size() <= 1, teamMatchmakingService.getParty().getMembers()));

    teamMatchmakingService.getParty().getMembers().addListener((Observable o) -> {
      if (isSelfReady()) {
        readyButton.getStyleClass().removeAll("party-ready-button-not-ready");
        readyButton.getStyleClass().add("party-ready-button-ready");
        readyButton.setText(i18n.get("teammatchmaking.ready"));
      } else {
        readyButton.getStyleClass().removeAll("party-ready-button-ready");
        readyButton.getStyleClass().add("party-ready-button-not-ready");
        readyButton.setText(i18n.get("teammatchmaking.notReady"));
      }

      refreshingLabel.setVisible(false);
    });
  }

  @Override
  public Node getRoot() {
    return teamMatchmakingRoot;
  }

  public void showMatchmakingMaps(ActionEvent actionEvent) {
    eventBus.post(new ShowLadderMapsEvent());//TODO show team matchmaking maps and not ladder maps
  }

  public void onInvitePlayerButtonClicked(ActionEvent actionEvent) {
    InvitePlayerController invitePlayerController = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_invite_player.fxml");
    Pane root = invitePlayerController.getRoot();
    JFXDialog dialog = uiService.showInDialog(teamMatchmakingRoot, root, i18n.get("teammatchmaking.invitePlayer"));
  }

  public void onEnterQueueButtonClicked(ActionEvent actionEvent) {
    //TODO
  }

  public void onLeavePartyButtonClicked(ActionEvent actionEvent) {
    teamMatchmakingService.leaveParty();
  }

  public void onLeaveQueueButtonClicked(ActionEvent actionEvent) {
    //TODO
  }

  public void onReadyButtonClicked(ActionEvent actionEvent) {
    if (!isSelfReady()) {
      teamMatchmakingService.readyParty();
    } else {
      teamMatchmakingService.unreadyParty();
    }

    refreshingLabel.setVisible(true);
  }

  public boolean isSelfReady() {
    return teamMatchmakingService.getParty().getMembers().stream()
        .anyMatch(p -> p.getPlayer().getId() == playerService.getCurrentPlayer().map(Player::getId).orElse(-1)
            && p.isReady());
  }
}
