package com.faforever.client.test.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerInfoWindowController;
import com.faforever.client.theme.UiService;

public class ShowPlayerInfoMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean object) {
    PlayerInfoWindowController controller = getBean(UiService.class).loadFxml("theme/user_info_window.fxml");
    controller.setPlayer(object);
    controller.setOwnerWindow(getParentPopup().getOwnerWindow());
    controller.show();
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.userInfo");
  }
}
