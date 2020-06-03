package com.faforever.client.teammatchmaking;

import com.faforever.client.theme.UiService;
import javafx.scene.control.ListCell;

public class MatchmakingQueueItemListCell extends ListCell<MatchmakingQueue> {

  private final MatchmakingQueueItemController controller;

  public MatchmakingQueueItemListCell(UiService uiService) {
    controller = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_queue_card.fxml");
  }

  @Override
  protected void updateItem(MatchmakingQueue queue, boolean empty) {
    super.updateItem(queue, empty);
    if (queue == null || empty) {
      setText(null);
      setGraphic(null);
      return;
    }

    controller.setQueue(queue);
    setGraphic(controller.getRoot());
  }

  @Override
  public void updateSelected(boolean selected) {
    super.updateSelected(selected);
  }
}
