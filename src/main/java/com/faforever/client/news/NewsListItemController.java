package com.faforever.client.news;

import com.faforever.client.i18n.I18n;
import com.faforever.client.theme.ThemeService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

public class NewsListItemController {

  @FXML
  Pane newsItemRoot;
  @FXML
  ImageView imageView;
  @FXML
  Label titleLabel;
  @FXML
  Label authoredLabel;
  @Resource
  I18n i18n;
  @Resource
  ThemeService themeService;
  private NewsItem newsItem;
  private OnItemSelectedListener onItemSelectedListener;

  @PostConstruct
  void postConstruct() {
    // TODO only use this if there's no thumbnail. However, there's never a thumbnail ATM.
    imageView.setImage(themeService.getThemeImage(ThemeService.DEFAULT_NEWS_IMAGE));
  }

  public Node getRoot() {
    return newsItemRoot;
  }

  @FXML
  void onMouseClicked() {
    onItemSelectedListener.onSelected(newsItem);
  }

  public void setNewsItem(NewsItem newsItem) {
    this.newsItem = newsItem;

    titleLabel.setText(newsItem.getTitle());
    authoredLabel.setText(i18n.get("news.authoredFormat", newsItem.getAuthor(), newsItem.getDate()));
  }

  public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener) {
    this.onItemSelectedListener = onItemSelectedListener;
  }

  public interface OnItemSelectedListener {

    void onSelected(NewsItem newsItem);
  }
}
