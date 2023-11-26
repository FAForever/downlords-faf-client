package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.social.SocialService;
import com.faforever.client.util.Assert;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class RemovePlayerNoteMenuItem extends AbstractMenuItem<PlayerBean> {

  private final PlayerService playerService;
  private final SocialService socialService;
  private final I18n i18n;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "No player has been set");
    socialService.removeNote(object);
  }

  @Override
  protected boolean isDisplayed() {
    return object != null && !StringUtils.isBlank(object.getNote()) && playerService.getCurrentPlayer() != object;
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.removeNote");
  }
}
