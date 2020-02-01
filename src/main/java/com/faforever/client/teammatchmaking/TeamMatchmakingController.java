package com.faforever.client.teammatchmaking;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.main.event.ShowLadderMapsEvent;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXListView;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;

import static javafx.beans.binding.Bindings.createBooleanBinding;

@Component
@Lazy
public class TeamMatchmakingController extends AbstractViewController<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final GameService gameService;
  private final PreferencesService preferencesService;
  private final PlayerService playerService;
  private final LeaderboardService leaderboardService;
  private final I18n i18n;
  private final ClientProperties clientProperties;
  private final UiService uiService;
  private final TeamMatchmakingService teamMatchmakingService;
  @FXML
  public JFXButton invitePlayerButton;


  @FXML
  public StackPane teamMatchmakingRoot;
  @FXML
  public JFXListView playerListView;
  @FXML
  public JFXButton leavePartyButton;
  @FXML
  public JFXButton readyButton;


  private EventBus eventBus;
//  private ObservableList<PartyPlayerItem> playerItems;

  @Inject
  public TeamMatchmakingController(GameService gameService,
                                   PreferencesService preferencesService,
                                   PlayerService playerService,
                                   LeaderboardService leaderboardService,
                                   I18n i18n, ClientProperties clientProperties,
                                   UiService uiService, TeamMatchmakingService teamMatchmakingService, EventBus eventBus) {
    this.gameService = gameService;
    this.preferencesService = preferencesService;
    this.playerService = playerService;
    this.leaderboardService = leaderboardService;
    this.i18n = i18n;
    this.clientProperties = clientProperties;
    this.uiService = uiService;
    this.teamMatchmakingService = teamMatchmakingService;
    this.eventBus = eventBus;

//    playerItems = FXCollections.observableArrayList();


  }

  @Override
  public void initialize() {
    super.initialize();

    //TODO
//    setSearching(false);
//    JavaFxUtil.addListener(gameService.searching1v1Property(), (observable, oldValue, newValue) -> setSearching(newValue));

//    playerItems.add(new PartyPlayerItem(playerService.getCurrentPlayer().get()));
//    playerItems.add(new PartyPlayerItem(playerService.getPlayerForUsername(playerService.getPlayerNames().stream().findAny().get()).get()));
//    playerItems.add(new PartyPlayerItem(playerService.getPlayerForUsername(playerService.getPlayerNames().stream().findAny().get()).get()));
//    playerItems.add(new PartyPlayerItem(playerService.getPlayerForUsername(playerService.getPlayerNames().stream().findAny().get()).get()));

    playerListView.setCellFactory(listView -> new PartyMemberItemListCell(uiService));
    playerListView.setItems(teamMatchmakingService.getParty().getMembers());

    invitePlayerButton.managedProperty().bind(invitePlayerButton.visibleProperty());
//    invitePlayerButton.visibleProperty().bind(teamMatchmakingService.getParty().ownerProperty().isEqualTo(playerService.getCurrentPlayer()));
    invitePlayerButton.visibleProperty().bind(createBooleanBinding(
        () -> teamMatchmakingService.getParty().getOwner().getId() == playerService.getCurrentPlayer().map(Player::getId).orElse(-1),
        teamMatchmakingService.getParty().ownerProperty(),
        playerService.currentPlayerProperty()
    ));
    leavePartyButton.disableProperty().bind(createBooleanBinding(() -> teamMatchmakingService.getParty().getMembers().size() <= 1, teamMatchmakingService.getParty().getMembers()));


    teamMatchmakingService.getParty().getMembers().addListener((Observable o) -> {
      if (isSelfReady()) {
        readyButton.getStyleClass().remove("party-ready-button-not-ready");
        readyButton.getStyleClass().add("party-ready-button-ready");
        readyButton.setText(i18n.get("teammatchmaking.ready"));
      } else {
        readyButton.getStyleClass().remove("party-ready-button-ready");
        readyButton.getStyleClass().add("party-ready-button-not-ready");
        readyButton.setText(i18n.get("teammatchmaking.notReady"));
      }
    });
  }

  // TODO: FACTION SELECTION + DISPLAY

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
    //TODO: on server create party when single
  }

  public boolean isSelfReady() {
    return teamMatchmakingService.getParty().getMembers().stream()
        .anyMatch(p -> p.getPlayer().getId() == playerService.getCurrentPlayer().map(Player::getId).orElse(-1)
            && p.isReady());
  }
}
