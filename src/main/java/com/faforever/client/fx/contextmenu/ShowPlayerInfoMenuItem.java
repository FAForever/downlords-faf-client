package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerInfoWindowController;
import com.faforever.client.theme.UiService;
import org.springframework.util.Assert;

public class ShowPlayerInfoMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean player) {
    Assert.notNull(player, "No player has been set");
    PlayerInfoWindowController controller = getBean(UiService.class).loadFxml("theme/user_info_window.fxml");
    controller.setPlayer(player);
    controller.setOwnerWindow(getParentPopup().getOwnerWindow());
    controller.show();
  }

  @Override
  protected boolean isItemVisible(PlayerBean player) {
    return player != null;
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.userInfo");
  }
}
