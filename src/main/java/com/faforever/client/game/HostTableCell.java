package com.faforever.client.game;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.player.PlayerService;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.WeakListener;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;

public class HostTableCell extends TableCell<GameBean, String> {

  private final PlayerService playerService;
  private final AvatarService avatarService;

  private final ImageView avatarImageView;

  public HostTableCell(PlayerService playerService, AvatarService avatarService) {
    this.playerService = playerService;
    this.avatarService = avatarService;
    this.avatarImageView = createAvatarImageView();

    setContentDisplay(ContentDisplay.RIGHT);
    setGraphicTextGap(3.0);
    setAlignment(Pos.BASELINE_CENTER);
  }

  private ImageView createAvatarImageView() {
    ImageView imageView = new ImageView();
    imageView.setFitHeight(14);
    imageView.setPickOnBounds(true);
    imageView.setPreserveRatio(true);
    return imageView;
  }

  @Override
  protected void updateItem(String item, boolean empty) {
    if (item == null || empty) {
      setText(null);
      setGraphic(null);
    } else {
      setText(item);
      playerService.getPlayerByNameIfOnline(item).map(PlayerBean::getAvatar).ifPresentOrElse(avatar -> {
        avatarImageView.setImage(avatarService.loadAvatar(avatar));
        setGraphic(avatarImageView);
      }, () -> setGraphic(null));
    }
  }
}
