package com.faforever.client.hub;

import com.faforever.client.ThemeService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.news.NewsItem;
import com.faforever.client.news.NewsService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.jsoup.Jsoup;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

public class LastNewsController {

  @FXML
  Label textLabel;
  @FXML
  Label titleLabel;
  @FXML
  Label authoredLabel;
  @FXML
  Node lastNewsRoot;
  @FXML
  ImageView imageView;

  @Resource
  NewsService newsService;
  @Resource
  I18n i18n;
  @Resource
  ThemeService themeService;

  public Node getRoot() {
    return lastNewsRoot;
  }

  @PostConstruct
  void postConstruct() {
    List<NewsItem> newsItems = newsService.fetchNews();
    if (!newsItems.isEmpty()) {
      NewsItem newsItem = newsItems.get(0);
      authoredLabel.setText(i18n.get("news.authoredFormat", newsItem.getAuthor(), newsItem.getDate()));

      titleLabel.setText(newsItem.getTitle());

      String text = Jsoup.parse(newsItem.getContent()).text();
      textLabel.setText(text);
    }

    // TODO only use this if there's no thumbnail. However, there's never a thumbnail ATM.
    imageView.setImage(new Image(themeService.getThemeFile(ThemeService.DEFAULT_NEWS_IMAGE)));
  }
}
