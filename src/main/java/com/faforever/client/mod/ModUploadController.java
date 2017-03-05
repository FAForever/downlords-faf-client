package com.faforever.client.mod;

import com.faforever.client.api.dto.ApiException;
import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.event.ModUploadedEvent;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.util.IdenticonUtil;
import com.google.common.eventbus.EventBus;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static com.faforever.client.notification.Severity.ERROR;
import static java.util.Arrays.asList;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ModUploadController  implements Controller<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final ModService modService;
  private final ThreadPoolExecutor threadPoolExecutor;
  private final NotificationService notificationService;
  private final ReportingService reportingService;
  private final I18n i18n;
  private final EventBus eventBus;
  public Label uploadTaskMessageLabel;
  public Label uploadTaskTitleLabel;
  public Pane parseProgressPane;
  public Pane uploadProgressPane;
  public Pane uploadCompletePane;
  public ProgressBar uploadProgressBar;
  public Pane modInfoPane;
  public Label modNameLabel;
  public Label descriptionLabel;
  public Label versionLabel;
  public Label uidLabel;
  public ImageView thumbnailImageView;
  public Region modUploadRoot;
  private Path modPath;
  private CompletableTask<Void> modUploadTask;
  private Mod modInfo;

  @Inject
  public ModUploadController(ModService modService, ThreadPoolExecutor threadPoolExecutor, NotificationService notificationService, ReportingService reportingService, I18n i18n, EventBus eventBus) {
    this.modService = modService;
    this.threadPoolExecutor = threadPoolExecutor;
    this.notificationService = notificationService;
    this.reportingService = reportingService;
    this.i18n = i18n;
    this.eventBus = eventBus;
  }

  public void initialize() {
    modInfoPane.managedProperty().bind(modInfoPane.visibleProperty());
    uploadProgressPane.managedProperty().bind(uploadProgressPane.visibleProperty());
    parseProgressPane.managedProperty().bind(parseProgressPane.visibleProperty());
    uploadCompletePane.managedProperty().bind(uploadCompletePane.visibleProperty());

    modInfoPane.setVisible(false);
    uploadProgressPane.setVisible(false);
    parseProgressPane.setVisible(false);
    uploadCompletePane.setVisible(false);
  }

  public void setModPath(Path modPath) {
    this.modPath = modPath;
    enterParsingState();
    CompletableFuture.supplyAsync(() -> modService.extractModInfo(modPath), threadPoolExecutor)
        .thenAccept(this::setModInfo)
        .exceptionally(throwable -> {
          logger.warn("Mod could not be read", throwable);
          return null;
        });
  }

  private void enterParsingState() {
    modInfoPane.setVisible(false);
    uploadProgressPane.setVisible(false);
    parseProgressPane.setVisible(true);
    uploadCompletePane.setVisible(false);
  }

  private void setModInfo(Mod modInfo) {
    this.modInfo = modInfo;

    enterModInfoState();
    modNameLabel.textProperty().bind(modInfo.nameProperty());
    descriptionLabel.textProperty().bind(modInfo.descriptionProperty());
    versionLabel.textProperty().bind(modInfo.versionProperty().asString());
    uidLabel.textProperty().bind(modInfo.idProperty());
    thumbnailImageView.imageProperty().bind(
        Bindings.createObjectBinding(() -> {
          if (modInfo.getImagePath() != null && Files.isRegularFile(modInfo.getImagePath())) {
            return new Image(modInfo.getImagePath().toUri().toString(), true);
          }

          return IdenticonUtil.createIdenticon(modInfo.getId());
        }, modInfo.idProperty(), modInfo.imagePathProperty())
    );
  }

  private void enterModInfoState() {
    modInfoPane.setVisible(true);
    uploadProgressPane.setVisible(false);
    parseProgressPane.setVisible(false);
    uploadCompletePane.setVisible(false);
  }

  public void onCancelUploadClicked() {
    modUploadTask.cancel(true);
    enterModInfoState();
  }

  private void onUploadFailed(Throwable throwable) {
    enterModInfoState();
    if (throwable instanceof ApiException) {
      notificationService.addNotification(new ImmediateNotification(
          i18n.get("errorTitle"), i18n.get("modVault.upload.failed", throwable.getLocalizedMessage()), ERROR,
          asList(
              new Action(i18n.get("modVault.upload.retry"), event -> onUploadClicked()),
              new DismissAction(i18n)
          )
      ));
    } else {
      notificationService.addNotification(new ImmediateNotification(
          i18n.get("errorTitle"), i18n.get("modVault.upload.failed", throwable.getLocalizedMessage()), ERROR, throwable,
          asList(
              new Action(i18n.get("modVault.upload.retry"), event -> onUploadClicked()),
              new ReportAction(i18n, reportingService, throwable),
              new DismissAction(i18n)
          )
      ));
    }
  }

  public void onUploadClicked() {
    enterUploadingState();

    uploadProgressPane.setVisible(true);
    modUploadTask = modService.uploadMod(modPath);
    uploadTaskTitleLabel.textProperty().bind(modUploadTask.titleProperty());
    uploadTaskMessageLabel.textProperty().bind(modUploadTask.messageProperty());
    uploadProgressBar.progressProperty().bind(modUploadTask.progressProperty());

    modUploadTask.getFuture()
        .thenAccept(v -> eventBus.post(new ModUploadedEvent(modInfo)))
        .thenAccept(aVoid -> enterUploadCompleteState())
        .exceptionally(throwable -> {
          if (!(throwable instanceof CancellationException)) {
            onUploadFailed(throwable.getCause());
          }
          return null;
        });
  }

  private void enterUploadingState() {
    modInfoPane.setVisible(false);
    uploadProgressPane.setVisible(true);
    parseProgressPane.setVisible(false);
    uploadCompletePane.setVisible(false);
  }

  private void enterUploadCompleteState() {
    modInfoPane.setVisible(false);
    uploadProgressPane.setVisible(false);
    parseProgressPane.setVisible(false);
    uploadCompletePane.setVisible(true);
  }

  public void onCancelClicked() {
    getRoot().getScene().getWindow().hide();
  }

  public Region getRoot() {
    return modUploadRoot;
  }
}
