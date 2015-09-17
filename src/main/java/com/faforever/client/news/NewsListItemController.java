package com.faforever.client.news;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.ThemeUtil;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

public class NewsListItemController {

  public interface OnItemSelectedListener {

    void onSelected(NewsItem newsItem);
  }


  @FXML
  Pane newsItemRoot;

  @FXML
  ImageView imageView;

  @FXML
  Label titleLabel;

  @FXML
  Label authoredLabel;

  @Autowired
  I18n i18n;

  @Autowired
  PreferencesService preferencesService;

  private NewsItem newsItem;
  private OnItemSelectedListener onItemSelectedListener;

  @PostConstruct
  void postConstruct() {
    String theme = preferencesService.getPreferences().getTheme();

    // TODO only use this if there's no thumbnail. However, there's never a thumbnail ATM.
    imageView.setImage(new Image(ThemeUtil.themeFile(theme, "images/news_fallback.jpg")));
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
}
