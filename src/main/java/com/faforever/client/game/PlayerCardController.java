package com.faforever.client.game;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.contextmenu.AddFoeMenuItem;
import com.faforever.client.fx.contextmenu.AddFriendMenuItem;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.CopyUsernameMenuItem;
import com.faforever.client.fx.contextmenu.DoPlayerNoteMenuItem;
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
import com.faforever.client.theme.UiService;
import com.faforever.commons.api.dto.Faction;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
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
import java.util.Optional;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class PlayerCardController implements Controller<Node> {

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

  private PlayerBean player;

  private Tooltip noteTooltip;
  private final InvalidationListener notePropertyListener = observable -> {
    boolean emptyNote = StringUtils.isBlank(player.getNote());
    playerInfo.setUnderline(!emptyNote);
    if (emptyNote) {
      clearNoteTooltip();
    } else {
      updateNoteTooltip();
    }
  };

  public void setPlayer(PlayerBean player, Integer rating, Faction faction) {
    if (player == null) {
      return;
    }
    this.player = player;
    countryFlagService.loadCountryFlag(player.getCountry()).ifPresentOrElse(image ->
        countryImageView.setImage(image), () -> countryImageView.setVisible(false));
    Optional.ofNullable(player.getAvatar()).map(avatarService::loadAvatar).ifPresent(avatarImage -> {
      Tooltip.install(avatarImageView, new Tooltip(player.getAvatar().getDescription()));
      avatarImageView.setImage(avatarImage);
      avatarImageView.setVisible(true);
    });
    playerInfo.setText(rating != null
        ? i18n.get("userInfo.tooltipFormat.withRating", player.getUsername(), rating)
        : i18n.get("userInfo.tooltipFormat.noRating", player.getUsername()));
    setFactionIcon(faction);
    JavaFxUtil.bind(foeIconText.visibleProperty(),
        Bindings.createBooleanBinding(() -> player.getSocialStatus() == SocialStatus.FOE, player.socialStatusProperty()));
    JavaFxUtil.bind(friendIconText.visibleProperty(),
        Bindings.createBooleanBinding(() -> player.getSocialStatus() == SocialStatus.FRIEND, player.socialStatusProperty()));
    JavaFxUtil.addAndTriggerListener(player.noteProperty(), new WeakInvalidationListener(notePropertyListener));
  }

  public Node getRoot() {
    return root;
  }

  public void openContextMenu(MouseEvent event) {
    if (player != null) {
      contextMenuBuilder.newBuilder()
          .addItem(ShowPlayerInfoMenuItem.class, player)
          .addItem(SendPrivateMessageMenuItem.class, player.getUsername())
          .addItem(CopyUsernameMenuItem.class, player.getUsername())
          .addSeparator()
          .addItem(AddFriendMenuItem.class, player)
          .addItem(RemoveFriendMenuItem.class, player)
          .addItem(AddFoeMenuItem.class, player)
          .addItem(RemoveFoeMenuItem.class, player)
          .addSeparator()
          .addItem(DoPlayerNoteMenuItem.class, player)
          .addItem(RemovePlayerNoteMenuItem.class, player)
          .addSeparator()
          .addItem(ReportPlayerMenuItem.class, player)
          .addSeparator()
          .addItem(ViewReplaysMenuItem.class, player)
          .build()
          .show(getRoot().getScene().getWindow(), event.getScreenX(), event.getScreenY());
    }
  }

  @Override
  public void initialize() {
    JavaFxUtil.bindManagedToVisible(factionIcon, foeIconText, factionImage, friendIconText, countryImageView);
  }

  private void setFactionIcon(Faction faction) {
    if (faction == null) {
      return;
    }

    factionIcon.setVisible(true);
    List<String> classes = factionIcon.getStyleClass();
    switch (faction) {
      case AEON -> classes.add(UiService.AEON_STYLE_CLASS);
      case CYBRAN -> classes.add(UiService.CYBRAN_STYLE_CLASS);
      case SERAPHIM -> classes.add(UiService.SERAPHIM_STYLE_CLASS);
      case UEF -> classes.add(UiService.UEF_STYLE_CLASS);
      default -> {
        factionIcon.setVisible(false);
        factionImage.setVisible(true);
        factionImage.setImage(new Image(UiService.RANDOM_FACTION_IMAGE));
      }
    }
  }

  private void clearNoteTooltip() {
    playerInfo.setTooltip(null);
    noteTooltip = null;
  }

  private void updateNoteTooltip() {
    if (noteTooltip == null) {
      noteTooltip = new Tooltip();
      noteTooltip.setShowDuration(Duration.seconds(30));
      noteTooltip.setShowDelay(Duration.ZERO);
      noteTooltip.setHideDelay(Duration.ZERO);
      playerInfo.setTooltip(noteTooltip);
    }
    noteTooltip.setText(player.getNote());
  }
}
