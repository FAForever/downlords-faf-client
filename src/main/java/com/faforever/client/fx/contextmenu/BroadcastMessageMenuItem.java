package com.faforever.client.fx.contextmenu;

import com.faforever.client.i18n.I18n;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.commons.api.dto.GroupPermission;
import javafx.scene.control.TextInputDialog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class BroadcastMessageMenuItem extends AbstractMenuItem<Object> {

  private final I18n i18n;
  private final ModeratorService moderatorService;

  @Override
  protected void onClicked() {
    TextInputDialog broadcastMessageInputDialog = new TextInputDialog();
    broadcastMessageInputDialog.setTitle(i18n.get("chat.userContext.broadcast"));

    broadcastMessageInputDialog.showAndWait()
        .ifPresent(broadcastMessage -> {
              if (broadcastMessage.isBlank()) {
                log.info("Broadcast message is empty");
              } else {
                log.info("Sending broadcast message: {}", broadcastMessage);
                moderatorService.broadcastMessage(broadcastMessage);
              }
            }
        );
  }

  @Override
  protected boolean isDisplayed() {
    return moderatorService.getPermissions().contains(GroupPermission.ROLE_WRITE_MESSAGE);
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.broadcast");
  }
}
