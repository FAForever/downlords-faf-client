package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.client.player.SocialStatus.SELF;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class AddFriendMenuItem extends AbstractMenuItem<PlayerBean> {

  private final PlayerService playerService;
  private final I18n i18n;

  @Override
  protected void onClicked() {
    PlayerBean player = getObject();
    Assert.notNull(player, "No player has been set");
    if (player.getSocialStatus() == FOE) {
      playerService.removeFoe(player);
    }
    playerService.addFriend(player);
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.addFriend");
  }

  @Override
  protected boolean isItemVisible() {
    PlayerBean player = getObject();
    if (player == null) {
      return false;
    }
    SocialStatus status = player.getSocialStatus();
    return status != FRIEND && status != SELF;
  }
}
