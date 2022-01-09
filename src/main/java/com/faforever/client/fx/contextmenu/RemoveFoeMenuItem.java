package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import static com.faforever.client.player.SocialStatus.FOE;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class RemoveFoeMenuItem extends AbstractMenuItem<PlayerBean> {

  private final I18n i18n;
  private final PlayerService playerService;

  @Override
  protected void onClicked() {
    playerService.removeFoe(getObject());
  }

  @Override
  protected boolean isItemVisible() {
    PlayerBean player = getUnsafeObject();
    return player != null && player.getSocialStatus() == FOE;
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.removeFoe");
  }
}
