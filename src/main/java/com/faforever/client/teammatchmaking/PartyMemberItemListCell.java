package com.faforever.client.teammatchmaking;

import com.faforever.client.teammatchmaking.Party.PartyMember;
import com.faforever.client.theme.UiService;
import javafx.scene.control.ListCell;

public class PartyMemberItemListCell extends ListCell<PartyMember> {

  private final PartyMemberItemController controller;

  public PartyMemberItemListCell(UiService uiService) {
    controller = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_member_card.fxml");
  }

  @Override
  protected void updateItem(PartyMember member, boolean empty) {
    super.updateItem(member, empty);
    if (member == null || empty) {
      setText(null);
      setGraphic(null);
      return;
    }

    controller.setMember(member);
    setGraphic(controller.getRoot());
  }

  @Override
  public void updateSelected(boolean selected) {
    super.updateSelected(selected);
  }
}
