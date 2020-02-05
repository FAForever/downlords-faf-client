package com.faforever.client.teammatchmaking;

import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.teammatchmaking.Party.PartyMember;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.util.RatingUtil;
import com.google.common.base.Strings;
import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static javafx.beans.binding.Bindings.createObjectBinding;
import static javafx.beans.binding.Bindings.createStringBinding;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PartyMemberItemController implements Controller<Node> {

  private final CountryFlagService countryFlagService;
  private final AvatarService avatarService;
  private final PlayerService playerService;
  private final TeamMatchmakingService teamMatchmakingService;
  private final I18n i18n;

  @FXML
  public Node playerItemRoot;

  @FXML
  public ImageView userImageView;
  @FXML
  public ImageView avatarImageView;
  @FXML
  public ImageView countryImageView;
  @FXML
  public Label clanLabel;
  @FXML
  public Label usernameLabel;
  @FXML
  public Label ratingLabel;
  @FXML
  public Label gameCountLabel;
  @FXML
  public JFXButton kickPlayerButton;
  @FXML
  public ToggleButton aeonButton;
  @FXML
  public ToggleButton cybranButton;
  @FXML
  public ToggleButton uefButton;
  @FXML
  public ToggleButton seraphimButton;
  @FXML
  public Label refreshingLabel;

  private PartyMember member;

  public PartyMemberItemController(CountryFlagService countryFlagService, AvatarService avatarService, PlayerService playerService, TeamMatchmakingService teamMatchmakingService, I18n i18n) {
    this.countryFlagService = countryFlagService;
    this.avatarService = avatarService;
    this.playerService = playerService;
    this.teamMatchmakingService = teamMatchmakingService;
    this.i18n = i18n;
  }

  @Override
  public void initialize() {
    clanLabel.managedProperty().bind(clanLabel.visibleProperty());
  }

  @Override
  public Node getRoot() {
    return playerItemRoot;
  }

  void setMember(PartyMember member) {
    this.member = member;
    Player player = member.getPlayer();

    userImageView.setImage(IdenticonUtil.createIdenticon(player.getId()));

    countryImageView.visibleProperty().bind(player.countryProperty().isNotEmpty());
    countryImageView.imageProperty().bind(createObjectBinding(() -> StringUtils.isEmpty(player.getCountry()) ? null : countryFlagService.loadCountryFlag(player.getCountry()).orElse(null), player.countryProperty()));

    avatarImageView.visibleProperty().bind(player.avatarUrlProperty().isNotNull().and(player.avatarUrlProperty().isNotEmpty()));
    avatarImageView.imageProperty().bind(createObjectBinding(() -> Strings.isNullOrEmpty(player.getAvatarUrl()) ? null : avatarService.loadAvatar(player.getAvatarUrl()), player.avatarUrlProperty()));

    clanLabel.visibleProperty().bind(player.clanProperty().isNotEmpty().and(player.clanProperty().isNotNull()));
    clanLabel.textProperty().bind(createStringBinding(() -> Strings.isNullOrEmpty(player.getClan()) ? "" : String.format("[%s]", player.getClan()), player.clanProperty()));

    usernameLabel.textProperty().bind(player.usernameProperty());

    ratingLabel.textProperty().bind(createStringBinding(() -> i18n.get("teammatchmaking.rating", RatingUtil.getRoundedGlobalRating(player)), player.globalRatingMeanProperty(), player.globalRatingDeviationProperty()));
    gameCountLabel.textProperty().bind(createStringBinding(() -> i18n.get("teammatchmaking.gameCount", player.getNumberOfGames()), player.numberOfGamesProperty()));

    kickPlayerButton.visibleProperty().bind(teamMatchmakingService.getParty().ownerProperty().isEqualTo(playerService.currentPlayerProperty()).and(playerService.currentPlayerProperty().isNotEqualTo(player)));

    // TODO: bind to sth else
    teamMatchmakingService.getParty().getMembers().addListener((Observable o) -> {
      boolean ready = teamMatchmakingService.getParty().getMembers().stream()
          .anyMatch(m -> m.getPlayer().getId() == player.getId() && m.isReady());
      ObservableList<String> classes = playerItemRoot.getStyleClass();
      if (ready && !classes.contains("card-playerReady")) {
        classes.add("card-playerReady");
      }
      if (!ready) {
        classes.remove("card-playerReady");
      }
    });
  }

  public void onKickPlayerButtonClicked(ActionEvent actionEvent) {
    teamMatchmakingService.kickPlayerFromParty(this.member.getPlayer());
  }

  public void onFactionButtonClicked(ActionEvent actionEvent) {
    if (!playerService.getCurrentPlayer().get().equals(this.member.getPlayer())) {
      return;
    }

    boolean[] factions = {
        aeonButton.isSelected(),
        cybranButton.isSelected(),
        uefButton.isSelected(),
        seraphimButton.isSelected()
    };

    teamMatchmakingService.setPartyFactions(factions);

    //TODO: remove
    refreshingLabel.setVisible(true);
    //TODO:NO NO NO NO
    new Thread(() -> {
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
      }
      Platform.runLater(() -> refreshingLabel.setVisible(false));
    }).start();
  }
}
