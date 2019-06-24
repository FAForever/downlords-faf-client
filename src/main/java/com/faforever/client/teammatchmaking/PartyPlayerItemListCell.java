package com.faforever.client.teammatchmaking;

import com.faforever.client.theme.UiService;
import javafx.scene.control.ListCell;

public class PartyPlayerItemListCell extends ListCell<PartyPlayerItem> {

  private final PartyPlayerItemController controller;

  public PartyPlayerItemListCell(UiService uiService) {
    controller = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_player_card.fxml");
  }

  @Override
  protected void updateItem(PartyPlayerItem item, boolean empty) {
    super.updateItem(item, empty);
    if (item == null || empty) {
      setText(null);
      setGraphic(null);
      return;
    }

    controller.setPlayerItem(item);
    setGraphic(controller.getRoot());
  }

  @Override
  public void updateSelected(boolean selected) {
    super.updateSelected(selected);
  }
}
