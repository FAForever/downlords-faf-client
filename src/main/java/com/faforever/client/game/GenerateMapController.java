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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.ListSpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.RangeSlider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
  private final I18n i18n;
  public CreateGameController createGameController;
  public Pane generateMapRoot;
  public Button generateMapButton;
  public TextField previousMapName;
  public ComboBox<GenerationType> generationTypeComboBox;
  public Spinner<Integer> spawnCountSpinner;
  public Spinner<String> mapSizeSpinner;
  public RangeSlider waterSlider;
  public CheckBox waterRandom;
  public HBox waterSliderBox;
  public HBox waterRandomBox;
  public RangeSlider mountainSlider;
  public CheckBox mountainRandom;
  public HBox mountainSliderBox;
  public HBox mountainRandomBox;
  public RangeSlider plateauSlider;
  public CheckBox plateauRandom;
  public HBox plateauSliderBox;
  public HBox plateauRandomBox;
  public RangeSlider rampSlider;
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
    GeneratorPrefs genPrefs = preferencesService.getPreferences().getGeneratorPrefs();
    initOptionSlider(genPrefs.waterLowProperty(), genPrefs.waterHighProperty(), genPrefs.waterRandomProperty(),
        waterSlider, waterSliderBox, waterRandom, waterRandomBox);
    initOptionSlider(genPrefs.plateauLowProperty(), genPrefs.plateauHighProperty(), genPrefs.plateauRandomProperty(),
        plateauSlider, plateauSliderBox, plateauRandom, plateauRandomBox);
    initOptionSlider(genPrefs.mountainLowProperty(), genPrefs.mountainHighProperty(), genPrefs.mountainRandomProperty(),
        mountainSlider, mountainSliderBox, mountainRandom, mountainRandomBox);
    initOptionSlider(genPrefs.rampLowProperty(), genPrefs.rampHighProperty(), genPrefs.rampRandomProperty(),
        rampSlider, rampSliderBox, rampRandom, rampRandomBox);
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
    GenerationType generationTypeProperty = generatorPrefs.getGenerationType();
    generationTypeComboBox.setItems(FXCollections.observableArrayList(GenerationType.values()));
    generationTypeComboBox.setConverter(getGenerationTypeConverter());
    generationTypeComboBox.setValue(generationTypeProperty);
    generatorPrefs.generationTypeProperty().bind(generationTypeComboBox.valueProperty());
    generationTypeComboBox.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty()));
  }

  private void initSpawnCountSpinner() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGeneratorPrefs();
    int spawnCountProperty = generatorPrefs.getSpawnCount();
    spawnCountSpinner.setValueFactory(new IntegerSpinnerValueFactory(2, 16, spawnCountProperty, 2));
    generatorPrefs.spawnCountProperty().bind(spawnCountSpinner.getValueFactory().valueProperty());
    spawnCountSpinner.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty()));
  }

  private void initMapSizeSpinner() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGeneratorPrefs();
    String mapSizeProperty = generatorPrefs.getMapSize();
    mapSizeSpinner.setValueFactory(new ListSpinnerValueFactory<>(validMapSizes));
    mapSizeSpinner.increment(validMapSizes.indexOf(mapSizeProperty));
    generatorPrefs.mapSizeProperty().bind(mapSizeSpinner.getValueFactory().valueProperty());
    mapSizeSpinner.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty()));
  }

  private void initOptionSlider(IntegerProperty lowProperty, IntegerProperty highProperty, BooleanProperty randomProperty,
                                RangeSlider slider, HBox sliderContainer, CheckBox randomBox, HBox randomContainer) {
    sliderContainer.visibleProperty().bind(randomBox.selectedProperty().not());
    slider.lowValueProperty().bindBidirectional(lowProperty);
    slider.highValueProperty().bindBidirectional(highProperty);
    randomBox.selectedProperty().bindBidirectional(randomProperty);
    sliderContainer.disableProperty().bind(Bindings.or(Bindings.isNotEmpty(previousMapName.textProperty()), Bindings.notEqual(generationTypeComboBox.valueProperty(), GenerationType.CASUAL)));
    randomContainer.disableProperty().bind(Bindings.or(Bindings.isNotEmpty(previousMapName.textProperty()), Bindings.notEqual(generationTypeComboBox.valueProperty(), GenerationType.CASUAL)));
  }

  private Optional<Float> getSliderValue(RangeSlider slider, CheckBox checkBox) {
    if (checkBox.isSelected() || generationTypeComboBox.getValue() != GenerationType.CASUAL) {
      return Optional.empty();
    }
    int lowValue = (int) slider.getLowValue();
    int highValue = (int) slider.getHighValue();
    int range = highValue - lowValue;
    if (range > 0) {
      return Optional.of((new Random().nextInt(range) + lowValue) / 127f);
    } else {
      return Optional.of(lowValue / 127f);
    }
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
