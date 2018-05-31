package com.faforever.client.news;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NewsListItemController implements Controller<Node> {

  private final I18n i18n;
  private final UiService uiService;
  public Node newsItemRoot;
  public ImageView imageView;
  public Label titleLabel;
  public Label authoredLabel;

  public NewsListItemController(I18n i18n, UiService uiService) {
    this.i18n = i18n;
    this.uiService = uiService;
  }

  @Override
  public Node getRoot() {
    return newsItemRoot;
  }

  public void setNewsItem(NewsItem newsItem) {
    // TODO only use this if there's no thumbnail. However, there's never a thumbnail ATM.
    imageView.setImage(uiService.getThemeImage(newsItem.getNewsCategory().getImagePath()));

    titleLabel.setText(newsItem.getTitle());
    authoredLabel.setText(i18n.get("news.authoredFormat", newsItem.getAuthor(), newsItem.getDate()));
  }
}
