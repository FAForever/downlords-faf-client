package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.player.PlayerNoteController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.dialog.Dialog;
import com.faforever.client.util.Assert;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class EditPlayerNoteMenuItem extends AbstractMenuItem<PlayerBean> {

  private final UiService uiService;
  private final PlayerService playerService;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "No player has been set");
    PlayerNoteController playerNoteController = uiService.loadFxml("theme/player_note.fxml");
    Dialog dialog = uiService.showInDialog((StackPane) getParentPopup().getOwnerWindow().getScene().getRoot(), playerNoteController.getRoot());
    playerNoteController.setPlayer(object);
    playerNoteController.setCloseButtonAction(event -> dialog.close());
    dialog.setOnDialogOpened(event -> playerNoteController.requestFocus());
    dialog.show();
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
