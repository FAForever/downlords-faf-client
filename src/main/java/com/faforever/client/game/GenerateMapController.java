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
import javafx.scene.control.Slider;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Random;
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
  public Slider waterSlider;
  public CheckBox waterRandom;
  public Slider mountainSlider;
  public CheckBox mountainRandom;
  public Slider plateauSlider;
  public CheckBox plateauRandom;
  public Slider rampSlider;
  public CheckBox rampRandom;
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
    initWaterSlider();
    initPlateauSlider();
    initMountainSlider();
    initRampSlider();
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

  private StringConverter<Double> getLabelConverter(){
    return new StringConverter<Double>() {
      @Override
      public String toString(Double n) {
        if (n < 127) return "None";
        return "Lots";
      }
      @Override
      public Double fromString(String s) {
        if (s.equals("None")){return 0d;}
        return 127d;
      }
    };
  }

  private void initWaterSlider() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGeneratorPrefs();
    double waterDensityProperty = generatorPrefs.getWaterDensityProperty();
    boolean waterRandomProperty = generatorPrefs.getWaterRandomProperty();
    waterSlider.setLabelFormatter(getLabelConverter());
    waterRandom.setSelected(waterRandomProperty);
    waterRandom.selectedProperty().addListener(((observable, oldValue, newValue) -> {generatorPrefs.setWaterRandomProperty(newValue);
    waterSlider.setVisible(!newValue);}));
    waterSlider.setVisible(!waterRandomProperty);
    waterSlider.setValue(waterDensityProperty);
    waterSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {generatorPrefs.setWaterDensityProperty(newValue.intValue());
    preferencesService.storeInBackground();}));
  }

  private void initPlateauSlider() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGeneratorPrefs();
    double plateauDensityProperty = generatorPrefs.getPlateauDensityProperty();
    boolean plateauRandomProperty = generatorPrefs.getPlateauRandomProperty();
    plateauSlider.setLabelFormatter(getLabelConverter());
    plateauRandom.setSelected(plateauRandomProperty);
    plateauRandom.selectedProperty().addListener(((observable, oldValue, newValue) -> {generatorPrefs.setPlateauRandomProperty(newValue);
      plateauSlider.setVisible(!newValue);}));
    plateauSlider.setVisible(!plateauRandomProperty);
    plateauSlider.setValue(plateauDensityProperty);
    plateauSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {generatorPrefs.setPlateauDensityProperty(newValue.intValue());
      preferencesService.storeInBackground();}));
  }

  private void initMountainSlider() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGeneratorPrefs();
    double mountainDensityProperty = generatorPrefs.getMountainDensityProperty();
    boolean mountainRandomProperty = generatorPrefs.getMountainRandomProperty();
    mountainSlider.setLabelFormatter(getLabelConverter());
    mountainRandom.setSelected(mountainRandomProperty);
    mountainRandom.selectedProperty().addListener(((observable, oldValue, newValue) -> {generatorPrefs.setMountainRandomProperty(newValue);
      mountainSlider.setVisible(!newValue);}));
    mountainSlider.setVisible(!mountainRandomProperty);
    mountainSlider.setValue(mountainDensityProperty);
    mountainSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {generatorPrefs.setMountainDensityProperty(newValue.intValue());
      preferencesService.storeInBackground();}));
  }

  private void initRampSlider() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGeneratorPrefs();
    double rampDensityProperty = generatorPrefs.getRampDensityProperty();
    boolean rampRandomProperty = generatorPrefs.getRampRandomProperty();
    rampSlider.setLabelFormatter(getLabelConverter());
    rampRandom.setSelected(rampRandomProperty);
    rampRandom.selectedProperty().addListener(((observable, oldValue, newValue) -> {generatorPrefs.setRampRandomProperty(newValue);
      rampSlider.setVisible(!newValue);}));
    rampSlider.setVisible(!rampRandomProperty);
    rampSlider.setValue(rampDensityProperty);
    rampSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {generatorPrefs.setRampDensityProperty(newValue.intValue());
      preferencesService.storeInBackground();}));
  }

  private byte getSliderValue(Slider slider, CheckBox checkBox) {
    if (checkBox.isSelected()){
      return (byte) new Random().nextInt(127);
    }
    return (byte) slider.getValue();
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
        byte landDensity = (byte) (127 - getSliderValue(waterSlider, waterRandom));
        byte plateauDensity = getSliderValue(plateauSlider, plateauRandom);
        byte mountainDensity = getSliderValue(mountainSlider, mountainRandom);
        byte rampDensity = getSliderValue(rampSlider, rampRandom);
        byte[] optionArray = {spawnCount, landDensity, plateauDensity, mountainDensity, rampDensity};
        mapGeneratorService.generateMap(optionArray).thenAccept(mapName -> {
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
