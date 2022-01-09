package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowUserReplaysEvent;
import com.google.common.eventbus.EventBus;
import org.springframework.util.Assert;

public class ViewReplaysMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean player) {
    Assert.notNull(player, "No player has been set");
    getBean(EventBus.class).post(new ShowUserReplaysEvent(player.getId()));
  }

  @Override
  protected boolean isItemVisible(PlayerBean player) {
    return player != null;
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.viewReplays");
  }
}
