package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.generator.GenerationType;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.map.generator.OutdatedVersionException;
import com.faforever.client.map.generator.UnsupportedVersionException;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.GeneratorPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.ListSpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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
import java.util.List;
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
  public Label commandLineLabel;
  public TextField commandLineArgsText;
  public ComboBox<GenerationType> generationTypeComboBox;
  public Label mapStyleLabel;
  public ComboBox<String> mapStyleComboBox;
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
  public Slider mexSlider;
  public CheckBox mexRandom;
  public HBox mexSliderBox;
  public HBox mexRandomBox;
  public Slider reclaimSlider;
  public CheckBox reclaimRandom;
  public HBox reclaimSliderBox;
  public HBox reclaimRandomBox;
  private Runnable onCloseButtonClickedListener;
  private final ObservableList<String> validMapSizes = FXCollections.observableArrayList("5km", "10km", "20km");
  private final int[] mapValues = new int[]{256, 512, 1024};

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(commandLineLabel, commandLineArgsText, mapStyleComboBox, mapStyleLabel);
    initCommandlineArgs();
    initGenerationTypeComboBox();
    initMapStyleComboBox();
    initSpawnCountSpinner();
    initMapSizeSpinner();
    GeneratorPrefs genPrefs = preferencesService.getPreferences().getGenerator();
    initOptionSlider(genPrefs.waterDensityProperty(), genPrefs.waterRandomProperty(),
        waterSlider, waterSliderBox, waterRandom, waterRandomBox);
    initOptionSlider(genPrefs.plateauDensityProperty(), genPrefs.plateauRandomProperty(),
        plateauSlider, plateauSliderBox, plateauRandom, plateauRandomBox);
    initOptionSlider(genPrefs.mountainDensityProperty(), genPrefs.mountainRandomProperty(),
        mountainSlider, mountainSliderBox, mountainRandom, mountainRandomBox);
    initOptionSlider(genPrefs.rampDensityProperty(), genPrefs.rampRandomProperty(),
        rampSlider, rampSliderBox, rampRandom, rampRandomBox);
    initOptionSlider(genPrefs.mexDensityProperty(), genPrefs.mexRandomProperty(),
        mexSlider, mexSliderBox, mexRandom, mexRandomBox);
    initOptionSlider(genPrefs.reclaimDensityProperty(), genPrefs.reclaimRandomProperty(),
        reclaimSlider, reclaimSliderBox, reclaimRandom, reclaimRandomBox);
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

  private void initCommandlineArgs() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGenerator();
    String commandLineArgs = generatorPrefs.getCommandLineArgs();
    commandLineArgsText.setText(commandLineArgs);
    generatorPrefs.commandLineArgsProperty().bind(commandLineArgsText.textProperty());
    commandLineArgsText.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty()));
    if (!commandLineArgsText.getText().isBlank()) {
      commandLineArgsText.setVisible(true);
      commandLineLabel.setVisible(true);
    }
  }

  private void initGenerationTypeComboBox() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGenerator();
    GenerationType generationType = generatorPrefs.getGenerationType();
    generationTypeComboBox.setItems(FXCollections.observableArrayList(GenerationType.values()));
    generationTypeComboBox.setConverter(getGenerationTypeConverter());
    generationTypeComboBox.setValue(generationType);
    generatorPrefs.generationTypeProperty().bind(generationTypeComboBox.valueProperty());
    generationTypeComboBox.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty())
        .or(Bindings.isNotEmpty(commandLineArgsText.textProperty())));
  }

  private void initSpawnCountSpinner() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGenerator();
    int spawnCountProperty = generatorPrefs.getSpawnCount();
    spawnCountSpinner.setValueFactory(new IntegerSpinnerValueFactory(2, 16, spawnCountProperty, 2));
    generatorPrefs.spawnCountProperty().bind(spawnCountSpinner.getValueFactory().valueProperty());
    spawnCountSpinner.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty())
        .or(Bindings.isNotEmpty(commandLineArgsText.textProperty())));
  }

  private void initMapSizeSpinner() {
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGenerator();
    String mapSizeProperty = generatorPrefs.getMapSize();
    mapSizeSpinner.setValueFactory(new ListSpinnerValueFactory<>(validMapSizes));
    mapSizeSpinner.increment(validMapSizes.indexOf(mapSizeProperty));
    generatorPrefs.mapSizeProperty().bind(mapSizeSpinner.getValueFactory().valueProperty());
    mapSizeSpinner.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty())
        .or(Bindings.isNotEmpty(commandLineArgsText.textProperty())));
  }

  private void initMapStyleComboBox() {
    mapStyleComboBox.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty())
        .or(Bindings.notEqual(generationTypeComboBox.valueProperty(), GenerationType.CASUAL))
        .or(Bindings.isNotEmpty(commandLineArgsText.textProperty())));
  }

  private void initOptionSlider(IntegerProperty valueProperty, BooleanProperty randomProperty,
                                Slider slider, HBox sliderContainer, CheckBox randomBox, HBox randomContainer) {
    sliderContainer.visibleProperty().bind(randomBox.selectedProperty().not());
    slider.setValue(valueProperty.getValue());
    randomBox.setSelected(randomProperty.getValue());
    slider.valueProperty().bindBidirectional(valueProperty);
    randomBox.selectedProperty().bindBidirectional(randomProperty);
    sliderContainer.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty())
        .or(Bindings.notEqual(generationTypeComboBox.valueProperty(), GenerationType.CASUAL))
        .or(Bindings.isNotEmpty(commandLineArgsText.textProperty()))
        .or(Bindings.isNotNull(mapStyleComboBox.valueProperty())
            .and(Bindings.notEqual(mapStyleComboBox.valueProperty(), MapGeneratorService.GENERATOR_RANDOM_STYLE))));
    randomContainer.disableProperty().bind(Bindings.isNotEmpty(previousMapName.textProperty())
        .or(Bindings.notEqual(generationTypeComboBox.valueProperty(), GenerationType.CASUAL))
        .or(Bindings.isNotEmpty(commandLineArgsText.textProperty()))
        .or(Bindings.isNotNull(mapStyleComboBox.valueProperty())
            .and(Bindings.notEqual(mapStyleComboBox.valueProperty(), MapGeneratorService.GENERATOR_RANDOM_STYLE))));
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
      getSliderValue(mexSlider, mexRandom).ifPresent(value -> optionMap.put("mexDensity", value));
      getSliderValue(reclaimSlider, reclaimRandom).ifPresent(value -> optionMap.put("reclaimDensity", value));
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
    preferencesService.storeInBackground();
    CompletableFuture<String> generateFuture;
    if (!previousMapName.getText().isEmpty()) {
      if (!mapGeneratorService.isGeneratedMap(previousMapName.getText())) {
        log.warn(String.format("Invalid Generated Map Name %s", previousMapName.getText()));
        notificationService.addImmediateWarnNotification("mapGenerator.invalidName");
        return;
      }
      generateFuture = mapGeneratorService.generateMap(previousMapName.getText());
    } else if (!commandLineArgsText.getText().isBlank()) {
      generateFuture = mapGeneratorService.generateMapWithArgs(commandLineArgsText.getText());
    } else {
      int spawnCount = spawnCountSpinner.getValue();
      int mapSize = mapValues[validMapSizes.indexOf(mapSizeSpinner.getValue())];
      if (mapStyleComboBox.getValue() != null && !MapGeneratorService.GENERATOR_RANDOM_STYLE.equals(mapStyleComboBox.getValue())) {
        String style = mapStyleComboBox.getValue();
        generateFuture = mapGeneratorService.generateMap(spawnCount, mapSize, style);
      } else {
        GenerationType generationType = generationTypeComboBox.getValue();
        generateFuture = mapGeneratorService.generateMap(spawnCount, mapSize, getOptionMap(), generationType);
      }
    }
    generateFuture.thenAccept(mapName -> JavaFxUtil.runLater(() -> {
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
      notificationService.addImmediateWarnNotification("mapGenerator.tooNewVersion");
    } else if (cause instanceof OutdatedVersionException) {
      log.warn("Map generation failed due to outdated version", e);
      notificationService.addImmediateWarnNotification("mapGenerator.tooOldVersion");
    } else {
      log.warn("Map generation failed", e);
      notificationService.addImmediateErrorNotification(e, "mapGenerator.generationFailed");
    }
  }

  @VisibleForTesting
  void toggleCommandlineInput() {
    commandLineLabel.setVisible(!commandLineLabel.isVisible());
    commandLineArgsText.setVisible(!commandLineArgsText.isVisible());
  }

  protected void setCreateGameController(CreateGameController controller) {
    createGameController = controller;
  }

  protected void setStyles(List<String> styles) {
    styles.add(0, MapGeneratorService.GENERATOR_RANDOM_STYLE);
    mapStyleComboBox.setItems(FXCollections.observableList(styles));
    GeneratorPrefs generatorPrefs = preferencesService.getPreferences().getGenerator();
    String mapStyle = generatorPrefs.getMapStyle();
    if (mapStyleComboBox.getItems().contains(mapStyle)) {
      mapStyleComboBox.getSelectionModel().select(mapStyle);
    } else {
      mapStyleComboBox.getSelectionModel().select(MapGeneratorService.GENERATOR_RANDOM_STYLE);
    }
    generatorPrefs.mapStyleProperty().bind(mapStyleComboBox.valueProperty());
    mapStyleComboBox.setVisible(true);
    mapStyleLabel.setVisible(true);
  }

  public void onNewLabelClicked(MouseEvent mouseEvent) {
    if (mouseEvent.getButton().equals(MouseButton.PRIMARY) && mouseEvent.getClickCount() == 2) {
      toggleCommandlineInput();
    }
  }

  public Pane getRoot() {
    return generateMapRoot;
  }

  void setOnCloseButtonClickedListener(Runnable onCloseButtonClickedListener) {
    this.onCloseButtonClickedListener = onCloseButtonClickedListener;
  }
}
