package com.faforever.client.game;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.LeaderboardRatingJournalBean;
import com.faforever.client.domain.PlayerBean;
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
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import com.faforever.commons.api.dto.Faction;
import javafx.beans.binding.Bindings;
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
  private final ContextMenuBuilder contextMenuBuilder;
  private final I18n i18n;

  public Label playerInfo;
  public ImageView countryImageView;
  public ImageView avatarImageView;
  public Label foeIconText;
  public HBox root;
  public Label friendIconText;
  public Region factionIcon;
  public ImageView factionImage;
  public Label noteIcon;
  public Label ratingChange;

  private final ObjectProperty<PlayerBean> player = new SimpleObjectProperty<>();
  private final ObjectProperty<GamePlayerStatsBean> playerStats = new SimpleObjectProperty<>();
  private final ObjectProperty<Integer> rating = new SimpleObjectProperty<>();
  private final ObjectProperty<Faction> faction = new SimpleObjectProperty<>();
  private final Tooltip noteTooltip = new Tooltip();
  private final Tooltip avatarTooltip = new Tooltip();

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(factionIcon, foeIconText, factionImage, friendIconText, countryImageView, noteIcon);
    countryImageView.visibleProperty().bind(countryImageView.imageProperty().isNotNull());
    avatarImageView.visibleProperty().bind(avatarImageView.imageProperty().isNotNull());

    factionImage.setImage(uiService.getImage(ThemeService.RANDOM_FACTION_IMAGE));
    factionImage.visibleProperty().bind(faction.map(value -> value == Faction.RANDOM));
    factionIcon.visibleProperty().bind(faction.map(value -> value != Faction.RANDOM && value != Faction.CIVILIAN));

    countryImageView.imageProperty()
        .bind(player.flatMap(PlayerBean::countryProperty)
            .map(country -> countryFlagService.loadCountryFlag(country).orElse(null))
            .when(showing));
    avatarImageView.imageProperty()
        .bind(player.flatMap(PlayerBean::avatarProperty).map(avatarService::loadAvatar).when(showing));
    playerInfo.textProperty()
        .bind(player.flatMap(PlayerBean::usernameProperty)
            .flatMap(username -> rating.map(value -> i18n.get("userInfo.tooltipFormat.withRating", username, value))
                .orElse(i18n.get("userInfo.tooltipFormat.noRating", username)))
            .when(showing));
    foeIconText.visibleProperty()
        .bind(player.flatMap(PlayerBean::socialStatusProperty)
            .map(socialStatus -> socialStatus == SocialStatus.FOE)
            .when(showing));
    friendIconText.visibleProperty()
        .bind(player.flatMap(PlayerBean::socialStatusProperty)
            .map(socialStatus -> socialStatus == SocialStatus.FRIEND)
            .when(showing));
    player.flatMap(PlayerBean::noteProperty)
        .when(showing)
        .addListener((SimpleChangeListener<String>) this::onNoteChanged);

    ratingChange.visibleProperty().bind(playerStats.isNotNull());
    ObservableValue<Integer> ratingChangeObservable = playerStats.flatMap(stats -> Bindings.valueAt(stats.getLeaderboardRatingJournals(), 0))
        .map(this::getRatingChange);
    ratingChange.textProperty().bind(ratingChangeObservable.map(i18n::numberWithSign));
    ratingChangeObservable.addListener((observable, oldValue, newValue) -> onRatingChanged(oldValue, newValue));

    faction.addListener(((observable, oldValue, newValue) -> onFactionChanged(oldValue, newValue)));

    noteTooltip.textProperty().bind(player.flatMap(PlayerBean::noteProperty).when(showing));
    noteTooltip.setShowDelay(Duration.ZERO);
    noteTooltip.setShowDuration(Duration.seconds(30));
    noteIcon.visibleProperty().bind(noteTooltip.textProperty().isNotEmpty());

    avatarTooltip.textProperty()
        .bind(player.flatMap(PlayerBean::avatarProperty).flatMap(AvatarBean::descriptionProperty).when(showing));
    avatarTooltip.setShowDelay(Duration.ZERO);
    avatarTooltip.setShowDuration(Duration.seconds(30));
    Tooltip.install(avatarImageView, avatarTooltip);
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
    PlayerBean playerBean = player.get();
    if (playerBean != null) {
      contextMenuBuilder.newBuilder()
          .addItem(ShowPlayerInfoMenuItem.class, playerBean)
          .addItem(SendPrivateMessageMenuItem.class, playerBean.getUsername())
          .addItem(CopyUsernameMenuItem.class, playerBean.getUsername())
          .addSeparator()
          .addItem(AddFriendMenuItem.class, playerBean)
          .addItem(RemoveFriendMenuItem.class, playerBean)
          .addItem(AddFoeMenuItem.class, playerBean)
          .addItem(RemoveFoeMenuItem.class, playerBean)
          .addSeparator()
          .addItem(AddEditPlayerNoteMenuItem.class, playerBean)
          .addItem(RemovePlayerNoteMenuItem.class, playerBean)
          .addSeparator()
          .addItem(ReportPlayerMenuItem.class, playerBean)
          .addSeparator()
          .addItem(ViewReplaysMenuItem.class, playerBean)
          .build()
          .show(getRoot().getScene().getWindow(), event.getScreenX(), event.getScreenY());
    }
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

  private Integer getRatingChange(LeaderboardRatingJournalBean ratingJournal) {
    if (ratingJournal.getMeanAfter() != null && ratingJournal.getDeviationAfter() != null) {
      int newRating = RatingUtil.getRating(ratingJournal.getMeanAfter(), ratingJournal.getDeviationAfter());
      int oldRating = RatingUtil.getRating(ratingJournal.getMeanBefore(), ratingJournal.getDeviationBefore());

      return newRating - oldRating;
    }

    return null;
  }

  public PlayerBean getPlayer() {
    return player.get();
  }

  public ObjectProperty<PlayerBean> playerProperty() {
    return player;
  }

  public void setPlayer(PlayerBean player) {
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

  public Faction getFaction() {
    return faction.get();
  }

  public ObjectProperty<Faction> factionProperty() {
    return faction;
  }

  public void setFaction(Faction faction) {
    this.faction.set(faction);
  }

  public GamePlayerStatsBean getPlayerStats() {
    return playerStats.get();
  }

  public ObjectProperty<GamePlayerStatsBean> playerStatsProperty() {
    return playerStats;
  }

  public void setPlayerStats(GamePlayerStatsBean playerStats) {
    this.playerStats.set(playerStats);
  }
}
