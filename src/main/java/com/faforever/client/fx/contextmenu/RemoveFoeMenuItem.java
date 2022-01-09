package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import org.springframework.util.Assert;

import static com.faforever.client.player.SocialStatus.FOE;

public class RemoveFoeMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean player) {
    Assert.notNull(player, "No player has been set");
    getBean(PlayerService.class).removeFoe(player);
  }

  @Override
  protected boolean isItemVisible(PlayerBean player) {
    return player != null && player.getSocialStatus() == FOE;
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.removeFoe");
  }
}
