package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowUserReplaysEvent;
import com.google.common.eventbus.EventBus;

public class ViewReplaysMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean player) {
    getBean(EventBus.class).post(new ShowUserReplaysEvent(player.getId()));
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.viewReplays");
  }
}
