package com.faforever.client.news;

import com.faforever.client.theme.UiService;
import javafx.scene.control.ListCell;

public class NewsItemListCell extends ListCell<NewsItem> {

  private final NewsListItemController controller;

  public NewsItemListCell(UiService uiService) {
    controller = uiService.loadFxml("theme/news_list_item.fxml");
  }

  @Override
  protected void updateItem(NewsItem item, boolean empty) {
    super.updateItem(item, empty);
    if (item == null || empty) {
      setText(null);
      setGraphic(null);
      return;
    }

    controller.setNewsItem(item);
    setGraphic(controller.getRoot());
  }
}
