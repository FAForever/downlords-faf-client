package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.player.PlayerNoteService;
import com.faforever.client.util.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class RemovePlayerNoteMenuItem extends AbstractMenuItem<PlayerBean> {

  private final PlayerNoteService playerNoteService;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "No player has been set");
    playerNoteService.removeNote(object);
  }

  @Override
  protected boolean isItemVisible() {
    return object != null && playerNoteService.containsNote(object);
  }

  @Override
  protected String getItemText() {
    return "Remove note";
  }
}
