package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;

import static com.faforever.client.player.SocialStatus.FOE;

public class RemoveFoeMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean player) {
    getBean(PlayerService.class).removeFoe(player);
  }

  @Override
  protected boolean isItemVisible() {
    return getObject().getSocialStatus() == FOE;
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.removeFoe");
  }
}
