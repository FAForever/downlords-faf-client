package com.faforever.client.teammatchmaking;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.chat.ChatChannel;
import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.MatchmakingChatController;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.PartyBean.PartyMember;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.fx.SimpleInvalidationListener;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.MatchmakerPrefs;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.Faction;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.faforever.client.chat.ChatService.PARTY_CHANNEL_SUFFIX;

@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class TeamMatchmakingController extends AbstractViewController<Node> {

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
  private final ChatService chatService;

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
  @VisibleForTesting
  protected MatchmakingChatController matchmakingChatController;
  private final SimpleInvalidationListener searchButtonInvalidationListener = this::setSearchButtonText;
  private final SimpleChangeListener<PlayerBean> ownerChangeListener = newValue -> {
    if (matchmakingChatController != null) {
      matchmakingChatController.closeChannel();
    }
    createChannelTab("#" + newValue.getUsername() + PARTY_CHANNEL_SUFFIX);
  };
  private final SimpleInvalidationListener currentPlayerListener = this::setLeagueInfo;

  @Override
  public void initialize() {
    JavaFxUtil.bindManagedToVisible(clanLabel, avatarImageView, leagueImageView);
    JavaFxUtil.fixScrollSpeed(scrollPane);

    factionsToButtons = Map.of(Faction.UEF, uefButton, Faction.AEON, aeonButton, Faction.CYBRAN, cybranButton, Faction.SERAPHIM, seraphimButton);

    ObservableValue<Boolean> showing = uiService.createShowingProperty(getRoot());

    searchButton.selectedProperty().bindBidirectional(teamMatchmakingService.searchingProperty());

    ReadOnlyObjectProperty<PlayerBean> currentPlayerProperty = playerService.currentPlayerProperty();
    searchButton.disableProperty()
        .bind(teamMatchmakingService.getParty().ownerProperty().isNotEqualTo(currentPlayerProperty)
            .or(teamMatchmakingService.partyMembersNotReadyProperty())
            .or(teamMatchmakingService.anyQueueSelectedProperty().not().and(searchButton.selectedProperty().not()))
            .when(showing));

    initializeDynamicChatPosition();
    initializeUppercaseText();

    BooleanExpression notPartyOwner = BooleanBinding.booleanExpression(teamMatchmakingService.getParty()
        .ownerProperty()
        .isNotEqualTo(playerService.currentPlayerProperty()));

    leavePartyButton.disableProperty().bind(notPartyOwner.not().when(showing));
    invitePlayerButton.disableProperty().bind(notPartyOwner.when(showing));

    countryImageView.imageProperty()
        .bind(currentPlayerProperty.flatMap(PlayerBean::countryProperty)
            .map(country -> countryFlagService.loadCountryFlag(country).orElse(null))
            .when(showing));

    avatarImageView.imageProperty()
        .bind(currentPlayerProperty.flatMap(PlayerBean::avatarProperty).map(avatarService::loadAvatar));

    ObservableValue<String> clanTagProperty = currentPlayerProperty.flatMap(PlayerBean::clanProperty);
    clanLabel.visibleProperty().bind(clanTagProperty.map(clanTag -> !clanTag.isBlank()).orElse(false).when(showing));
    clanLabel.textProperty().bind(clanTagProperty.map("[%s]"::formatted).orElse("").when(showing));
    gameCountLabel.textProperty()
        .bind(currentPlayerProperty.flatMap(PlayerBean::numberOfGamesProperty)
            .map(numGames -> i18n.get("teammatchmaking.gameCount", numGames))
            .map(String::toUpperCase)
            .when(showing));
    usernameLabel.textProperty().bind(currentPlayerProperty.flatMap(PlayerBean::usernameProperty).when(showing));
    crownLabel.visibleProperty().bind(Bindings.size(teamMatchmakingService.getParty().getMembers()).greaterThan(1)
        .and(notPartyOwner.not())
        .when(showing));

    teamMatchmakingService.inQueueProperty()
        .addListener(new WeakInvalidationListener(searchButtonInvalidationListener));
    teamMatchmakingService.getParty()
        .ownerProperty()
        .addListener(new WeakInvalidationListener(searchButtonInvalidationListener));
    teamMatchmakingService.partyMembersNotReadyProperty()
        .addListener(new WeakInvalidationListener(searchButtonInvalidationListener));
    teamMatchmakingService.anyQueueSelectedProperty()
        .addListener(new WeakInvalidationListener(searchButtonInvalidationListener));
    JavaFxUtil.addAndTriggerListener(currentPlayerProperty, new WeakInvalidationListener(searchButtonInvalidationListener));

    JavaFxUtil.addAndTriggerListener(teamMatchmakingService.getParty()
        .ownerProperty(), new WeakChangeListener<>(ownerChangeListener));

    JavaFxUtil.addAndTriggerListener(currentPlayerProperty, new WeakInvalidationListener(currentPlayerListener));
    JavaFxUtil.addAndTriggerListener(teamMatchmakingService.getParty()
        .getMembers(), (InvalidationListener) observable -> renderPartyMembers());
    JavaFxUtil.addListener(teamMatchmakingService.getQueues(), (ListChangeListener<MatchmakerQueueBean>) change -> {
      boolean shouldReRender = false;
      while (change.next()) {
        shouldReRender |= change.wasAdded() || change.wasRemoved();
      }

      if (shouldReRender) {
        renderQueues();
      }
    });
    JavaFxUtil.addAndTriggerListener(teamMatchmakingService.getParty()
        .getMembers(), (InvalidationListener) observable -> {
      refreshingLabel.setVisible(false);
      selectFactionsBasedOnParty();
    });


    ObservableList<Faction> factions = matchmakerPrefs.getFactions();
    selectFactions(factions);
    teamMatchmakingService.sendFactionSelection(factions);
    teamMatchmakingService.requestMatchmakerInfo();
    renderQueues();
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
    PlayerBean currentPlayer = playerService.getCurrentPlayer();

    String buttonText;
    if (teamMatchmakingService.isInQueue()) {
      buttonText = i18n.get("teammatchmaking.searchButton.inQueue").toUpperCase();
    } else if (!Objects.equals(teamMatchmakingService.getParty().getOwner(), currentPlayer)) {
      buttonText = i18n.get("teammatchmaking.searchButton.inParty").toUpperCase();
    } else if (currentPlayer.getStatus() != PlayerStatus.IDLE) {
      buttonText = i18n.get("teammatchmaking.searchButton.inGame").toUpperCase();
    } else if (teamMatchmakingService.partyMembersNotReady()) {
      buttonText = i18n.get("teammatchmaking.searchButton.memberInGame").toUpperCase();
    } else if (!teamMatchmakingService.isAnyQueueSelected()) {
      buttonText = i18n.get("teammatchmaking.searchButton.noQueueSelected").toUpperCase();
    } else {
      buttonText = i18n.get("teammatchmaking.searchButton").toUpperCase();
    }
    JavaFxUtil.runLater(() -> searchButton.setText(buttonText));
  }

  private void setLeagueInfo() {
    leaderboardService.getHighestActiveLeagueEntryForPlayer(playerService.getCurrentPlayer())
        .thenAccept(leagueEntry -> JavaFxUtil.runLater(() -> {
          if (leagueEntry.isEmpty() || leagueEntry.get().getSubdivision() == null) {
            leagueLabel.setText(i18n.get("teammatchmaking.inPlacement").toUpperCase());
            leagueImageView.setVisible(false);
          } else {
            SubdivisionBean subdivision = leagueEntry.get().getSubdivision();
            leagueLabel.setText(i18n.get("leaderboard.divisionName", i18n.getOrDefault(subdivision.getDivision()
                .getNameKey(), subdivision.getDivisionI18nKey()), subdivision.getNameKey()).toUpperCase());
            leagueImageView.setImage(leaderboardService.loadDivisionImage(subdivision.getMediumImageUrl()));
            leagueImageView.setVisible(true);
          }
        }));
  }

  private synchronized void renderPartyMembers() {
    PlayerBean currentPlayer = playerService.getCurrentPlayer();
    if (currentPlayer != null) {
      List<PartyMember> members = new ArrayList<>(teamMatchmakingService.getParty().getMembers());
      members.removeIf(partyMember -> currentPlayer.equals(partyMember.getPlayer()));
      List<Node> memberCards = members.stream().map(member -> {
        PartyMemberItemController controller = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_member_card.fxml");
        controller.setMember(member);
        return controller.getRoot();
      }).toList();
      JavaFxUtil.runLater(() -> {
        playerCard.pseudoClassStateChanged(LEADER_PSEUDO_CLASS, (teamMatchmakingService.getParty()
            .getOwner()
            .equals(currentPlayer) && teamMatchmakingService.getParty().getMembers().size() > 1));
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
    InvitePlayerController invitePlayerController = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_invite_player.fxml");
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
      selectFactionsBasedOnParty();
      return;
    }
    matchmakerPrefs.getFactions().setAll(factions);

    teamMatchmakingService.sendFactionSelection(factions);
    refreshingLabel.setVisible(true);
  }

  private void selectFactionsBasedOnParty() {
    List<Faction> factions = teamMatchmakingService.getParty()
        .getMembers()
        .stream()
        .filter(member -> Objects.equals(member.getPlayer().getId(), playerService.getCurrentPlayer().getId()))
        .findFirst()
        .map(PartyMember::getFactions)
        .orElse(List.of());
    selectFactions(factions);
  }

  private void selectFactions(List<Faction> factions) {
    factionsToButtons.forEach((faction, toggleButton) -> toggleButton.setSelected(factions.contains(faction)));
  }

  private void createChannelTab(String channelName) {
    chatService.joinChannel(channelName);
    ChatChannel chatChannel = chatService.getOrCreateChannel(channelName);
    matchmakingChatController = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_chat.fxml");
    matchmakingChatController.setChatChannel(chatChannel);
    JavaFxUtil.runLater(() -> {
      chatTabPane.getTabs().clear();
      chatTabPane.getTabs().add(matchmakingChatController.getRoot());
    });
  }

  private synchronized void renderQueues() {
    List<MatchmakerQueueBean> queues = new ArrayList<>(teamMatchmakingService.getQueues());
    queues.sort(Comparator.comparing(MatchmakerQueueBean::getTeamSize).thenComparing(MatchmakerQueueBean::getId));
    int queuesPerRow = Math.min(queues.size(), 4);
    List<VBox> queueCards = queues.stream().map(queue -> {
      MatchmakingQueueItemController controller = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_queue_card.fxml");
      controller.setQueue(queue);
      controller.getRoot()
          .prefWidthProperty()
          .bind(queuePane.widthProperty().divide(queuesPerRow).subtract(queuePane.getHgap()));
      return controller.getRoot();
    }).collect(Collectors.toList());
    JavaFxUtil.runLater(() -> queuePane.getChildren().setAll(queueCards));
  }

  public void onSearchButtonClicked() {
    if (searchButton.isSelected()) {
      teamMatchmakingService.joinQueues()
          .thenAccept(success -> JavaFxUtil.runLater(() -> searchButton.setSelected(success)));
    } else {
      teamMatchmakingService.leaveQueues();
    }
  }
}
