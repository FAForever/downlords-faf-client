package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import static com.faforever.client.player.SocialStatus.FRIEND;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class RemoveFriendMenuItem extends AbstractMenuItem<PlayerBean> {

  private final I18n i18n;
  private final PlayerService playerService;

  @Override
  protected void onClicked(PlayerBean player) {
    Assert.notNull(player, "No player has been set");
    playerService.removeFriend(player);
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.removeFriend");
  }

  @Override
  protected boolean isItemVisible(PlayerBean player) {
    return player != null && player.getSocialStatus() == FRIEND;
  }
}
