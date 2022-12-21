package com.faforever.client.game;

import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.exception.NotifiableException;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.LastGamePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.commons.lobby.GameVisibility;
import com.google.common.base.Strings;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class CreateCustomGameController extends AbstractCreateGameController {

  private CustomMapListController customMapListController;
  private CustomFeaturedModListController customFeaturedModListController;

  public CreateCustomGameController(ApplicationContext applicationContext, UiService uiService, MapService mapService, ModService modService, GameService gameService, NotificationService notificationService, ContextMenuBuilder contextMenuBuilder, PreferencesService preferencesService, UserService userService, I18n i18n) {
    super(applicationContext, uiService, mapService, modService, gameService, notificationService, contextMenuBuilder, preferencesService, userService, i18n);
  }

  @Override
  public void initialize() {
    super.initialize();

    setLastGameTitleAndListen();
    setLastGamePasswordAndListen();
    setRatingsAndListen();
  }

  private void setLastGamePasswordAndListen() {
    LastGamePrefs lastGame = preferencesService.getPreferences().getLastGame();
    passwordTextField.setText(lastGame.getLastGamePassword());
    JavaFxUtil.addListener(passwordTextField.textProperty(), (observable, oldValue, newValue) -> {
      lastGame.setLastGamePassword(newValue);
      preferencesService.storeInBackground();
    });
  }

  private void setLastGameTitleAndListen() {
    LastGamePrefs lastGame = preferencesService.getPreferences().getLastGame();
    titleTextField.setText(Strings.nullToEmpty(lastGame.getLastGameTitle()));
    JavaFxUtil.addListener(titleTextField.textProperty(), (observable, oldValue, newValue) -> {
      lastGame.setLastGameTitle(newValue);
      preferencesService.storeInBackground();
    });
  }

  private void setRatingsAndListen() {
    LastGamePrefs lastGame = preferencesService.getPreferences().getLastGame();

    Integer lastGameMinRating = lastGame.getLastGameMinRating();
    Integer lastGameMaxRating = lastGame.getLastGameMaxRating();
    minRankingTextField.setText(lastGameMinRating != null ? i18n.number(lastGameMinRating) : "");
    maxRankingTextField.setText(lastGameMaxRating != null ? i18n.number(lastGameMaxRating) : "");

    JavaFxUtil.addListener(minRankingTextField.textProperty(), (observable, oldValue, newValue) -> {
      lastGame.setLastGameMinRating(newValue.isEmpty() ? null : Integer.parseInt(newValue));
      preferencesService.storeInBackground();
    });

    JavaFxUtil.addListener(minRankingTextField.textProperty(), (observable, oldValue, newValue) -> {
      lastGame.setLastGameMaxRating(newValue.isEmpty() ? null : Integer.parseInt(newValue));
      preferencesService.storeInBackground();
    });

    JavaFxUtil.bindBidirectional(enforceRankingCheckBox.selectedProperty(), lastGame.lastGameEnforceRatingProperty());
    JavaFxUtil.addListener(enforceRankingCheckBox.selectedProperty(), observable -> preferencesService.storeInBackground());
  }

  @Override
  public Node getMapListContainer() {
    customMapListController = uiService.loadFxml("theme/play/feature/custom_map_list.fxml");
    JavaFxUtil.addAndTriggerListener(customMapListController.selectedMapProperty(), (observable, oldValue, newValue) -> {
      if (newValue == null) {
        clearMapDetail();
      } else {
        preferencesService.getPreferences().getLastGame().setLastMap(newValue.getFolderName());
        preferencesService.storeInBackground();
        setMapDetail(newValue.getFolderName(), newValue.getSize(), newValue.getMap().getDisplayName(), newValue.getDescription(),
            newValue.getMaxPlayers(), Optional.ofNullable(newValue.getVersion()).map(ComparableVersion::toString).orElse(null));
      }
    });
    return customMapListController.getRoot();
  }

  @Override
  public Node getFeaturedModListContainer() {
    customFeaturedModListController = uiService.loadFxml("theme/play/feature/featured_mod_list.fxml", CustomFeaturedModListController.class);
    JavaFxUtil.addAndTriggerListener(customFeaturedModListController.selectedFeaturedModProperty(), (observable, oldValue, newValue) ->
        setWarning("game.create.featuredModMissing", newValue == null));
    return customFeaturedModListController.getRoot();
  }

  @Override
  protected void startOfflineGame() {
    try {
      gameService.startOfflineGameAndOpenSkirmish(customMapListController.getSelectedMap().getFolderName(), false);
    } catch (IOException e) {
      notificationService.addImmediateErrorNotification(e, "game.create.failed");
    }
  }

  @Override
  protected void startGame() {
    MapVersionBean selectedMap = customMapListController.getSelectedMap();
    Collection<ModVersionBean> selectedModVersions = modManagerController.getSelectedModVersions();
    mapService.updateLatestVersionIfNecessary(selectedMap)
        .exceptionally(throwable -> {
          log.error("Error when updating the map", throwable);
          return selectedMap;
        })
        .thenCombine(modService.updateAndActivateModVersions(selectedModVersions)
            .exceptionally(throwable -> {
              log.error("Error when updating selected mods", throwable);
              notificationService.addImmediateErrorNotification(throwable, "game.create.errorUpdatingMods");
              return selectedModVersions;
            }), (mapBean, mods) -> {
          hostGame(createGameInfo(mapBean.getFolderName(), modService.getUUIDsFromModVersions(mods)));
          return null;
        }).exceptionally(throwable -> {
          throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
          log.error("Game could not be hosted", throwable);
          if (throwable instanceof NotifiableException) {
            notificationService.addErrorNotification((NotifiableException) throwable);
          } else {
            notificationService.addImmediateErrorNotification(throwable, "game.create.failed");
          }
          return null;
        });
  }

  private void hostGame(NewGameInfo newGameInfo) {
    gameService.hostGame(newGameInfo).exceptionally(throwable -> {
      throwable  = ConcurrentUtil.unwrapIfCompletionException(throwable);
      log.error("Game could not be hosted", throwable);
      if (throwable instanceof NotifiableException) {
        notificationService.addErrorNotification((NotifiableException) throwable);
      } else {
        notificationService.addImmediateErrorNotification(throwable, "game.create.failed");
      }
      return null;
    });
  }

  private NewGameInfo createGameInfo(String mapFolderName, Set<String> simMods) {
    return new NewGameInfo(
        titleTextField.getText().trim(),
        Strings.emptyToNull(passwordTextField.getText()),
        customFeaturedModListController.getSelectedFeaturedMod(),
        mapFolderName,
        simMods,
        onlyForFriendsCheckBox.isSelected() ? GameVisibility.PRIVATE : GameVisibility.PUBLIC,
        getMinRating(),
        getMaxRating(),
        enforceRankingCheckBox.isSelected());
  }

  public void selectMap(@Nullable String mapFolderName) {
    customMapListController.selectMap(mapFolderName);
  }

  public void setOnStackPaneRequest(Supplier<StackPane> onStackPaneRequest) {
    customMapListController.setOnStackPaneRequest(onStackPaneRequest);
  }
}
