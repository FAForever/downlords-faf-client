package com.faforever.client.fx.contextmenu;

import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.google.common.eventbus.EventBus;

import static com.faforever.client.player.SocialStatus.SELF;

public class SendPrivateMessageMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean object) {
    getBean(EventBus.class).post(new InitiatePrivateChatEvent(object.getUsername()));
  }

  @Override
  protected boolean isItemVisible() {
    return getObject().getSocialStatus() != SELF;
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.privateMessage");
  }
}
