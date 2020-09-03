package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.map.generator.OutdatedVersionException;
import com.faforever.client.map.generator.UnsupportedVersionException;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.GeneratorPrefs;
import com.faforever.client.preferences.PreferencesService;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.ListSpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.security.InvalidParameterException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class GenerateMapController implements Controller<Pane> {

  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final MapGeneratorService mapGeneratorService;
  public CreateGameController createGameController;
  public Pane generateMapRoot;
  public Button generateMapButton;
  public TextField previousMapName;
  public Spinner<Integer> spawnCountSpinner;
  public Spinner<String> mapSizeSpinner;
  public Slider waterSlider;
  public CheckBox waterRandom;
  public HBox waterSliderBox;
  public HBox waterRandomBox;
  public Slider mountainSlider;
  public CheckBox mountainRandom;
  public HBox mountainSliderBox;
  public HBox mountainRandomBox;
  public Slider plateauSlider;
  public CheckBox plateauRandom;
  public HBox plateauSliderBox;
  public HBox plateauRandomBox;
  public Slider rampSlider;
  public CheckBox rampRandom;
  public HBox rampSliderBox;
  public HBox rampRandomBox;
  private Runnable onCloseButtonClickedListener;
  private ObservableList<String> validMapSizes = FXCollections.observableArrayList("5km", "10km", "20km");
  private int[] mapValues = new int[]{256, 512, 1024};

  public void initialize() {
    initSpawnCountSpinner();
    initMapSizeSpinner();
    initWaterSlider();
    initPlateauSlider();
    initMountainSlider();
    initRampSlider();
  }

  private void initSpawnCountSpinner() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGeneratorPrefs();
    int spawnCountProperty = generatorPrefs.getSpawnCountProperty();
    spawnCountSpinner.setValueFactory(new IntegerSpinnerValueFactory(2, 16, spawnCountProperty, 2));
    generatorPrefs.spawnCountPropertyProperty().bind(spawnCountSpinner.getValueFactory().valueProperty());
    spawnCountSpinner.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty()));
  }

  private void initMapSizeSpinner() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGeneratorPrefs();
    String mapSizeProperty = generatorPrefs.getMapSizeProperty();
    mapSizeSpinner.setValueFactory(new ListSpinnerValueFactory<>(validMapSizes));
    mapSizeSpinner.increment(validMapSizes.indexOf(mapSizeProperty));
    generatorPrefs.mapSizePropertyProperty().bind(mapSizeSpinner.getValueFactory().valueProperty());
    mapSizeSpinner.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty()));
  }

  private StringConverter<Double> getLabelConverter() {
    return new StringConverter<>() {
      @Override
      public String toString(Double n) {
        if (n < 127) {
          return "%game.generate.none";
        }
        return "%game.generate.lots";
      }

      @Override
      public Double fromString(String s) {
        throw new UnsupportedOperationException();
      }
    };
  }

  private void initWaterSlider() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGeneratorPrefs();
    double waterDensityProperty = generatorPrefs.getWaterDensityProperty();
    boolean waterRandomProperty = generatorPrefs.getWaterRandomProperty();
    waterSlider.setLabelFormatter(getLabelConverter());
    waterRandom.setSelected(waterRandomProperty);
    waterSlider.setValue(waterDensityProperty);
    waterSliderBox.visibleProperty().bind(waterRandom.selectedProperty().not());
    generatorPrefs.waterDensityPropertyProperty().bind(waterSlider.valueProperty());
    generatorPrefs.waterRandomPropertyProperty().bind(waterRandom.selectedProperty());
    waterSliderBox.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty()));
    waterRandomBox.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty()));
  }

  private void initPlateauSlider() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGeneratorPrefs();
    double plateauDensityProperty = generatorPrefs.getPlateauDensityProperty();
    boolean plateauRandomProperty = generatorPrefs.getPlateauRandomProperty();
    plateauSlider.setLabelFormatter(getLabelConverter());
    plateauRandom.setSelected(plateauRandomProperty);
    plateauSlider.setValue(plateauDensityProperty);
    plateauSliderBox.visibleProperty().bind(plateauRandom.selectedProperty().not());
    generatorPrefs.plateauDensityPropertyProperty().bind(plateauSlider.valueProperty());
    generatorPrefs.plateauRandomPropertyProperty().bind(plateauRandom.selectedProperty());
    plateauSliderBox.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty()));
    plateauRandomBox.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty()));
  }

  private void initMountainSlider() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGeneratorPrefs();
    double mountainDensityProperty = generatorPrefs.getMountainDensityProperty();
    boolean mountainRandomProperty = generatorPrefs.getMountainRandomProperty();
    mountainSlider.setLabelFormatter(getLabelConverter());
    mountainRandom.setSelected(mountainRandomProperty);
    mountainSlider.setValue(mountainDensityProperty);
    mountainSliderBox.visibleProperty().bind(mountainRandom.selectedProperty().not());
    generatorPrefs.mountainDensityPropertyProperty().bind(mountainSlider.valueProperty());
    generatorPrefs.mountainRandomPropertyProperty().bind(mountainRandom.selectedProperty());
    mountainSliderBox.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty()));
    mountainRandomBox.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty()));
  }

  private void initRampSlider() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGeneratorPrefs();
    double rampDensityProperty = generatorPrefs.getRampDensityProperty();
    boolean rampRandomProperty = generatorPrefs.getRampRandomProperty();
    rampSlider.setLabelFormatter(getLabelConverter());
    rampRandom.setSelected(rampRandomProperty);
    rampSlider.setValue(rampDensityProperty);
    rampSliderBox.visibleProperty().bind(rampRandom.selectedProperty().not());
    generatorPrefs.rampDensityPropertyProperty().bind(rampSlider.valueProperty());
    generatorPrefs.rampRandomPropertyProperty().bind(rampRandom.selectedProperty());
    rampSliderBox.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty()));
    rampRandomBox.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty()));
  }

  private byte getSliderValue(Slider slider, CheckBox checkBox) {
    if (checkBox.isSelected()) {
      return (byte) new Random().nextInt(127);
    }
    return (byte) slider.getValue();
  }

  protected byte[] getOptionArray() {
    byte spawnCount = spawnCountSpinner.getValue().byteValue();
    byte mapSize = (byte) (mapValues[validMapSizes.indexOf(mapSizeSpinner.getValue())] / 64);
    byte landDensity = (byte) (Byte.MAX_VALUE - getSliderValue(waterSlider, waterRandom));
    byte plateauDensity = getSliderValue(plateauSlider, plateauRandom);
    byte mountainDensity = getSliderValue(mountainSlider, mountainRandom);
    byte rampDensity = getSliderValue(rampSlider, rampRandom);
    return new byte[]{spawnCount, mapSize, landDensity, plateauDensity, mountainDensity, rampDensity};
  }

  public void onCloseButtonClicked() {
    onCloseButtonClickedListener.run();
  }

  public void onGenerateMapButtonClicked() {
    onGenerateMap();
  }

  public void onGenerateMap() {
    CompletableFuture<String> generateFuture;
    if (!previousMapName.getText().isEmpty()) {
      if (!mapGeneratorService.isGeneratedMap(previousMapName.getText())) {
        notificationService.addImmediateErrorNotification(new IllegalArgumentException(), "mapGenerator.invalidName");
        log.warn("Invalid Generated Map Name", new IllegalArgumentException());
        return;
      }
      generateFuture = mapGeneratorService.generateMap(previousMapName.getText());
    } else {
      byte[] optionArray = getOptionArray();
      generateFuture = mapGeneratorService.generateMap(optionArray);
    }
    generateFuture.thenAccept(mapName -> Platform.runLater(() -> {
      createGameController.initMapSelection();
      createGameController.mapListView.getItems().stream()
          .filter(mapBean -> mapBean.getFolderName().equalsIgnoreCase(mapName))
          .findAny().ifPresent(mapBean -> {
        createGameController.mapListView.getSelectionModel().select(mapBean);
        createGameController.mapListView.scrollTo(mapBean);
        createGameController.setSelectedMap(mapBean);
      });
    }))
        .exceptionally(throwable -> {
          handleGenerationException(throwable);
          return null;
        });
    onCloseButtonClickedListener.run();
  }

  private void handleGenerationException(Throwable e) {
    Throwable cause = e.getCause();
    if (cause instanceof InvalidParameterException) {
      notificationService.addImmediateErrorNotification(e, "mapGenerator.invalidName");
      log.warn("Map generation failed", e);
    } else if (cause instanceof UnsupportedVersionException) {
      notificationService.addImmediateErrorNotification(cause, "mapGenerator.tooNewVersion");
      log.warn("Map generation failed", e);
    } else if (cause instanceof OutdatedVersionException) {
      notificationService.addImmediateErrorNotification(cause, "mapGenerator.tooOldVersion");
      log.warn("Map generation failed", e);
    } else {
      notificationService.addImmediateErrorNotification(e, "mapGenerator.generationFailed");
      log.warn("Map generation failed", e);
    }
  }

  protected void setCreateGameController(CreateGameController controller) {
    createGameController = controller;
  }

  public Pane getRoot() {
    return generateMapRoot;
  }

  void setOnCloseButtonClickedListener(Runnable onCloseButtonClickedListener) {
    this.onCloseButtonClickedListener = onCloseButtonClickedListener;
  }
}
