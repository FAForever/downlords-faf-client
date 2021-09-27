package com.faforever.client.mod;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.event.ModUploadedEvent;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.CopyErrorAction;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.GetHelpAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.commons.api.dto.ApiException;
import com.google.common.eventbus.EventBus;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import static com.faforever.client.notification.Severity.ERROR;
import static java.util.Arrays.asList;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class ModUploadController implements Controller<Node> {

  private final ModService modService;
  private final ExecutorService executorService;
  private final NotificationService notificationService;
  private final ReportingService reportingService;
  private final I18n i18n;
  private final PlatformService platformService;
  private final ClientProperties clientProperties;
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
  public CheckBox rulesCheckBox;
  public Label rulesLabel;
  private Path modPath;
  private CompletableTask<Void> modUploadTask;
  private ModVersionBean modVersionInfo;
  private Runnable cancelButtonClickedListener;

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
    CompletableFuture.supplyAsync(() -> {
      try {
        return modService.extractModInfo(modPath);
      } catch (ModLoadException e) {
        throw new CompletionException(e);
      }
    }, executorService)
        .thenAccept(this::setModVersionInfo)
        .exceptionally(throwable -> {
          throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
          log.warn("ModVersion could not be read", throwable);
          notificationService.addImmediateErrorNotification(throwable, "modVault.upload.readError");
          return null;
        });
  }

  private void enterParsingState() {
    modInfoPane.setVisible(false);
    uploadProgressPane.setVisible(false);
    parseProgressPane.setVisible(true);
    uploadCompletePane.setVisible(false);
  }

  private void setModVersionInfo(ModVersionBean modVersion) {
    this.modVersionInfo = modVersion;
    JavaFxUtil.runLater(() -> {
      enterModInfoState();
      modNameLabel.textProperty().bind(modVersion.getMod().displayNameProperty());
      descriptionLabel.textProperty().bind(modVersion.descriptionProperty());
      versionLabel.textProperty().bind(modVersion.versionProperty().asString());
      uidLabel.textProperty().bind(modVersion.uidProperty());
      thumbnailImageView.imageProperty().bind(
          Bindings.createObjectBinding(() -> modService.loadThumbnail(modVersion), modVersion.idProperty(), modVersion.imagePathProperty())
      );
    });
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
      notificationService.addServerNotification(new ImmediateNotification(
          i18n.get("errorTitle"), i18n.get("modVault.upload.failed", throwable.getLocalizedMessage()), ERROR,
          asList(
              new GetHelpAction(i18n, reportingService),
              new DismissAction(i18n)
          )
      ));
    } else {
      notificationService.addServerNotification(new ImmediateNotification(
          i18n.get("errorTitle"), i18n.get("modVault.upload.failed", throwable.getLocalizedMessage()), ERROR, throwable,
          asList(
              new Action(i18n.get("modVault.upload.retry"), event -> onUploadClicked()),
              new CopyErrorAction(i18n, reportingService, throwable),
              new GetHelpAction(i18n, reportingService),
              new DismissAction(i18n)
          )
      ));
    }
  }

  public void onUploadClicked() {
    if (!rulesCheckBox.isSelected()) {
      rulesLabel.getStyleClass().add("bad");
      return;
    }
    enterUploadingState();

    uploadProgressPane.setVisible(true);
    modUploadTask = modService.uploadMod(modPath);
    uploadTaskTitleLabel.textProperty().bind(modUploadTask.titleProperty());
    uploadTaskMessageLabel.textProperty().bind(modUploadTask.messageProperty());
    uploadProgressBar.progressProperty().bind(modUploadTask.progressProperty());

    modUploadTask.getFuture()
        .thenAccept(v -> eventBus.post(new ModUploadedEvent(modVersionInfo)))
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

  public void onShowRulesClicked() {
    platformService.showDocument(clientProperties.getVault().getModRulesUrl());
  }

  public void onCancelClicked() {
    cancelButtonClickedListener.run();
  }

  public Region getRoot() {
    return modUploadRoot;
  }

  public void setOnCancelButtonClickedListener(Runnable cancelButtonClickedListener) {
    this.cancelButtonClickedListener = cancelButtonClickedListener;
  }
}
