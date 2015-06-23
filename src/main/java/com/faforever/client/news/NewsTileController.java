package com.faforever.client.news;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.ThemeUtil;
import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Date;

public class NewsTileController {

  @FXML
  Pane newsTileRoot;

  @FXML
  ImageView imageView;

  @FXML
  Label titleLabel;

  @FXML
  Label descriptionLabel;

  @FXML
  Label authoredLabel;

  @Autowired
  I18n i18n;

  @Autowired
  HostServices hostServices;

  @Autowired
  PreferencesService preferencesService;

  private String author;
  private String link;

  @PostConstruct
  void postConstruct() {
    String theme = preferencesService.getPreferences().getTheme();

    // TODO only use this if there's no thumbnail. However, there's never a thumbnail ATM.
    imageView.setImage(new Image(ThemeUtil.themeFile(theme, "images/news_fallback.jpg")));
  }

  public void setAuthored(String author, Date date) {
    authoredLabel.setText(i18n.get("news.authoredFormat", author, date));
  }

  public void setLink(String link) {
    this.link = link;
  }

  public void setTitle(String title) {
    titleLabel.setText(title);
  }

  public void setDescription(String description) {
    descriptionLabel.setText(description);
  }

  public Node getRoot() {
    return newsTileRoot;
  }

  @FXML
  void onMouseClicked(MouseEvent event) {
    hostServices.showDocument(link);
  }
}
