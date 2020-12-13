package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.generator.GenerationType;
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
import javafx.scene.control.ComboBox;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class GenerateMapController implements Controller<Pane> {

  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final MapGeneratorService mapGeneratorService;
  private final I18n i18n;
  public CreateGameController createGameController;
  public Pane generateMapRoot;
  public Button generateMapButton;
  public TextField previousMapName;
  public ComboBox<GenerationType> generationTypeComboBox;
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
  private final ObservableList<String> validMapSizes = FXCollections.observableArrayList("5km", "10km", "20km");
  private final int[] mapValues = new int[]{256, 512, 1024};

  public void initialize() {
    initGenerationTypeSpinner();
    initSpawnCountSpinner();
    initMapSizeSpinner();
    initWaterSlider();
    initPlateauSlider();
    initMountainSlider();
    initRampSlider();
  }

  private StringConverter<GenerationType> getGenerationTypeConverter() {
    return new StringConverter<>() {
      @Override
      public String toString(GenerationType generationType) {
        return i18n.get(generationType.getI18nKey());
      }

      @Override
      public GenerationType fromString(String s) {
        throw new UnsupportedOperationException();
      }
    };
  }

  private void initGenerationTypeSpinner() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGeneratorPrefs();
    GenerationType generationTypeProperty = generatorPrefs.getGenerationTypeProperty();
    generationTypeComboBox.setItems(FXCollections.observableArrayList(GenerationType.values()));
    generationTypeComboBox.setConverter(getGenerationTypeConverter());
    generationTypeComboBox.setValue(generationTypeProperty);
    generatorPrefs.generationTypePropertyProperty().bind(generationTypeComboBox.valueProperty());
    generationTypeComboBox.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty()));
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
    waterSliderBox.disableProperty().bind(Bindings.or(Bindings.isNotEmpty(previousMapName.textProperty()), Bindings.notEqual(generationTypeComboBox.valueProperty(), GenerationType.CASUAL)));
    waterRandomBox.disableProperty().bind(Bindings.or(Bindings.isNotEmpty(previousMapName.textProperty()), Bindings.notEqual(generationTypeComboBox.valueProperty(), GenerationType.CASUAL)));
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
    plateauSliderBox.disableProperty().bind(Bindings.or(Bindings.isNotEmpty(previousMapName.textProperty()), Bindings.notEqual(generationTypeComboBox.valueProperty(), GenerationType.CASUAL)));
    plateauRandomBox.disableProperty().bind(Bindings.or(Bindings.isNotEmpty(previousMapName.textProperty()), Bindings.notEqual(generationTypeComboBox.valueProperty(), GenerationType.CASUAL)));
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
    mountainSliderBox.disableProperty().bind(Bindings.or(Bindings.isNotEmpty(previousMapName.textProperty()), Bindings.notEqual(generationTypeComboBox.valueProperty(), GenerationType.CASUAL)));
    mountainRandomBox.disableProperty().bind(Bindings.or(Bindings.isNotEmpty(previousMapName.textProperty()), Bindings.notEqual(generationTypeComboBox.valueProperty(), GenerationType.CASUAL)));
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
    rampSliderBox.disableProperty().bind(Bindings.or(Bindings.isNotEmpty(previousMapName.textProperty()), Bindings.notEqual(generationTypeComboBox.valueProperty(), GenerationType.CASUAL)));
    rampRandomBox.disableProperty().bind(Bindings.or(Bindings.isNotEmpty(previousMapName.textProperty()), Bindings.notEqual(generationTypeComboBox.valueProperty(), GenerationType.CASUAL)));
  }

  private Optional<Float> getSliderValue(Slider slider, CheckBox checkBox) {
    if (checkBox.isSelected() || generationTypeComboBox.getValue() != GenerationType.CASUAL) {
      return Optional.empty();
    }
    return Optional.of(((byte) slider.getValue()) / 127f);
  }

  protected Map<String, Float> getOptionMap() {
    Map<String, Float> optionMap = new HashMap<>();
    if (generationTypeComboBox.getValue() == GenerationType.CASUAL) {
      getSliderValue(waterSlider, waterRandom).ifPresent(value -> optionMap.put("landDensity", 1 - value));
      getSliderValue(plateauSlider, plateauRandom).ifPresent(value -> optionMap.put("plateauDensity", value));
      getSliderValue(mountainSlider, mountainRandom).ifPresent(value -> optionMap.put("mountainDensity", value));
      getSliderValue(rampSlider, rampRandom).ifPresent(value -> optionMap.put("rampDensity", value));
    }
    return optionMap;
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
        log.warn("Invalid Generated Map Name", new IllegalArgumentException());
        notificationService.addImmediateErrorNotification(new IllegalArgumentException(), "mapGenerator.invalidName");
        return;
      }
      generateFuture = mapGeneratorService.generateMap(previousMapName.getText());
    } else {
      int spawnCount = spawnCountSpinner.getValue();
      int mapSize = mapValues[validMapSizes.indexOf(mapSizeSpinner.getValue())];
      GenerationType generationType = generationTypeComboBox.getValue();
      generateFuture = mapGeneratorService.generateMap(spawnCount, mapSize, getOptionMap(), generationType);
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
      log.warn("Map generation failed due to invalid parameter", e);
      notificationService.addImmediateErrorNotification(e, "mapGenerator.invalidName");
    } else if (cause instanceof UnsupportedVersionException) {
      log.warn("Map generation failed due to unsupported version", e);
      notificationService.addImmediateErrorNotification(cause, "mapGenerator.tooNewVersion");
    } else if (cause instanceof OutdatedVersionException) {
      log.warn("Map generation failed due to outdated version", e);
      notificationService.addImmediateErrorNotification(cause, "mapGenerator.tooOldVersion");
    } else {
      log.warn("Map generation failed", e);
      notificationService.addImmediateErrorNotification(e, "mapGenerator.generationFailed");
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
