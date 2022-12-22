package com.faforever.client.game;

import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.exception.NotifiableException;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.LastCoopGamePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.ConcurrentUtil;
import com.google.common.base.Strings;
import javafx.scene.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class CreateCoopGameController extends AbstractCreateGameController {

  private CoopMapListController coopMapListController;
  private CoopFeaturedModListController coopFeaturedModListController;

  private final LastCoopGamePrefs lastCoopGame;

  public CreateCoopGameController(ApplicationContext applicationContext, UiService uiService, MapService mapService, ModService modService, GameService gameService, NotificationService notificationService, ContextMenuBuilder contextMenuBuilder, PreferencesService preferencesService, UserService userService, I18n i18n) {
    super(applicationContext, uiService, mapService, modService, gameService, notificationService, contextMenuBuilder, preferencesService, userService, i18n);

    lastCoopGame = preferencesService.getPreferences().getLastCoopGame();
  }

  @Override
  public void initialize() {
    super.initialize();

    setLastTitleAndListen();
    setLastPasswordAndListen();
    setLastMapAndListen();
    setOfflineModeAndListen();
  }

  private void setOfflineModeAndListen() {
    JavaFxUtil.bindBidirectional(offlineModeCheckbox.selectedProperty(), lastCoopGame.offlineModeProperty());
    JavaFxUtil.addListener(offlineModeCheckbox.selectedProperty(), observable -> preferencesService.storeInBackground());
  }

  private void setLastMapAndListen() {
    coopMapListController.selectMission(lastCoopGame.getLastMapFolderName());
    JavaFxUtil.addListener(coopMapListController.selectedMissionProperty(), (observable, oldValue, newValue) -> {
      lastCoopGame.setLastMapFolderName(Strings.nullToEmpty(newValue.getMapFolderName()));
      preferencesService.storeInBackground();
    });
  }

  private void setLastPasswordAndListen() {
    JavaFxUtil.bindBidirectional(passwordTextField.textProperty(), lastCoopGame.lastPasswordProperty());
    JavaFxUtil.addListener(passwordTextField.textProperty(), observable -> preferencesService.storeInBackground());
  }

  private void setLastTitleAndListen() {
    JavaFxUtil.bindBidirectional(titleTextField.textProperty(), lastCoopGame.lastTitleProperty());
    JavaFxUtil.addListener(titleTextField.textProperty(), observable -> preferencesService.storeInBackground());
  }

  @Override
  protected void setUpViews() {
    super.setUpViews();
    ratingContainer.setVisible(false);
  }

  @Override
  public Node getMapListContainer() {
    coopMapListController = uiService.loadFxml("theme/play/coop_map_list.fxml");
    JavaFxUtil.addAndTriggerListener(coopMapListController.selectedMissionProperty(), (observable, oldValue, newValue) -> {
      if (newValue == null) {
        clearMapDetail();
      } else {
        setMapDetail(newValue.getMapFolderName(), null, newValue.getName(), newValue.getDescription(), null, null);
      }
    });
    return coopMapListController.getRoot();
  }

  @Override
  public Node getFeaturedModListContainer() {
    coopFeaturedModListController = uiService.loadFxml("theme/play/featured_mod_list.fxml", CoopFeaturedModListController.class);
    return coopFeaturedModListController.getRoot();
  }

  @Override
  protected void startOfflineGame() {
    mapService.download(coopMapListController.getSelectedMission().getMapFolderName()).thenRun(() -> {
      try {
        gameService.startOfflineGameAndOpenSkirmish(coopMapListController.getSelectedMission().getMapFolderName(), true);
      } catch (IOException e) {
        notificationService.addImmediateErrorNotification(e, "game.create.failed");
      }
    });
  }

  @Override
  protected void startGame() {
    Collection<ModVersionBean> selectedModVersions = modManagerController.getSelectedModVersions();
    modService.updateAndActivateModVersions(selectedModVersions)
        .exceptionally(throwable -> {
          log.error("Error when updating selected mods", throwable);
          notificationService.addImmediateErrorNotification(throwable, "game.create.errorUpdatingMods");
          return selectedModVersions;
        })
        .thenApply(mods -> gameService.hostGame(createGameInfo(modService.getUUIDsFromModVersions(mods)))
            .exceptionally(throwable -> {
              throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
              log.error("Could not host coop game", throwable);
              if (throwable instanceof NotifiableException) {
                notificationService.addErrorNotification((NotifiableException) throwable);
              } else {
                notificationService.addImmediateErrorNotification(throwable, "coop.host.error");
              }
              return null;
            }));
  }

  private NewGameInfo createGameInfo(Set<String> simMods) {
    return new NewGameInfo(
        titleTextField.getText().trim(),
        Strings.emptyToNull(passwordTextField.getText()),
        coopFeaturedModListController.getSelectedFeaturedMod(),
        coopMapListController.getSelectedMission().getMapFolderName(),
        simMods);
  }
}
