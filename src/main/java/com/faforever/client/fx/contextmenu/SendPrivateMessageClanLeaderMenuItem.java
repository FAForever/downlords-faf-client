package com.faforever.client.fx.contextmenu;

import com.faforever.client.chat.ChatService;
import com.faforever.client.clan.ClanService;
import com.faforever.client.domain.ClanBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.util.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class SendPrivateMessageClanLeaderMenuItem extends AbstractMenuItem<PlayerBean> {

  private final I18n i18n;
  private final ClanService clanService;
  private final ChatService chatService;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no player has been set");

    clanService.getClanByTag(object.getClan())
        .thenAccept(possibleClan -> possibleClan.map(ClanBean::getLeader)
            .map(PlayerBean::getUsername)
                                                .ifPresent(chatService::onInitiatePrivateChat));
  }

  @Override
  protected String getStyleIcon() {
    return "bubble-icon";
  }

  @Override
  protected boolean isDisplayed() {
    return object != null;
  }

  @Override
  protected String getItemText() {
    return i18n.get("clan.messageLeader");
  }
}
