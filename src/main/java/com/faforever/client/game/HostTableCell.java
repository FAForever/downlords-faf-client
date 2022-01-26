package com.faforever.client.game;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.player.PlayerService;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HostTableCell extends TableCell<GameBean, String> {

  private final PlayerService playerService;
  private final AvatarService avatarService;

  @Override
  protected void updateItem(String item, boolean empty) {
    if (item == null || empty) {
      setText(null);
      setGraphic(null);
    } else {
      setText(item);
      playerService.getPlayerByNameIfOnline(item).map(PlayerBean::getAvatar).ifPresentOrElse(avatar -> {
        ImageView avatarImage = new ImageView(avatarService.loadAvatar(avatar));
        avatarImage.setFitWidth(40);
        avatarImage.setFitHeight(20);
        avatarImage.setPickOnBounds(true);
        avatarImage.setPreserveRatio(true);
        setGraphic(avatarImage);
        Tooltip.install(avatarImage, new Tooltip(avatar.getDescription()));
        setContentDisplay(ContentDisplay.RIGHT);
        setGraphicTextGap(3.0);
      }, () -> setGraphic(null));
    }
  }
}
