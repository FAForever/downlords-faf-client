package com.faforever.client.teammatchmaking;

import com.faforever.client.chat.ChatMessage;
import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.MatchmakingChatController;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.Faction;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.FafService;
import com.faforever.client.teammatchmaking.Party.PartyMember;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.dialog.Dialog;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static javafx.beans.binding.Bindings.createBooleanBinding;
import static javafx.beans.binding.Bindings.createObjectBinding;
import static javafx.beans.binding.Bindings.createStringBinding;

@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class TeamMatchmakingController extends AbstractViewController<Node> {

  private static final PseudoClass LEADER_PSEUDO_CLASS = PseudoClass.getPseudoClass("leader");
  private static final PseudoClass CHAT_AT_BOTTOM_PSEUDO_CLASS = PseudoClass.getPseudoClass("bottom");

  private final CountryFlagService countryFlagService;
  private final AvatarService avatarService;
  private final PlayerService playerService;
  private final I18n i18n;
  private final UiService uiService;
  private final TeamMatchmakingService teamMatchmakingService;
  private final FafService fafService;
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
  public HBox queueBox;
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
  private Player player;
  private MatchmakingChatController matchmakingChatController;

  @Override
  public void initialize() {
    eventBus.register(this);
    JavaFxUtil.fixScrollSpeed(scrollPane);
    initializeDynamicChatPosition();

    player = playerService.getCurrentPlayer().get();
    initializeUppercaseText();
    countryImageView.imageProperty().bind(createObjectBinding(() -> countryFlagService.loadCountryFlag(
        StringUtils.isEmpty(player.getCountry()) ? "" : player.getCountry()).orElse(null), player.countryProperty()));
    avatarImageView.visibleProperty().bind(player.avatarUrlProperty().isNotNull().and(player.avatarUrlProperty().isNotEmpty()));
    avatarImageView.imageProperty().bind(createObjectBinding(() -> Strings.isNullOrEmpty(player.getAvatarUrl()) ? null : avatarService.loadAvatar(player.getAvatarUrl()), player.avatarUrlProperty()));
    leagueImageView.setImage(avatarService.loadAvatar("https://content.faforever.com/divisions/icons/Medium/Diamond-2_medium.png"));
    clanLabel.managedProperty().bind(clanLabel.visibleProperty());
    clanLabel.visibleProperty().bind(player.clanProperty().isNotEmpty().and(player.clanProperty().isNotNull()));
    clanLabel.textProperty().bind(createStringBinding(() ->
        Strings.isNullOrEmpty(player.getClan()) ? "" : String.format("[%s]", player.getClan()), player.clanProperty()));
    usernameLabel.textProperty().bind(player.usernameProperty());
    crownLabel.visibleProperty().bind(createBooleanBinding(() ->
        teamMatchmakingService.getParty().getMembers().size() > 1 && teamMatchmakingService.getParty().getOwner().equals(player),
        teamMatchmakingService.getParty().ownerProperty(), teamMatchmakingService.getParty().getMembers()));

    teamMatchmakingService.getParty().getMembers().addListener((Observable o) -> {
      playerCard.pseudoClassStateChanged(LEADER_PSEUDO_CLASS,
          (teamMatchmakingService.getParty().getOwner().equals(player) && teamMatchmakingService.getParty().getMembers().size() > 1));
      List<PartyMember> members = new ArrayList<>(teamMatchmakingService.getParty().getMembers());
      partyMemberPane.getChildren().clear();
      members.removeIf(partyMember -> partyMember.getPlayer().equals(player));
      for(int i = 0; i < members.size(); i++) {
        PartyMemberItemController controller = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_member_card.fxml");
        controller.setMember(members.get(i));
        if (members.size() == 1)
          partyMemberPane.add(controller.getRoot(), 0, 0, 2, 1);
        else
          partyMemberPane.add(controller.getRoot(), i % 2, i / 2);
      }
    });

    invitePlayerButton.disableProperty().bind(createBooleanBinding(
        () -> teamMatchmakingService.getParty().getOwner().getId() != playerService.getCurrentPlayer().map(Player::getId).orElse(-1),
        teamMatchmakingService.getParty().ownerProperty(),
        playerService.currentPlayerProperty()
    ));
    leavePartyButton.disableProperty().bind(createBooleanBinding(() -> teamMatchmakingService.getParty().getMembers().size() <= 1, teamMatchmakingService.getParty().getMembers()));

    teamMatchmakingService.getParty().getMembers().addListener((InvalidationListener) c -> {
      refreshingLabel.setVisible(false);
      selectFactionsBasedOnParty();
    });

    JavaFxUtil.addListener(teamMatchmakingService.getParty().ownerProperty(), (observable, oldValue, newValue) -> {
      if (matchmakingChatController != null) {
        matchmakingChatController.closeChannel();
      }
      createChannelTab(String.format("#%s'sParty", newValue.getUsername()));
    });
    createChannelTab(String.format("#%s'sParty", teamMatchmakingService.getParty().getOwner().getUsername()));

    fafService.requestMatchmakerInfo();
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

    leagueLabel.textProperty().bind(createStringBinding(() -> i18n.get("leaderboard.divisionName").toUpperCase(),
        player.globalRatingMeanProperty())); // This should actually be a divisionProperty once that is available
    gameCountLabel.textProperty().bind(createStringBinding(() ->
        i18n.get("teammatchmaking.gameCount", player.getNumberOfGames()).toUpperCase(), player.numberOfGamesProperty()));
    queueHeadingLabel.textProperty().bind(createStringBinding(() -> {
      if (teamMatchmakingService.getParty().getOwner().equals(player))
        return i18n.get("teammatchmaking.queueTitle").toUpperCase();
      else
        return i18n.get("teammatchmaking.queueTitle.inParty").toUpperCase();
    }, teamMatchmakingService.getParty().ownerProperty()));
  }

  @Override
  public Node getRoot() {
    return teamMatchmakingRoot;
  }

  public void onInvitePlayerButtonClicked(ActionEvent actionEvent) {
    InvitePlayerController invitePlayerController = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_invite_player.fxml");
    Pane root = invitePlayerController.getRoot();
    Dialog dialog = uiService.showInDialog(teamMatchmakingRoot, root, i18n.get("teammatchmaking.invitePlayer"));
  }

  public void onLeavePartyButtonClicked(ActionEvent actionEvent) {
    teamMatchmakingService.leaveParty();
  }

  public void onFactionButtonClicked(ActionEvent actionEvent) {

    if (!uefButton.isSelected() && !aeonButton.isSelected() && !cybranButton.isSelected() && !seraphimButton.isSelected()) {
      selectFactionsBasedOnParty();
      return;
    }

    List<Faction> factions = new ArrayList<>();
    if (uefButton.isSelected()) {
      factions.add(Faction.UEF);
    }
    if (aeonButton.isSelected()) {
      factions.add(Faction.AEON);
    }
    if (cybranButton.isSelected()) {
      factions.add(Faction.CYBRAN);
    }
    if (seraphimButton.isSelected()) {
      factions.add(Faction.SERAPHIM);
    }

    teamMatchmakingService.setPartyFactions(factions);

    refreshingLabel.setVisible(true);
  }

  private void selectFactionsBasedOnParty() {
    uefButton.setSelected(isFactionSelectedInParty(Faction.UEF));
    aeonButton.setSelected(isFactionSelectedInParty(Faction.AEON));
    cybranButton.setSelected(isFactionSelectedInParty(Faction.CYBRAN));
    seraphimButton.setSelected(isFactionSelectedInParty(Faction.SERAPHIM));
  }

  private boolean isFactionSelectedInParty(Faction faction) {
    return teamMatchmakingService.getParty().getMembers().stream()
        .anyMatch(m -> m.getPlayer().getId() == player.getId() && m.getFactions().contains(faction));
  }

  private void createChannelTab(String channelName) {
    JavaFxUtil.assertApplicationThread();
    matchmakingChatController = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_chat.fxml");
    matchmakingChatController.setChannel(channelName);
    chatTabPane.getTabs().clear();
    chatTabPane.getTabs().add(matchmakingChatController.getRoot());
  }

  @Subscribe
  public void onChatMessage(ChatMessageEvent event) {
    Platform.runLater(() -> {
      ChatMessage message = event.getMessage();
      if (message.getSource().equals(String.format("#%s'sParty", teamMatchmakingService.getParty().getOwner().getUsername()))) {
        matchmakingChatController.onChatMessage(message);
      }
    });
  }

  @Subscribe
  public void onQueuesAdded(QueuesAddedEvent event) {
    Platform.runLater(() -> {
      List<MatchmakingQueue> queues = teamMatchmakingService.getMatchmakingQueues();
      queueBox.getChildren().clear();
      queues.forEach(queue -> {
        MatchmakingQueueItemController controller = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_queue_card.fxml");
        controller.setQueue(queue);
        queueBox.getChildren().add(controller.getRoot());
      });
    });
  }
}
