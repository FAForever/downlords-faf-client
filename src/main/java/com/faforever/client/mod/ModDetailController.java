package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.reporting.ReportingService;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebView;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;

import static java.util.Collections.singletonList;

public class ModDetailController {

  @FXML
  Label progressLabel;
  @FXML
  Button uninstallButton;
  @FXML
  Button installButton;
  @FXML
  ImageView modImageView;
  @FXML
  Label nameLabel;
  @FXML
  Label authorLabel;
  @FXML
  ProgressBar progressBar;
  @FXML
  WebView modDescriptionWebView;
  @FXML
  Node modDetailRoot;

  @Autowired
  ModService modService;
  @Autowired
  NotificationService notificationService;
  @Autowired
  I18n i18n;
  @Autowired
  ReportingService reportingService;

  private ModInfoBean mod;

  @FXML
  void initialize() {
    uninstallButton.managedProperty().bind(uninstallButton.visibleProperty());
    installButton.managedProperty().bind(installButton.visibleProperty());
    progressBar.managedProperty().bind(progressBar.visibleProperty());
    progressLabel.managedProperty().bind(progressLabel.visibleProperty());
    progressLabel.visibleProperty().bind(progressBar.visibleProperty());
    progressBar.visibleProperty().bind(uninstallButton.visibleProperty().not().and(installButton.visibleProperty().not()));
  }

  public void setMod(ModInfoBean mod) {
    this.mod = mod;
    if (StringUtils.isNotEmpty(mod.getThumbnailUrl())) {
      modImageView.setImage(new Image(mod.getThumbnailUrl()));
    }
    nameLabel.setText(mod.getName());
    authorLabel.setText(mod.getAuthor());

    boolean modInstalled = modService.isModInstalled(mod.getUid());
    installButton.setVisible(!modInstalled);
    uninstallButton.setVisible(modInstalled);

    modDescriptionWebView.getEngine().loadContent(mod.getDescription());

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
    installButton.setVisible(!installed);
    uninstallButton.setVisible(installed);
  }

  @FXML
  void onInstallButtonClicked() {
    installButton.setVisible(false);

    CompletableFuture<Void> future = modService.downloadAndInstallMod(mod, progressBar.progressProperty(), progressLabel.textProperty());
    future.exceptionally(throwable -> {
      notificationService.addNotification(new ImmediateNotification(
          i18n.get("errorTitle"),
          i18n.get("modVault.installationFailed", mod.getName(), throwable.getLocalizedMessage()),
          Severity.ERROR, throwable, singletonList(new ReportAction(i18n, reportingService, throwable))));
      return null;
    });
  }

  @FXML
  void onUninstallButtonClicked() {
    progressBar.progressProperty().unbind();
    progressBar.setProgress(-1);
    uninstallButton.setVisible(false);

    modService.uninstallMod(mod).exceptionally(throwable -> {
      notificationService.addNotification(new ImmediateNotification(
          i18n.get("errorTitle"),
          i18n.get("modVault.couldNotDeleteMod", mod.getName(), throwable.getLocalizedMessage()),
          Severity.ERROR, throwable, singletonList(new ReportAction(i18n, reportingService, throwable))));
      return null;
    });
  }

  public void onCloseButtonClicked() {
    getRoot().setVisible(false);
  }

  public Node getRoot() {
    return modDetailRoot;
  }
}
