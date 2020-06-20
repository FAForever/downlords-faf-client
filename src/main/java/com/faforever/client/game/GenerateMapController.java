package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.DualStringListCell;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.GeneratorPrefs;
import com.faforever.client.preferences.PreferenceUpdateListener;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.annotations.VisibleForTesting;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.Function;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class GenerateMapController implements Controller<Pane> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final MapGeneratorService mapGeneratorService;
  public CreateGameController createGameController;
  public Pane generateMapRoot;
  public JFXButton generateMapButton;
  public JFXTextField previousMapName;
  public Spinner<Integer> spawnCountSpinner;
  public CheckBox generateWaterCheckBox;
  @VisibleForTesting
  FilteredList<MapBean> filteredMapBeans;
  private Runnable onCloseButtonClickedListener;
  private PreferenceUpdateListener preferenceUpdateListener;

  /**
   * Remembers if the controller's init method was called, to avoid memory leaks by adding several listeners
   */
  private boolean initialized;

  public void initialize() {
    if (preferencesService.getPreferences().getForgedAlliance().getInstallationPath() == null) {
      preferenceUpdateListener = preferences -> {
        if (!initialized && preferencesService.getPreferences().getForgedAlliance().getInstallationPath() != null) {
          initialized = true;

          Platform.runLater(this::init);
        }
      };
      preferencesService.addUpdateListener(new WeakReference<>(preferenceUpdateListener));
    } else {
      init();
    }
  }

  public void init() {
    initSpawnCountSpinner();
    initGenerateWaterCheckbox();
  }

  private void initSpawnCountSpinner() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGeneratorPrefs();
    int spawnCountProperty = generatorPrefs.getSpawnCountProperty();
    spawnCountSpinner.setValueFactory(new IntegerSpinnerValueFactory(2, 16, spawnCountProperty, 2));
    spawnCountSpinner.getValueFactory().valueProperty().addListener((observable, oldValue, newValue) -> {
      generatorPrefs.setSpawnCountProperty(newValue);
      preferencesService.storeInBackground();
    });
  }

  private void initGenerateWaterCheckbox() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGeneratorPrefs();
    boolean generateWaterProperty = generatorPrefs.getGenerateWaterProperty();
    generateWaterCheckBox.setSelected(generateWaterProperty);
    generateWaterCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
      generatorPrefs.setGenerateWaterProperty(newValue);});
  }

  public void onCloseButtonClicked() {
    onCloseButtonClickedListener.run();
  }

  public void onGenerateMapButtonClicked() {
    try {
      if (!previousMapName.getText().isEmpty()){
        mapGeneratorService.generateMap(previousMapName.getText()).thenAccept(mapName -> {
          Platform.runLater(() -> {
            createGameController.initMapSelection();
            createGameController.mapListView.getItems().stream()
                .filter(mapBean -> mapBean.getFolderName().equalsIgnoreCase(mapName))
                .findAny().ifPresent(mapBean -> {
              createGameController.mapListView.getSelectionModel().select(mapBean);
              createGameController.mapListView.scrollTo(mapBean);
              createGameController.setSelectedMap(mapBean);
            });
          });
        });
      } else {
      byte spawnCount = spawnCountSpinner.getValue().byteValue();
      byte landDensity = generateWaterCheckBox.isSelected() ? (byte) 26 : (byte) 127;
      mapGeneratorService.generateMap(spawnCount, landDensity).thenAccept(mapName -> {
        Platform.runLater(() -> {
          createGameController.initMapSelection();
          createGameController.mapListView.getItems().stream()
              .filter(mapBean -> mapBean.getFolderName().equalsIgnoreCase(mapName))
              .findAny().ifPresent(mapBean -> {
            createGameController.mapListView.getSelectionModel().select(mapBean);
            createGameController.mapListView.scrollTo(mapBean);
            createGameController.setSelectedMap(mapBean);
          });
        });
      });
      }
    } catch (Exception e) {
      notificationService.addImmediateErrorNotification(e, "mapGenerator.generationFailed");
      logger.error("Map generation failed", e);
    }
    onCloseButtonClickedListener.run();
  }

  public void setCreateGameController(CreateGameController controller) {
    createGameController = controller;
  }

  public Pane getRoot() {
    return generateMapRoot;
  }

  void setOnCloseButtonClickedListener(Runnable onCloseButtonClickedListener) {
    this.onCloseButtonClickedListener = onCloseButtonClickedListener;
  }
}
