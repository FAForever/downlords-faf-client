package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.util.ThemeUtil;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Consumer;

public class ModTileController {

  @FXML
  Pane progressLayout;
  @FXML
  Label progressLabel;
  @FXML
  Label commentsLabel;
  @FXML
  ImageView thumbnailImageView;
  @FXML
  Label nameLabel;
  @FXML
  Label authorLabel;
  @FXML
  Label likesLabel;
  @FXML
  Node modTileRoot;
  @FXML
  ProgressBar progressBar;

  @Autowired
  ModService modService;
  @Autowired
  NotificationService notificationService;
  @Autowired
  I18n i18n;
  @Autowired
  ReportingService reportingService;
  @Autowired
  PreferencesService preferencesService;

  private ModInfoBean mod;
  private Consumer<ModInfoBean> onOpenDetailListener;

  @FXML
  void initialize() {
    progressLabel.visibleProperty().bind(progressBar.visibleProperty());
  }

  public void setMod(ModInfoBean mod) {
    this.mod = mod;
    Image image;
    if (StringUtils.isNotEmpty(mod.getThumbnailUrl())) {
      image = new Image(mod.getThumbnailUrl());
    } else {
      image = new Image(ThemeUtil.themeFile(preferencesService.getPreferences().getTheme(), "images/unknown_mod.png"), true);
    }
    thumbnailImageView.setImage(image);
    nameLabel.setText(mod.getName());
    authorLabel.setText(mod.getAuthor());
    likesLabel.setText(String.format("%d", mod.getLikes()));
    commentsLabel.setText(String.format("%d", mod.getComments().size()));

    progressBar.setVisible(false);

    modService.getInstalledMods().addListener((ListChangeListener<ModInfoBean>) change -> {
      while (change.next()) {
        for (ModInfoBean modInfoBean : change.getAddedSubList()) {
          if (mod.getUid().equals(modInfoBean.getUid())) {
            setInstalled(true);
            return;
          }
        }
        for (ModInfoBean modInfoBean : change.getRemoved()) {
          if (mod.getUid().equals(modInfoBean.getUid())) {
            setInstalled(false);
            return;
          }
        }
      }
    });
  }

  private void setInstalled(boolean installed) {
    progressBar.setVisible(false);
  }

  public Node getRoot() {
    return modTileRoot;
  }

  public void setOnOpenDetailListener(Consumer<ModInfoBean> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  @FXML
  void onShowModDetail() {
    onOpenDetailListener.accept(mod);
  }
}
