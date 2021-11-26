package com.faforever.client.teammatchmaking;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.chat.ChatMessage;
import com.faforever.client.chat.MatchmakingChatController;
import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.PartyBean.PartyMember;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.Faction;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.faforever.client.chat.ChatService.PARTY_CHANNEL_SUFFIX;

@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class TeamMatchmakingController extends AbstractViewController<Node> {

  public static final PseudoClass LEADER_PSEUDO_CLASS = PseudoClass.getPseudoClass("leader");
  public static final PseudoClass CHAT_AT_BOTTOM_PSEUDO_CLASS = PseudoClass.getPseudoClass("bottom");

  private final AssetService assetService;
  private final CountryFlagService countryFlagService;
  private final AvatarService avatarService;
  private final LeaderboardService leaderboardService;
  private final PreferencesService preferencesService;
  private final PlayerService playerService;
  private final I18n i18n;
  private final UiService uiService;
  private final TeamMatchmakingService teamMatchmakingService;
  private final EventBus eventBus;

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
  public Label queueHeadingLabel;
  public ScrollPane scrollPane;
  public HBox playerCard;
  public Label crownLabel;
  public TabPane chatTabPane;
  public GridPane contentPane;
  public ColumnConstraints column2;
  public RowConstraints row2;
  private PlayerBean player;
  private Map<Faction, ToggleButton> factionsToButtons;
  @VisibleForTesting
  protected MatchmakingChatController matchmakingChatController;
  private InvalidationListener matchmakingQueuesLabelInvalidationListener;
  private InvalidationListener playerPropertiesInvalidationListener;
  private ChangeListener<PlayerBean> partyOwnerChangeListener;

  @Override
  public void initialize() {
    JavaFxUtil.bindManagedToVisible(clanLabel, avatarImageView, leagueImageView);
    JavaFxUtil.fixScrollSpeed(scrollPane);
    eventBus.register(this);

    factionsToButtons = Map.of(Faction.UEF, uefButton, Faction.AEON, aeonButton,
        Faction.CYBRAN, cybranButton, Faction.SERAPHIM, seraphimButton);

    player = playerService.getCurrentPlayer();
    initializeDynamicChatPosition();
    initializeUppercaseText();
    initializeListeners();

    ObservableList<Faction> factions = preferencesService.getPreferences().getMatchmaker().getFactions();
    selectFactions(factions);
    teamMatchmakingService.sendFactionSelection(factions);
    teamMatchmakingService.requestMatchmakerInfo();
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

  private void setQueueHeadingLabel() {
    String labelText;
    if (teamMatchmakingService.isCurrentlyInQueue()) {
      labelText = i18n.get("teammatchmaking.queueTitle.inQueue").toUpperCase();
    } else if (!teamMatchmakingService.getParty().getOwner().equals(player)) {
      labelText = i18n.get("teammatchmaking.queueTitle.inParty").toUpperCase();
    } else if (player.getStatus() != PlayerStatus.IDLE) {
      labelText = i18n.get("teammatchmaking.queueTitle.inGame").toUpperCase();
    } else if (teamMatchmakingService.partyMembersNotReady()) {
      labelText = i18n.get("teammatchmaking.queueTitle.memberInGame").toUpperCase();
    } else {
      labelText = i18n.get("teammatchmaking.queueTitle").toUpperCase();
    }
    JavaFxUtil.runLater(() -> queueHeadingLabel.setText(labelText));
  }

  private void setLeagueInfo() {
    leaderboardService.getHighestLeagueEntryForPlayer(player).thenAccept(leagueEntry -> JavaFxUtil.runLater(() -> {
      if (leagueEntry.isEmpty() || leagueEntry.get().getSubdivision() == null) {
        leagueLabel.setText(i18n.get("teammatchmaking.inPlacement").toUpperCase());
        leagueImageView.setVisible(false);
      } else {
        SubdivisionBean subdivision = leagueEntry.get().getSubdivision();
        leagueLabel.setText(i18n.get("leaderboard.divisionName",
            i18n.getOrDefault(subdivision.getDivision().getNameKey(), subdivision.getDivisionI18nKey()),
            subdivision.getNameKey()).toUpperCase());
        leagueImageView.setImage(assetService.loadAndCacheImage(
            subdivision.getMediumImageUrl(), Paths.get("divisions"), null
        ));
        leagueImageView.setVisible(true);
      }
    }));
  }

  private void initializeListeners() {
    matchmakingQueuesLabelInvalidationListener = observable -> setQueueHeadingLabel();

    playerPropertiesInvalidationListener = observable -> {
      Image countryFlag = countryFlagService.loadCountryFlag(player.getCountry()).orElse(null);
      Image avatarImage = player.getAvatar() == null ? null : avatarService.loadAvatar(player.getAvatar());
      String clanTag = Strings.isNullOrEmpty(player.getClan()) ? "" : String.format("[%s]", player.getClan());
      setLeagueInfo();
      JavaFxUtil.runLater(() -> {
        countryImageView.setImage(countryFlag);
        avatarImageView.setImage(avatarImage);
        clanLabel.setVisible(!Strings.isNullOrEmpty(player.getClan()));
        clanLabel.setText(clanTag);
        gameCountLabel.setText(i18n.get("teammatchmaking.gameCount", player.getNumberOfGames()).toUpperCase());
        usernameLabel.setText(player.getUsername());
      });
    };

    partyOwnerChangeListener = (observable, oldValue, newValue) -> JavaFxUtil.runLater(() -> {
      leavePartyButton.setDisable(newValue == player);
      invitePlayerButton.setDisable(newValue != player);
      setCrownVisibility();
    });

    addListeners();
  }

  private void addListeners() {
    JavaFxUtil.addAndTriggerListener(player.clanProperty(), new WeakInvalidationListener(playerPropertiesInvalidationListener));
    JavaFxUtil.addListener(player.avatarProperty(), new WeakInvalidationListener(playerPropertiesInvalidationListener));
    JavaFxUtil.addListener(player.countryProperty(), new WeakInvalidationListener(playerPropertiesInvalidationListener));
    JavaFxUtil.addListener(player.getLeaderboardRatings(), new WeakInvalidationListener(playerPropertiesInvalidationListener));
    JavaFxUtil.addListener(player.usernameProperty(), new WeakInvalidationListener(playerPropertiesInvalidationListener));
    JavaFxUtil.addAndTriggerListener(teamMatchmakingService.getParty().ownerProperty(), new WeakChangeListener<>(partyOwnerChangeListener));
    JavaFxUtil.addListener(teamMatchmakingService.getParty().getMembers(), (InvalidationListener) observable -> setCrownVisibility());
    JavaFxUtil.addAndTriggerListener(teamMatchmakingService.getParty().getMembers(), (InvalidationListener) observable -> renderPartyMembers());
    JavaFxUtil.addAndTriggerListener(teamMatchmakingService.getMatchmakerQueues(), (InvalidationListener) observable -> renderQueues());
    JavaFxUtil.addListener(teamMatchmakingService.currentlyInQueueProperty(), new WeakInvalidationListener(matchmakingQueuesLabelInvalidationListener));
    JavaFxUtil.addListener(teamMatchmakingService.getParty().ownerProperty(), new WeakInvalidationListener(matchmakingQueuesLabelInvalidationListener));
    JavaFxUtil.addListener(teamMatchmakingService.partyMembersNotReadyProperty(), new WeakInvalidationListener(matchmakingQueuesLabelInvalidationListener));
    JavaFxUtil.addAndTriggerListener(player.statusProperty(), new WeakInvalidationListener(matchmakingQueuesLabelInvalidationListener));
    JavaFxUtil.addAndTriggerListener(teamMatchmakingService.getParty().getMembers(), (InvalidationListener) observable -> {
      refreshingLabel.setVisible(false);
      selectFactionsBasedOnParty();
    });

    JavaFxUtil.addAndTriggerListener(teamMatchmakingService.getParty().ownerProperty(), (observable, oldValue, newValue) -> {
      if (matchmakingChatController != null) {
        matchmakingChatController.closeChannel();
      }
      createChannelTab("#" + newValue.getUsername() + PARTY_CHANNEL_SUFFIX);
    });
  }

  private void setCrownVisibility() {
    crownLabel.setVisible(teamMatchmakingService.getParty().getMembers().size() > 1 && teamMatchmakingService.getParty().getOwner().equals(player));
  }

  private synchronized void renderPartyMembers() {
    if (player != null) {
      List<PartyMember> members = new ArrayList<>(teamMatchmakingService.getParty().getMembers());
      members.removeIf(partyMember -> player.equals(partyMember.getPlayer()));
      List<Node> memberCards = members.stream().map(member -> {
        PartyMemberItemController controller = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_member_card.fxml");
        controller.setMember(member);
        return controller.getRoot();
      }).collect(Collectors.toList());
      JavaFxUtil.runLater(() -> {
        playerCard.pseudoClassStateChanged(LEADER_PSEUDO_CLASS,
            (teamMatchmakingService.getParty().getOwner().equals(player) && teamMatchmakingService.getParty().getMembers().size() > 1));
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
    List<Faction> factions = factionsToButtons.entrySet().stream()
        .filter(entry -> entry.getValue().isSelected())
        .map(Map.Entry::getKey)
        .sorted(Comparator.naturalOrder())
        .collect(Collectors.toList());
    if (factions.isEmpty()) {
      selectFactionsBasedOnParty();
      return;
    }
    preferencesService.getPreferences().getMatchmaker().getFactions().setAll(factions);
    preferencesService.storeInBackground();

    teamMatchmakingService.sendFactionSelection(factions);
    refreshingLabel.setVisible(true);
  }

  private void selectFactionsBasedOnParty() {
    List<Faction> factions = teamMatchmakingService.getParty().getMembers().stream()
        .filter(member -> member.getPlayer().getId() == player.getId())
        .findFirst()
        .map(PartyMember::getFactions)
        .orElse(List.of());
    selectFactions(factions);
  }

  private void selectFactions(List<Faction> factions) {
    factionsToButtons.forEach((faction, toggleButton) ->
        toggleButton.setSelected(factions.contains(faction)));
  }

  private void createChannelTab(String channelName) {
    JavaFxUtil.runLater(() -> {
      matchmakingChatController = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_chat.fxml");
      matchmakingChatController.setChannel(channelName);
      chatTabPane.getTabs().clear();
      chatTabPane.getTabs().add(matchmakingChatController.getRoot());
    });
  }

  @Subscribe
  public void onChatMessage(ChatMessageEvent event) {
    ChatMessage message = event.getMessage();
    if (matchmakingChatController != null && message.getSource().equals(matchmakingChatController.getReceiver())) {
      JavaFxUtil.runLater(() -> matchmakingChatController.onChatMessage(message));
    }
  }

  private synchronized void renderQueues() {
    List<MatchmakerQueueBean> queues = new ArrayList<>(teamMatchmakingService.getMatchmakerQueues());
    queues.sort(Comparator.comparing(MatchmakerQueueBean::getId));
    int queuesPerRow = Math.min(queues.size(), 4);
    List<VBox> queueCards = queues.stream().map(queue -> {
      MatchmakingQueueItemController controller = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_queue_card.fxml");
      controller.setQueue(queue);
      controller.getRoot().prefWidthProperty().bind(Bindings.createDoubleBinding(() -> queuePane.getWidth() / queuesPerRow - queuePane.getHgap(), queuePane.widthProperty()));
      return controller.getRoot();
    }).collect(Collectors.toList());
    JavaFxUtil.runLater(() -> queuePane.getChildren().setAll(queueCards));
  }
}
