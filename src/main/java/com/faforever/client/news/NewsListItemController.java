package com.faforever.client.news;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.theme.UiService;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NewsListItemController implements Controller<Node> {

  static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

  public Node newsItemRoot;
  public ImageView imageView;
  public Label titleLabel;
  public Label authoredLabel;
  @Inject
  I18n i18n;
  @Inject
  UiService uiService;
  private NewsItem newsItem;
  private OnItemSelectedListener onItemSelectedListener;

  public void initialize() {
    // TODO only use this if there's no thumbnail. However, there's never a thumbnail ATM.
    imageView.setImage(uiService.getThemeImage(UiService.DEFAULT_NEWS_IMAGE));
  }

  @Override
  public Node getRoot() {
    return newsItemRoot;
  }

  public void onMouseClicked() {
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
