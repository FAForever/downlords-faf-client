package com.faforever.client.fx.contextmenu;

import com.faforever.client.i18n.I18n;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.commons.api.dto.GroupPermission;
import javafx.scene.control.TextInputDialog;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BroadcastMessageMenuItem extends AbstractMenuItem<Object> {

  @Override
  protected void onClicked(Object object) {
    TextInputDialog broadcastMessageInputDialog = new TextInputDialog();
    broadcastMessageInputDialog.setTitle(getBean(I18n.class).get("chat.userContext.broadcast"));

    broadcastMessageInputDialog.showAndWait()
        .ifPresent(broadcastMessage -> {
              if (broadcastMessage.isBlank()) {
                log.error("Broadcast message is empty: {}", broadcastMessage);
              } else {
                log.info("Sending broadcast message: {}", broadcastMessage);
                getBean(ModeratorService.class).broadcastMessage(broadcastMessage);
              }
            }
        );
  }

  @Override
  protected boolean isItemVisible() {
    return getBean(ModeratorService.class).getPermissions().contains(GroupPermission.ROLE_WRITE_MESSAGE);
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.broadcast");
  }
}
