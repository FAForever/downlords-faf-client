package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.player.PlayerService;
import com.faforever.client.util.Assert;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class EditPlayerNoteMenuItem extends AbstractMenuItem<PlayerBean> {

  private final PlayerService playerService;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "No player has been set");
    playerService.updateNote(object, RandomStringUtils.randomAlphabetic(10));
  }

  @Override
  protected boolean isItemVisible() {
    return object != null && playerService.containsNote(object);
  }

  @Override
  protected String getItemText() {
    return "Edit note";
  }
}
