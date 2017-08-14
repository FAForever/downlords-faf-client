package com.faforever.client.mod;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.reporting.ReportingService;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static java.util.Collections.singletonList;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ModDetailController implements Controller<Node> {

  private final ModService modService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final ReportingService reportingService;

  public Label progressLabel;
  public Button uninstallButton;
  public Button installButton;
  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Label authorLabel;
  public ProgressBar progressBar;
  public Label modDescriptionLabel;
  public Node modDetailRoot;
  private Mod mod;
  private ListChangeListener<Mod> installStatusChangeListener;

  @Inject
  public ModDetailController(ModService modService, NotificationService notificationService, I18n i18n, ReportingService reportingService) {
    this.modService = modService;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.reportingService = reportingService;
  }

  public void initialize() {
    uninstallButton.managedProperty().bind(uninstallButton.visibleProperty());
    installButton.managedProperty().bind(installButton.visibleProperty());
    progressBar.managedProperty().bind(progressBar.visibleProperty());
    progressBar.visibleProperty().bind(uninstallButton.visibleProperty().not().and(installButton.visibleProperty().not()));
    progressLabel.managedProperty().bind(progressLabel.visibleProperty());
    progressLabel.visibleProperty().bind(progressBar.visibleProperty());

    modDetailRoot.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ESCAPE) {
        onCloseButtonClicked();
      }
    });

    installStatusChangeListener = change -> {
      while (change.next()) {
        for (Mod mod : change.getAddedSubList()) {
          if (this.mod.getId().equals(mod.getId())) {
            setInstalled(true);
            return;
          }
        }
        for (Mod mod : change.getRemoved()) {
          if (this.mod.getId().equals(mod.getId())) {
            setInstalled(false);
            return;
          }
        }
      }
    };
  }

  public void onCloseButtonClicked() {
    ((Pane) modDetailRoot.getParent()).getChildren().remove(modDetailRoot);
  }

  private void setInstalled(boolean installed) {
    installButton.setVisible(!installed);
    uninstallButton.setVisible(installed);
  }

  public Node getRoot() {
    return modDetailRoot;
  }

  public void setMod(Mod mod) {
    this.mod = mod;
    thumbnailImageView.setImage(modService.loadThumbnail(mod));
    nameLabel.setText(mod.getName());
    authorLabel.setText(mod.getAuthor());

    boolean modInstalled = modService.isModInstalled(mod.getId());
    installButton.setVisible(!modInstalled);
    uninstallButton.setVisible(modInstalled);

    modDescriptionLabel.textProperty().bind(mod.descriptionProperty());
    modService.getInstalledMods().addListener(new WeakListChangeListener<>(installStatusChangeListener));
    setInstalled(modService.isModInstalled(mod.getId()));
  }

  public void onInstallButtonClicked() {
    installButton.setVisible(false);

    modService.downloadAndInstallMod(mod, progressBar.progressProperty(), progressLabel.textProperty())
        .exceptionally(throwable -> {
          notificationService.addNotification(new ImmediateNotification(
              i18n.get("errorTitle"),
              i18n.get("modVault.installationFailed", mod.getName(), throwable.getLocalizedMessage()),
              Severity.ERROR, throwable, singletonList(new ReportAction(i18n, reportingService, throwable))));
          return null;
        });
  }

  public void onUninstallButtonClicked() {
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

  public void onDimmerClicked() {
    onCloseButtonClicked();
  }

  public void onContentPaneClicked(MouseEvent event) {
    event.consume();
  }
}
