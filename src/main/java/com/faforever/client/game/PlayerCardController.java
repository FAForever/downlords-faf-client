package com.faforever.client.game;

import com.faforever.client.avatar.Avatar;
import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.api.GamePlayerStats;
import com.faforever.client.domain.api.LeaderboardRatingJournal;
import com.faforever.client.domain.api.Subdivision;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.fx.contextmenu.AddEditPlayerNoteMenuItem;
import com.faforever.client.fx.contextmenu.AddFoeMenuItem;
import com.faforever.client.fx.contextmenu.AddFriendMenuItem;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.CopyUsernameMenuItem;
import com.faforever.client.fx.contextmenu.RemoveFoeMenuItem;
import com.faforever.client.fx.contextmenu.RemoveFriendMenuItem;
import com.faforever.client.fx.contextmenu.RemovePlayerNoteMenuItem;
import com.faforever.client.fx.contextmenu.ReportPlayerMenuItem;
import com.faforever.client.fx.contextmenu.SendPrivateMessageMenuItem;
import com.faforever.client.fx.contextmenu.ShowPlayerInfoMenuItem;
import com.faforever.client.fx.contextmenu.ViewReplaysMenuItem;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.replay.DisplayType;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import com.faforever.commons.api.dto.Faction;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class PlayerCardController extends NodeController<Node> {

  private static final PseudoClass POSITIVE = PseudoClass.getPseudoClass("positive");
  private static final PseudoClass NEGATIVE = PseudoClass.getPseudoClass("negative");

  private final UiService uiService;
  private final CountryFlagService countryFlagService;
  private final AvatarService avatarService;
  private final LeaderboardService leaderboardService;
  private final ContextMenuBuilder contextMenuBuilder;
  private final I18n i18n;

  public Label playerInfo;
  public StackPane avatarStackPane;
  public ImageView countryImageView;
  public ImageView avatarImageView;
  public Label foeIconText;
  public HBox root;
  public Label friendIconText;
  public Region factionIcon;
  public ImageView factionImage;
  public ImageView divisionImageView;
  public Label noteIcon;
  public Label ratingLabel;
  public Label ratingChange;

  private final ObjectProperty<PlayerInfo> player = new SimpleObjectProperty<>();
  private final ObjectProperty<GamePlayerStats> playerStats = new SimpleObjectProperty<>();
  private final ObjectProperty<Integer> rating = new SimpleObjectProperty<>();
  private final ObjectProperty<Subdivision> division = new SimpleObjectProperty<>();
  private final ObjectProperty<Faction> faction = new SimpleObjectProperty<>();
  private final ObjectProperty<DisplayType> displayType = new SimpleObjectProperty<>();
  private final Tooltip noteTooltip = new Tooltip();
  private final Tooltip avatarTooltip = new Tooltip();
  private final Tooltip divisionTooltip = new Tooltip();

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(avatarStackPane, factionIcon, foeIconText, factionImage, friendIconText,
                                    countryImageView, divisionImageView, ratingLabel, ratingChange, noteIcon);
    countryImageView.visibleProperty().bind(countryImageView.imageProperty().isNotNull());
    avatarImageView.visibleProperty().bind(avatarImageView.imageProperty().isNotNull());
    divisionImageView.visibleProperty().bind(divisionImageView.imageProperty().isNotNull());
    ratingLabel.visibleProperty().bind(rating.isNotNull().and(displayType.isEqualTo(DisplayType.RATING)));
    ratingChange.visibleProperty().bind(playerStats.isNotNull().and(displayType.isEqualTo(DisplayType.RATING)));

    factionImage.setImage(uiService.getImage(ThemeService.RANDOM_FACTION_IMAGE));
    factionImage.visibleProperty().bind(faction.map(value -> value == Faction.RANDOM));
    factionIcon.visibleProperty().bind(faction.map(value -> value != Faction.RANDOM && value != Faction.CIVILIAN));

    countryImageView.imageProperty().bind(player.flatMap(PlayerInfo::countryProperty)
            .map(country -> countryFlagService.loadCountryFlag(country).orElse(null))
            .when(showing));
    avatarImageView.imageProperty()
                   .bind(player.flatMap(PlayerInfo::avatarProperty).map(avatarService::loadAvatar).when(showing));
    divisionImageView.imageProperty()
            .bind(division.map(Subdivision::smallImageUrl).map(leaderboardService::loadDivisionImage).when(showing));
    playerInfo.textProperty().bind(player.flatMap(PlayerInfo::usernameProperty)
            .when(showing));
    ratingLabel.textProperty().bind(rating.map(value -> i18n.get("game.tooltip.ratingFormat", value))
                                        .when(showing));
    foeIconText.visibleProperty().bind(player.flatMap(PlayerInfo::socialStatusProperty)
            .map(socialStatus -> socialStatus == SocialStatus.FOE)
            .when(showing));
    friendIconText.visibleProperty().bind(player.flatMap(PlayerInfo::socialStatusProperty)
            .map(socialStatus -> socialStatus == SocialStatus.FRIEND)
            .when(showing));
    player.flatMap(PlayerInfo::noteProperty)
        .when(showing)
        .addListener((SimpleChangeListener<String>) this::onNoteChanged);

    ObservableValue<Integer> ratingChangeObservable = playerStats.map(GamePlayerStats::leaderboardRatingJournals)
                                                                 .map(
                                                                     journals -> journals.isEmpty() ? null : journals.getFirst())
        .map(this::getRatingChange);
    ratingChange.textProperty().bind(ratingChangeObservable.map(i18n::numberWithSign));
    ratingChangeObservable.addListener((observable, oldValue, newValue) -> onRatingChanged(oldValue, newValue));

    faction.addListener(((observable, oldValue, newValue) -> onFactionChanged(oldValue, newValue)));

    noteTooltip.textProperty().bind(player.flatMap(PlayerInfo::noteProperty).when(showing));
    noteTooltip.setShowDelay(Duration.ZERO);
    noteTooltip.setShowDuration(Duration.seconds(30));
    noteIcon.visibleProperty().bind(noteTooltip.textProperty().isNotEmpty());

    avatarTooltip.textProperty()
                 .bind(player.flatMap(PlayerInfo::avatarProperty).map(Avatar::description).when(showing));
    avatarTooltip.setShowDelay(Duration.ZERO);
    avatarTooltip.setShowDuration(Duration.seconds(30));
    Tooltip.install(avatarImageView, avatarTooltip);

    divisionTooltip.textProperty().bind(
        division.map(value -> i18n.get("leaderboard.divisionName", i18n.get("leagues.divisionName.%s".formatted(value.division().nameKey())), value.nameKey()))
            .when(showing));
    divisionTooltip.setShowDelay(Duration.ZERO);
    divisionTooltip.setShowDuration(Duration.seconds(30));
    Tooltip.install(divisionImageView, divisionTooltip);
  }

  private void onNoteChanged(String newValue) {
    if (StringUtils.isEmpty(newValue)) {
      Tooltip.uninstall(root, noteTooltip);
    } else {
      Tooltip.install(root, noteTooltip);
    }
  }

  @Override
  public Node getRoot() {
    return root;
  }

  public void openContextMenu(MouseEvent event) {
    PlayerInfo playerInfo = player.get();
    if (playerInfo != null) {
      contextMenuBuilder.newBuilder()
                        .addItem(ShowPlayerInfoMenuItem.class, playerInfo)
                        .addItem(SendPrivateMessageMenuItem.class, playerInfo.getUsername())
                        .addItem(CopyUsernameMenuItem.class, playerInfo.getUsername())
          .addSeparator()
                        .addItem(AddFriendMenuItem.class, playerInfo)
                        .addItem(RemoveFriendMenuItem.class, playerInfo)
                        .addItem(AddFoeMenuItem.class, playerInfo)
                        .addItem(RemoveFoeMenuItem.class, playerInfo)
          .addSeparator()
                        .addItem(AddEditPlayerNoteMenuItem.class, playerInfo)
                        .addItem(RemovePlayerNoteMenuItem.class, playerInfo)
          .addSeparator()
                        .addItem(ReportPlayerMenuItem.class, playerInfo)
          .addSeparator()
                        .addItem(ViewReplaysMenuItem.class, playerInfo)
          .build()
          .show(getRoot().getScene().getWindow(), event.getScreenX(), event.getScreenY());
    }
    event.consume();
  }

  private void onFactionChanged(Faction oldFaction, Faction newFaction) {
    List<String> classes = factionIcon.getStyleClass();
    if (oldFaction != null) {
      switch (oldFaction) {
        case AEON -> classes.remove(ThemeService.AEON_STYLE_CLASS);
        case CYBRAN -> classes.remove(ThemeService.CYBRAN_STYLE_CLASS);
        case SERAPHIM -> classes.remove(ThemeService.SERAPHIM_STYLE_CLASS);
        case UEF -> classes.remove(ThemeService.UEF_STYLE_CLASS);
      }
    }

    if (newFaction != null) {
      switch (newFaction) {
        case AEON -> classes.add(ThemeService.AEON_STYLE_CLASS);
        case CYBRAN -> classes.add(ThemeService.CYBRAN_STYLE_CLASS);
        case SERAPHIM -> classes.add(ThemeService.SERAPHIM_STYLE_CLASS);
        case UEF -> classes.add(ThemeService.UEF_STYLE_CLASS);
      }
    }
  }

  private void onRatingChanged(Integer oldValue, Integer newValue) {
    if (oldValue != null) {
      ratingChange.pseudoClassStateChanged(oldValue < 0 ? NEGATIVE : POSITIVE, false);
    }

    if (newValue != null) {
      ratingChange.pseudoClassStateChanged(newValue < 0 ? NEGATIVE : POSITIVE, true);
    }
  }

  private Integer getRatingChange(LeaderboardRatingJournal ratingJournal) {
    if (ratingJournal.meanAfter() != null && ratingJournal.deviationAfter() != null) {
      int newRating = RatingUtil.getRating(ratingJournal.meanAfter(), ratingJournal.deviationAfter());
      int oldRating = RatingUtil.getRating(ratingJournal.meanBefore(), ratingJournal.deviationBefore());

      return newRating - oldRating;
    }

    return null;
  }

  public void removeAvatar() {
    this.avatarStackPane.setVisible(false);
  }

  public PlayerInfo getPlayer() {
    return player.get();
  }

  public ObjectProperty<PlayerInfo> playerProperty() {
    return player;
  }

  public void setPlayer(PlayerInfo player) {
    this.player.set(player);
  }

  public Integer getRating() {
    return rating.get();
  }

  public ObjectProperty<Integer> ratingProperty() {
    return rating;
  }

  public void setRating(Integer rating) {
    this.rating.set(rating);
  }

  public Subdivision getDivision() {
    return division.get();
  }

  public ObjectProperty<Subdivision> divisionProperty() {
    return division;
  }

  public void setDivision(Subdivision subdivision) {
    this.division.set(subdivision);
  }

  public Faction getFaction() {
    return faction.get();
  }

  public ObjectProperty<Faction> factionProperty() {
    return faction;
  }

  public void setFaction(Faction faction) {
    this.faction.set(faction);
  }

  public ObjectProperty<DisplayType> displayTypeProperty() {
    return displayType;
  }

  public GamePlayerStats getPlayerStats() {
    return playerStats.get();
  }

  public ObjectProperty<GamePlayerStats> playerStatsProperty() {
    return playerStats;
  }

  public void setPlayerStats(GamePlayerStats playerStats) {
    this.playerStats.set(playerStats);
  }
}
