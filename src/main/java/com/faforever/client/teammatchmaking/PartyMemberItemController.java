package com.faforever.client.teammatchmaking;

import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.Faction;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.teammatchmaking.Party.PartyMember;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import com.google.common.base.Strings;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.WeakChangeListener;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;

import static javafx.beans.binding.Bindings.createObjectBinding;
import static javafx.beans.binding.Bindings.createStringBinding;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class PartyMemberItemController implements Controller<Node> {

  private static final PseudoClass LEADER_PSEUDO_CLASS = PseudoClass.getPseudoClass("leader");
  private static final PseudoClass PLAYING_PSEUDO_CLASS = PseudoClass.getPseudoClass("playing");

  private final CountryFlagService countryFlagService;
  private final AvatarService avatarService;
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

  private Player player;
  private WeakReference<PartyMemberContextMenuController> contextMenuController = null;

  @Override
  public void initialize() {
    clanLabel.managedProperty().bind(clanLabel.visibleProperty());
  }

  @Override
  public Node getRoot() {
    return playerItemRoot;
  }

  public void setMember(PartyMember member) {
    this.player = member.getPlayer();

    initializeBindings();
    playerCard.pseudoClassStateChanged(LEADER_PSEUDO_CLASS, teamMatchmakingService.getParty().getOwner().equals(player));

    playerStatusImageView.setImage(uiService.getThemeImage(UiService.CHAT_LIST_STATUS_PLAYING));
    player.statusProperty().addListener(new WeakChangeListener<>((observable, oldValue, newValue) -> markMemberBusy(newValue)));
    markMemberBusy(player.statusProperty().get());

    selectFactionsBasedOnParty();
  }

  private void markMemberBusy(PlayerStatus status) {
    if (status != PlayerStatus.IDLE) {
      Platform.runLater(() -> {
        playerStatusImageView.setVisible(true);
        playerCard.pseudoClassStateChanged(PLAYING_PSEUDO_CLASS, true);
        teamMatchmakingService.getPlayersInGame().add(player);
      });
    } else {
      Platform.runLater(() -> {
        playerStatusImageView.setVisible(false);
        playerCard.pseudoClassStateChanged(PLAYING_PSEUDO_CLASS, false);
        teamMatchmakingService.getPlayersInGame().remove(player);
      });
    }
  }

  private void initializeBindings() {
    countryImageView.imageProperty().bind(createObjectBinding(() ->
        countryFlagService.loadCountryFlag(player.getCountry()).orElse(null), player.countryProperty()));
    avatarImageView.visibleProperty().bind(player.avatarUrlProperty().isNotNull().and(player.avatarUrlProperty().isNotEmpty()));
    avatarImageView.imageProperty().bind(createObjectBinding(() -> Strings.isNullOrEmpty(player.getAvatarUrl()) ? null : avatarService.loadAvatar(player.getAvatarUrl()), player.avatarUrlProperty()));
    leagueImageView.setManaged(false);
    JavaFxUtil.bindManagedToVisible(clanLabel, avatarImageView, playerStatusImageView);

    clanLabel.visibleProperty().bind(player.clanProperty().isNotEmpty().and(player.clanProperty().isNotNull()));
    clanLabel.textProperty().bind(createStringBinding(() -> Strings.isNullOrEmpty(player.getClan()) ? "" : String.format("[%s]", player.getClan()), player.clanProperty()));
    usernameLabel.textProperty().bind(player.usernameProperty());
    leagueLabel.textProperty().bind(createStringBinding(
        () -> i18n.get("leaderboard.divisionName", RatingUtil.getLeaderboardRating(player)).toUpperCase(),
        player.globalRatingMeanProperty())); // TODO: replace this with divisionproperty once it is available
    gameCountLabel.textProperty().bind(createStringBinding(
        () -> i18n.get("teammatchmaking.gameCount", player.getNumberOfGames()).toUpperCase(),
        player.numberOfGamesProperty()));
    crownLabel.visibleProperty().bind(teamMatchmakingService.getParty().ownerProperty().isEqualTo(player));

    BooleanBinding isDifferentPlayerBinding = playerService.currentPlayerProperty().isNotEqualTo(player);
    kickPlayerButton.visibleProperty().bind(teamMatchmakingService.getParty().ownerProperty().isEqualTo(playerService.currentPlayerProperty()).and(isDifferentPlayerBinding));
    JavaFxUtil.bindManagedToVisible(kickPlayerButton);
  }

  private void selectFactionsBasedOnParty() {
    uefLabel.setDisable(!isFactionSelectedInParty(Faction.UEF));
    aeonLabel.setDisable(!isFactionSelectedInParty(Faction.AEON));
    cybranLabel.setDisable(!isFactionSelectedInParty(Faction.CYBRAN));
    seraphimLabel.setDisable(!isFactionSelectedInParty(Faction.SERAPHIM));
  }

  private boolean isFactionSelectedInParty(Faction faction) {
    return teamMatchmakingService.getParty().getMembers().stream()
        .anyMatch(m -> m.getPlayer().getId() == player.getId() && m.getFactions().contains(faction));
  }

  public void onKickPlayerButtonClicked(ActionEvent actionEvent) {
    teamMatchmakingService.kickPlayerFromParty(this.player);
  }

  public void onContextMenuRequested(ContextMenuEvent event) {
    if (contextMenuController != null) {
      PartyMemberContextMenuController controller = contextMenuController.get();
      if (controller != null) {
        controller.getContextMenu().show(playerItemRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        return;
      }
    }

    PartyMemberContextMenuController controller = uiService.loadFxml("theme/play/teammatchmaking/party_member_context_menu.fxml");
    controller.setPlayer(player);
    controller.getContextMenu().show(playerItemRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());

    contextMenuController = new WeakReference<>(controller);
  }
}
