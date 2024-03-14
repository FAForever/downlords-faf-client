package com.faforever.client.teammatchmaking;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.api.LeagueEntry;
import com.faforever.client.domain.server.MatchmakerQueueInfo;
import com.faforever.client.domain.server.PartyInfo.PartyMember;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.game.PlayerGameStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.MatchmakerPrefs;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.Faction;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class TeamMatchmakingController extends NodeController<Node> {

  public static final PseudoClass LEADER_PSEUDO_CLASS = PseudoClass.getPseudoClass("leader");
  public static final PseudoClass CHAT_AT_BOTTOM_PSEUDO_CLASS = PseudoClass.getPseudoClass("bottom");

  private final CountryFlagService countryFlagService;
  private final AvatarService avatarService;
  private final LeaderboardService leaderboardService;
  private final PlayerService playerService;
  private final I18n i18n;
  private final UiService uiService;
  private final TeamMatchmakingService teamMatchmakingService;
  private final MatchmakerPrefs matchmakerPrefs;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public StackPane teamMatchmakingRoot;
  public Button invitePlayerButton;
  public Button leavePartyButton;
  public Label refreshingLabel;
  public ToggleButton uefButton;
  public ToggleButton cybranButton;
  public ToggleButton aeonButton;
  public ToggleButton seraphimButton;
  public ImageView avatarImageView;
  public ImageView countryImageView;
  public Label clanLabel;
  public Label usernameLabel;
  public Label gameCountLabel;
  public Label leagueLabel;
  public FlowPane queuePane;
  public GridPane partyMemberPane;
  public VBox preparationArea;
  public ImageView leagueImageView;
  public Label matchmakerHeadingLabel;
  public Label partyHeadingLabel;
  public ToggleButton searchButton;
  public ScrollPane scrollPane;
  public HBox playerCard;
  public Label crownLabel;
  public TabPane chatTabPane;
  public GridPane contentPane;
  public ColumnConstraints column2;
  public RowConstraints row2;

  private Map<Faction, ToggleButton> factionsToButtons;

  private final InvalidationListener partyMembersListener = observable -> {
    refreshingLabel.setVisible(false);
    renderPartyMembers();
  };
  private final ListChangeListener<MatchmakerQueueInfo> queueChangeListener = change -> {
    boolean shouldReRender = false;
    while (change.next()) {
      shouldReRender |= change.wasAdded() || change.wasRemoved();
    }

    if (shouldReRender) {
      renderQueues();
    }
  };

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(clanLabel, avatarImageView, leagueImageView);
    JavaFxUtil.fixScrollSpeed(scrollPane);

    leagueLabel.setText(i18n.get("teammatchmaking.inPlacement").toUpperCase());
    leagueImageView.setVisible(false);

    factionsToButtons = Map.of(Faction.UEF, uefButton, Faction.AEON, aeonButton, Faction.CYBRAN, cybranButton,
                               Faction.SERAPHIM, seraphimButton);

    searchButton.selectedProperty().bindBidirectional(teamMatchmakingService.searchingProperty());

    ReadOnlyObjectProperty<PlayerInfo> currentPlayerProperty = playerService.currentPlayerProperty();
    searchButton.disableProperty()
                .bind(teamMatchmakingService.getParty()
                                            .ownerProperty()
                                            .isNotEqualTo(currentPlayerProperty)
                                            .or(teamMatchmakingService.partyMembersNotReadyProperty())
                                            .or(teamMatchmakingService.anyQueueSelectedProperty()
                                                                      .not()
                                                                      .and(searchButton.selectedProperty().not()))
                                            .when(showing));

    initializeDynamicChatPosition();
    initializeUppercaseText();

    BooleanExpression notPartyOwner = BooleanBinding.booleanExpression(
        teamMatchmakingService.getParty().ownerProperty().isNotEqualTo(playerService.currentPlayerProperty()));

    leavePartyButton.disableProperty().bind(notPartyOwner.not().when(showing));
    invitePlayerButton.disableProperty().bind(notPartyOwner.when(showing));

    countryImageView.imageProperty().bind(currentPlayerProperty.flatMap(PlayerInfo::countryProperty)
                                               .map(country -> countryFlagService.loadCountryFlag(country).orElse(null))
                                               .when(showing));

    avatarImageView.imageProperty().bind(currentPlayerProperty.flatMap(PlayerInfo::avatarProperty)
                                              .map(avatarService::loadAvatar)
                                              .when(showing));

    ObservableValue<String> clanTagProperty = currentPlayerProperty.flatMap(PlayerInfo::clanProperty);
    clanLabel.visibleProperty().bind(clanTagProperty.map(clanTag -> !clanTag.isBlank()).orElse(false).when(showing));
    clanLabel.textProperty().bind(clanTagProperty.map("[%s]"::formatted).orElse("").when(showing));
    gameCountLabel.textProperty().bind(currentPlayerProperty.flatMap(PlayerInfo::numberOfGamesProperty)
                                             .map(numGames -> i18n.get("teammatchmaking.gameCount", numGames))
                                             .map(String::toUpperCase)
                                             .when(showing));
    usernameLabel.textProperty().bind(currentPlayerProperty.flatMap(PlayerInfo::usernameProperty).when(showing));
    crownLabel.visibleProperty()
              .bind(Bindings.size(teamMatchmakingService.getParty().getMembers())
                            .greaterThan(1)
                            .and(notPartyOwner.not())
                            .when(showing));

    teamMatchmakingService.inQueueProperty().when(showing).subscribe(this::setSearchButtonText);
    teamMatchmakingService.getParty().ownerProperty().when(showing).subscribe(this::setSearchButtonText);
    teamMatchmakingService.partyMembersNotReadyProperty().when(showing).subscribe(this::setSearchButtonText);
    teamMatchmakingService.anyQueueSelectedProperty().when(showing).subscribe(this::setSearchButtonText);
    currentPlayerProperty.when(showing).subscribe(this::setSearchButtonText);
    currentPlayerProperty.when(showing).subscribe(this::setLeagueInfo);

    selectFactions(matchmakerPrefs.getFactions());
    teamMatchmakingService.requestMatchmakerInfo();
  }

  @Override
  public void onShow() {
    JavaFxUtil.addAndTriggerListener(teamMatchmakingService.getParty().getMembers(), partyMembersListener);
    JavaFxUtil.addListener(teamMatchmakingService.getQueues(), queueChangeListener);
    renderQueues();
  }

  @Override
  public void onHide() {
    JavaFxUtil.removeListener(teamMatchmakingService.getQueues(), queueChangeListener);
    JavaFxUtil.removeListener(teamMatchmakingService.getParty().getMembers(), partyMembersListener);
  }

  private void initializeDynamicChatPosition() {
    contentPane.widthProperty().addListener((observable, oldValue, newValue) -> {
      if ((double) newValue < 1115.0) {
        GridPane.setColumnIndex(chatTabPane, 0);
        GridPane.setRowIndex(chatTabPane, 1);
        GridPane.setColumnSpan(chatTabPane, 2);
        GridPane.setColumnSpan(scrollPane, 2);
        column2.setMinWidth(0);
        row2.setMinHeight(200);
        chatTabPane.pseudoClassStateChanged(CHAT_AT_BOTTOM_PSEUDO_CLASS, true);
      } else {
        GridPane.setColumnIndex(chatTabPane, 1);
        GridPane.setRowIndex(chatTabPane, 0);
        GridPane.setColumnSpan(chatTabPane, 1);
        GridPane.setColumnSpan(scrollPane, 1);
        column2.setMinWidth(400);
        row2.setMinHeight(0);
        chatTabPane.pseudoClassStateChanged(CHAT_AT_BOTTOM_PSEUDO_CLASS, false);
      }
    });
  }

  private void initializeUppercaseText() {
    matchmakerHeadingLabel.setText(i18n.get("teammatchmaking.playerTitle").toUpperCase());
    partyHeadingLabel.setText(i18n.get("teammatchmaking.partyTitle").toUpperCase());
    invitePlayerButton.setText(i18n.get("teammatchmaking.invitePlayer").toUpperCase());
    leavePartyButton.setText(i18n.get("teammatchmaking.leaveParty").toUpperCase());
  }

  private void setSearchButtonText() {
    PlayerInfo currentPlayer = playerService.getCurrentPlayer();

    String buttonText;
    if (teamMatchmakingService.isInQueue()) {
      buttonText = i18n.get("teammatchmaking.searchButton.inQueue").toUpperCase();
    } else if (!Objects.equals(teamMatchmakingService.getParty().getOwner(), currentPlayer)) {
      buttonText = i18n.get("teammatchmaking.searchButton.inParty").toUpperCase();
    } else if (currentPlayer.getGameStatus() != PlayerGameStatus.IDLE) {
      buttonText = i18n.get("teammatchmaking.searchButton.inGame").toUpperCase();
    } else if (teamMatchmakingService.partyMembersNotReady()) {
      buttonText = i18n.get("teammatchmaking.searchButton.memberInGame").toUpperCase();
    } else if (!teamMatchmakingService.isAnyQueueSelected()) {
      buttonText = i18n.get("teammatchmaking.searchButton.noQueueSelected").toUpperCase();
    } else {
      buttonText = i18n.get("teammatchmaking.searchButton").toUpperCase();
    }
    fxApplicationThreadExecutor.execute(() -> searchButton.setText(buttonText));
  }

  private void setLeagueInfo(PlayerInfo currentPlayer) {
    if (currentPlayer == null) {
      return;
    }

    leaderboardService.getHighestActiveLeagueEntryForPlayer(currentPlayer).mapNotNull(LeagueEntry::subdivision)
                      .publishOn(fxApplicationThreadExecutor.asScheduler())
                      .subscribe(subdivision -> {
                        String divisionNameKey = subdivision.division().nameKey();
                        leagueLabel.setText(i18n.get("leaderboard.divisionName", i18n.getOrDefault(divisionNameKey,
                                                                                                   "leagues.divisionName.%s".formatted(
                                                                                                       divisionNameKey)),
                                                     subdivision.nameKey()).toUpperCase());
                        leagueImageView.setImage(leaderboardService.loadDivisionImage(subdivision.mediumImageUrl()));
                        leagueImageView.setVisible(true);
                      });
  }

  private void renderPartyMembers() {
    PlayerInfo currentPlayer = playerService.getCurrentPlayer();
    if (currentPlayer != null) {
      List<PartyMember> members = new ArrayList<>(teamMatchmakingService.getParty().getMembers());
      members.removeIf(partyMember -> currentPlayer.equals(partyMember.getPlayer()));
      List<Node> memberCards = members.stream().map(member -> {
        PartyMemberItemController controller = uiService.loadFxml(
            "theme/play/teammatchmaking/matchmaking_member_card.fxml");
        controller.setMember(member);
        return controller.getRoot();
      }).toList();
      fxApplicationThreadExecutor.execute(() -> {
        playerCard.pseudoClassStateChanged(LEADER_PSEUDO_CLASS, (teamMatchmakingService.getParty()
                                                                                       .getOwner()
                                                                                       .equals(
                                                                                           currentPlayer) && teamMatchmakingService.getParty()
                                                                                                                                   .getMembers()
                                                                                                                                   .size() > 1));
        partyMemberPane.getChildren().clear();
        int numMemberCards = memberCards.size();
        for (int i = 0; i < numMemberCards; i++) {
          if (numMemberCards == 1) {
            partyMemberPane.add(memberCards.get(i), 0, 0, 2, 1);
          } else {
            partyMemberPane.add(memberCards.get(i), i % 2, i / 2);
          }
        }
      });
    }
  }

  @Override
  public Node getRoot() {
    return teamMatchmakingRoot;
  }

  public void onInvitePlayerButtonClicked() {
    InvitePlayerController invitePlayerController = uiService.loadFxml(
        "theme/play/teammatchmaking/matchmaking_invite_player.fxml");
    Pane root = invitePlayerController.getRoot();
    uiService.showInDialog(teamMatchmakingRoot, root, i18n.get("teammatchmaking.invitePlayer"));
  }

  public void onLeavePartyButtonClicked() {
    teamMatchmakingService.leaveParty();
  }

  public void onFactionButtonClicked() {
    List<Faction> factions = factionsToButtons.entrySet()
                                              .stream()
                                              .filter(entry -> entry.getValue().isSelected())
                                              .map(Map.Entry::getKey)
                                              .sorted(Comparator.naturalOrder())
                                              .collect(Collectors.toList());

    if (factions.isEmpty()) {
      selectFactions(matchmakerPrefs.getFactions());
      return;
    }
    matchmakerPrefs.getFactions().setAll(factions);

    refreshingLabel.setVisible(true);
  }

  private void selectFactions(List<Faction> factions) {
    factionsToButtons.forEach((faction, toggleButton) -> toggleButton.setSelected(factions.contains(faction)));
  }

  protected void onMapPoolClickedListener(MatchmakerQueueInfo queue) {
    TeamMatchmakingMapListController controller = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_maplist_popup.fxml");
    controller.init(queue);
    Pane root = controller.getRoot();
    uiService.showInDialog(teamMatchmakingRoot, root, i18n.get("teammatchmaking.mapPool"));
  }

  private void renderQueues() {
    List<MatchmakerQueueInfo> queues = new ArrayList<>(teamMatchmakingService.getQueues());
    queues.sort(Comparator.comparing(MatchmakerQueueInfo::getTeamSize).thenComparing(MatchmakerQueueInfo::getId));
    int queuesPerRow = Math.min(queues.size(), 4);
    ObservableValue<Number> prefWidth = queuePane.widthProperty()
                                                 .divide(queuesPerRow)
                                                 .subtract(queuePane.getHgap())
                                                 .when(showing);
    List<VBox> queueCards = queues.stream().map(queue -> {
      MatchmakingQueueItemController controller = uiService.loadFxml(
          "theme/play/teammatchmaking/matchmaking_queue_card.fxml");
      controller.setQueue(queue);
      controller.setOnMapPoolClickedListener(this::onMapPoolClickedListener);
      controller.getRoot().prefWidthProperty().bind(prefWidth);
      return controller.getRoot();
    }).collect(Collectors.toList());
    fxApplicationThreadExecutor.execute(() -> queuePane.getChildren().setAll(queueCards));
  }

  public void onSearchButtonClicked() {
    if (searchButton.isSelected()) {
      teamMatchmakingService.joinQueues().thenAcceptAsync(searchButton::setSelected, fxApplicationThreadExecutor);
    } else {
      teamMatchmakingService.leaveQueues();
    }
  }
}
