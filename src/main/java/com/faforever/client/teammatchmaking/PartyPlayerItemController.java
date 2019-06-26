package com.faforever.client.teammatchmaking;

import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.util.RatingUtil;
import com.google.common.base.Strings;
import com.jfoenix.controls.JFXButton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static javafx.beans.binding.Bindings.createObjectBinding;
import static javafx.beans.binding.Bindings.createStringBinding;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PartyPlayerItemController implements Controller<Node> {

  private final CountryFlagService countryFlagService;
  private final AvatarService avatarService;
  private final PlayerService playerService;
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

  public PartyPlayerItemController(CountryFlagService countryFlagService, AvatarService avatarService, PlayerService playerService, I18n i18n) {
    this.countryFlagService = countryFlagService;
    this.avatarService = avatarService;
    this.playerService = playerService;
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

  void setPlayer(Player player) {
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


    if (player == playerService.getCurrentPlayer().orElse(null)) {
      kickPlayerButton.setVisible(false);
    }
    //TODO: hide kickPlayerButton if not host of the party

    //TODO READY status for other players, ready button, sendReady
  }

  public void onKickPlayerButtonClicked(ActionEvent actionEvent) {
    //TODO:
  }
}
