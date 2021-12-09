package com.faforever.client.teammatchmaking;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.PartyBean.PartyMember;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Assert;
import com.faforever.commons.lobby.Faction;
import com.google.common.base.Strings;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class PartyMemberItemController implements Controller<Node> {

  public static final PseudoClass LEADER_PSEUDO_CLASS = PseudoClass.getPseudoClass("leader");
  public static final PseudoClass PLAYING_PSEUDO_CLASS = PseudoClass.getPseudoClass("playing");

  private final CountryFlagService countryFlagService;
  private final AvatarService avatarService;
  private final LeaderboardService leaderboardService;
  private final PlayerService playerService;
  private final TeamMatchmakingService teamMatchmakingService;
  private final UiService uiService;
  private final I18n i18n;

  public Node playerItemRoot;
  public ImageView avatarImageView;
  public ImageView countryImageView;
  public ImageView leagueImageView;
  public Button kickPlayerButton;
  public Label clanLabel;
  public Label usernameLabel;
  public Label leagueLabel;
  public Label gameCountLabel;
  public Label uefLabel;
  public Label cybranLabel;
  public Label aeonLabel;
  public Label seraphimLabel;
  public Label crownLabel;
  public HBox playerCard;
  public ImageView playerStatusImageView;

  private PlayerBean player;
  private WeakReference<PartyMemberContextMenuController> contextMenuController = null;
  private InvalidationListener playerStatusInvalidationListener;
  private InvalidationListener playerPropertiesInvalidationListener;
  private InvalidationListener partyOwnerInvalidationListener;

  @Override
  public void initialize() {
    JavaFxUtil.bindManagedToVisible(clanLabel, avatarImageView, playerStatusImageView, leagueImageView, kickPlayerButton);
    initializeListeners();
  }

  @Override
  public Node getRoot() {
    return playerItemRoot;
  }

  public void setMember(PartyMember member) {
    Assert.checkNotNullIllegalState(player, "Party member already set");
    player = member.getPlayer();
    if (player == null) {
      log.info("Player of party member is null");
      return;
    }

    playerStatusImageView.setImage(uiService.getThemeImage(UiService.CHAT_LIST_STATUS_PLAYING));

    addListeners();
    selectFactionsBasedOnParty();
  }

  private void initializeListeners() {
    playerStatusInvalidationListener = observable -> setMemberGameStatus();
    playerPropertiesInvalidationListener = observable -> setPlayerProperties();
    partyOwnerInvalidationListener = observable -> setPartyOwnerProperties();
  }

  private void setMemberGameStatus() {
    boolean inGame = player.getStatus() != PlayerStatus.IDLE;
    JavaFxUtil.runLater(() -> {
      playerStatusImageView.setVisible(inGame);
      playerCard.pseudoClassStateChanged(PLAYING_PSEUDO_CLASS, inGame);
    });
  }

  private void setPartyOwnerProperties() {
    PlayerBean currentPlayer = playerService.getCurrentPlayer();
    PlayerBean owner = teamMatchmakingService.getParty().getOwner();
    JavaFxUtil.runLater(() -> {
      crownLabel.setVisible(owner == player);
      kickPlayerButton.setVisible(owner == currentPlayer && player != currentPlayer);
      playerCard.pseudoClassStateChanged(LEADER_PSEUDO_CLASS, owner == player);
    });
  }

  private void setPlayerProperties() {
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
  }

  @VisibleForTesting
  protected void setLeagueInfo() {
    leaderboardService.getHighestLeagueEntryForPlayer(player).thenAccept(leagueEntry -> JavaFxUtil.runLater(() -> {
      if (leagueEntry.isEmpty() || leagueEntry.get().getSubdivision() == null) {
        leagueLabel.setText(i18n.get("teammatchmaking.inPlacement").toUpperCase());
        leagueImageView.setVisible(false);
      } else {
        SubdivisionBean subdivision = leagueEntry.get().getSubdivision();
        leagueLabel.setText(i18n.get("leaderboard.divisionName",
            i18n.getOrDefault(subdivision.getDivision().getNameKey(), subdivision.getDivisionI18nKey()),
            subdivision.getNameKey()).toUpperCase());
        leagueImageView.setImage(leaderboardService.loadDivisionImage(subdivision.getMediumImageUrl()));
        leagueImageView.setVisible(true);
      }
    }));
  }

  private void addListeners() {
    JavaFxUtil.addAndTriggerListener(player.clanProperty(), new WeakInvalidationListener(playerPropertiesInvalidationListener));
    JavaFxUtil.addListener(player.avatarProperty(), new WeakInvalidationListener(playerPropertiesInvalidationListener));
    JavaFxUtil.addListener(player.countryProperty(), new WeakInvalidationListener(playerPropertiesInvalidationListener));
    JavaFxUtil.addListener(player.getLeaderboardRatings(), new WeakInvalidationListener(playerPropertiesInvalidationListener));
    JavaFxUtil.addListener(player.usernameProperty(), new WeakInvalidationListener(playerPropertiesInvalidationListener));
    JavaFxUtil.addAndTriggerListener(player.statusProperty(), new WeakInvalidationListener(playerStatusInvalidationListener));
    JavaFxUtil.addAndTriggerListener(teamMatchmakingService.getParty().ownerProperty(), new WeakInvalidationListener(partyOwnerInvalidationListener));
  }

  private void selectFactionsBasedOnParty() {
    uefLabel.setDisable(factionIsNotSelected(Faction.UEF));
    aeonLabel.setDisable(factionIsNotSelected(Faction.AEON));
    cybranLabel.setDisable(factionIsNotSelected(Faction.CYBRAN));
    seraphimLabel.setDisable(factionIsNotSelected(Faction.SERAPHIM));
  }

  private boolean factionIsNotSelected(Faction faction) {
    return teamMatchmakingService.getParty().getMembers().stream()
        .noneMatch(member -> member.getPlayer() == player && member.getFactions().contains(faction));
  }

  public void onKickPlayerButtonClicked(ActionEvent actionEvent) {
    teamMatchmakingService.kickPlayerFromParty(player);
  }

  public void onContextMenuRequested(ContextMenuEvent event) {
    if (contextMenuController != null) {
      PartyMemberContextMenuController controller = contextMenuController.get();
      if (controller != null) {
        controller.getContextMenu().show(playerItemRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        return;
      }
    }

    PartyMemberContextMenuController controller = uiService.loadFxml("theme/player_context_menu.fxml", PartyMemberContextMenuController.class);
    controller.setPlayer(player);
    controller.getContextMenu().show(playerItemRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());

    contextMenuController = new WeakReference<>(controller);
  }
}
