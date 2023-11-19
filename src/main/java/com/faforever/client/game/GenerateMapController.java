package com.faforever.client.game;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.generator.GenerationType;
import com.faforever.client.map.generator.GeneratorOptions;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.map.generator.OutdatedVersionException;
import com.faforever.client.map.generator.UnsupportedVersionException;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.GeneratorPrefs;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class GenerateMapController extends NodeController<Pane> {

  public static final double MIN_MAP_SIZE_STEP = 1.25;
  public static final double KM_TO_PIXEL_FACTOR = 51.2;

  private final NotificationService notificationService;
  private final MapGeneratorService mapGeneratorService;
  private final I18n i18n;
  private final GeneratorPrefs generatorPrefs;

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
  public Spinner<Double> mapSizeSpinner;
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
  private final ObservableList<Integer> validTeamSizes = FXCollections.observableList(IntStream.range(0, 17)
      .filter(value -> value != 1)
      .boxed()
      .collect(Collectors.toList()));
  private final FilteredList<Integer> selectableTeamSizes = new FilteredList<>(validTeamSizes);
  private final ObservableList<Integer> validSpawnCount = FXCollections.observableList(IntStream.range(2, 17)
      .boxed()
      .collect(Collectors.toList()));
  private final FilteredList<Integer> selectableSpawnCounts = new FilteredList<>(validSpawnCount);
  public Spinner<Integer> numTeamsSpinner;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(commandLineLabel, commandLineArgsText, mapStyleComboBox, mapStyleLabel);
    initCommandlineArgs();
    initGenerationTypeComboBox();
    initMapStyleComboBox();
    initNumTeamsSpinner();
    initSpawnCountSpinner();
    initMapSizeSpinner();
    initOptionSlider(generatorPrefs.waterDensityProperty(), generatorPrefs.waterRandomProperty(), waterSlider, waterSliderBox, waterRandom, waterRandomBox);
    initOptionSlider(generatorPrefs.plateauDensityProperty(), generatorPrefs.plateauRandomProperty(), plateauSlider, plateauSliderBox, plateauRandom, plateauRandomBox);
    initOptionSlider(generatorPrefs.mountainDensityProperty(), generatorPrefs.mountainRandomProperty(), mountainSlider, mountainSliderBox, mountainRandom, mountainRandomBox);
    initOptionSlider(generatorPrefs.rampDensityProperty(), generatorPrefs.rampRandomProperty(), rampSlider, rampSliderBox, rampRandom, rampRandomBox);
    initOptionSlider(generatorPrefs.mexDensityProperty(), generatorPrefs.mexRandomProperty(), mexSlider, mexSliderBox, mexRandom, mexRandomBox);
    initOptionSlider(generatorPrefs.reclaimDensityProperty(), generatorPrefs.reclaimRandomProperty(), reclaimSlider, reclaimSliderBox, reclaimRandom, reclaimRandomBox);
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

  private StringConverter<Double> getMapSizeConverter() {
    return new StringConverter<>() {
      @Override
      public String toString(Double mapSize) {
        return NumberFormat.getInstance().format(mapSize);
      }

      @Override
      public Double fromString(String s) {
        try {
          return Math.round(NumberFormat.getInstance().parse(s).doubleValue() / MIN_MAP_SIZE_STEP) * MIN_MAP_SIZE_STEP;
        } catch (ParseException e) {
          throw new IllegalArgumentException("Could not parse number", e);
        }
      }
    };
  }

  private void initCommandlineArgs() {
    String commandLineArgs = generatorPrefs.getCommandLineArgs();
    commandLineArgsText.setText(commandLineArgs);
    generatorPrefs.commandLineArgsProperty().bind(commandLineArgsText.textProperty());
    commandLineArgsText.disableProperty().bind(previousMapName.textProperty().isNotEmpty());
    if (!commandLineArgsText.getText().isBlank()) {
      commandLineArgsText.setVisible(true);
      commandLineLabel.setVisible(true);
    }
  }

  private void initGenerationTypeComboBox() {
    GenerationType generationType = generatorPrefs.getGenerationType();
    generationTypeComboBox.setItems(FXCollections.observableArrayList(GenerationType.values()));
    generationTypeComboBox.setConverter(getGenerationTypeConverter());
    generationTypeComboBox.setValue(generationType);
    generatorPrefs.generationTypeProperty().bind(generationTypeComboBox.valueProperty());
    generationTypeComboBox.disableProperty()
        .bind(previousMapName.textProperty().isNotEmpty()
            .or(commandLineArgsText.textProperty().isNotEmpty()));
  }

  private void initNumTeamsSpinner() {
    int numTeamsProperty = generatorPrefs.getNumTeams();
    numTeamsSpinner.setValueFactory(new ListSpinnerValueFactory<>(selectableTeamSizes));
    numTeamsSpinner.valueProperty().addListener((observable) -> {
      if (spawnCountSpinner.getValue() != null) {
        int spawnCount = spawnCountSpinner.getValue();
        int lastIndex = selectableSpawnCounts.indexOf(spawnCount);
        selectableSpawnCounts.setPredicate((value) -> {
          Integer numTeams = numTeamsSpinner.getValue();
          return numTeams == null || numTeams == 0 || value % numTeams == 0;
        });
        int newIndex = selectableSpawnCounts.indexOf(spawnCount);
        if (newIndex > 0) {
          int diff = newIndex - lastIndex;
          if (diff > 0) {
            spawnCountSpinner.increment(diff);
          } else {
            spawnCountSpinner.decrement(-diff);
          }
        }
      }
    });
    generatorPrefs.numTeamsProperty().bind(numTeamsSpinner.valueProperty());
    numTeamsSpinner.disableProperty()
        .bind(previousMapName.textProperty().isNotEmpty()
            .or(commandLineArgsText.textProperty().isNotEmpty()));
    int lastIndex = selectableTeamSizes.indexOf(numTeamsProperty);
    numTeamsSpinner.increment(lastIndex >= 0 ? lastIndex : 1);
  }

  private void initSpawnCountSpinner() {
    int spawnCountProperty = generatorPrefs.getSpawnCount();
    selectableSpawnCounts.setPredicate((value) -> {
      Integer numTeams = numTeamsSpinner.getValue();
      return numTeams == null || numTeams == 0 || value % numTeams == 0;
    });
    spawnCountSpinner.setValueFactory(new ListSpinnerValueFactory<>(selectableSpawnCounts));
    generatorPrefs.spawnCountProperty().bind(spawnCountSpinner.valueProperty());
    spawnCountSpinner.disableProperty()
        .bind(previousMapName.textProperty().isNotEmpty()
            .or(commandLineArgsText.textProperty().isNotEmpty()));
    int lastIndex = selectableSpawnCounts.indexOf(spawnCountProperty);
    spawnCountSpinner.increment(Math.max(lastIndex, 0));
  }

  private void initMapSizeSpinner() {
    double mapSize = generatorPrefs.getMapSizeInKm();
    mapSizeSpinner.setValueFactory(new DoubleSpinnerValueFactory(5, 20, mapSize, MIN_MAP_SIZE_STEP));
    mapSizeSpinner.getValueFactory().setConverter(getMapSizeConverter());
    generatorPrefs.mapSizeInKmProperty().bind(mapSizeSpinner.getValueFactory().valueProperty());
    mapSizeSpinner.disableProperty()
        .bind(previousMapName.textProperty().isNotEmpty()
            .or(commandLineArgsText.textProperty().isNotEmpty()));
  }

  private void initMapStyleComboBox() {
    mapStyleComboBox.disableProperty()
        .bind(previousMapName.textProperty().isNotEmpty()
            .or(generationTypeComboBox.valueProperty().isNotEqualTo(GenerationType.CASUAL))
            .or(commandLineArgsText.textProperty().isNotEmpty()));
  }

  private void initOptionSlider(IntegerProperty valueProperty, BooleanProperty randomProperty, Slider slider,
                                HBox sliderContainer, CheckBox randomBox, HBox randomContainer) {
    sliderContainer.visibleProperty().bind(randomBox.selectedProperty().not());
    slider.setValue(valueProperty.getValue());
    randomBox.setSelected(randomProperty.getValue());
    slider.valueProperty().bindBidirectional(valueProperty);
    randomBox.selectedProperty().bindBidirectional(randomProperty);
    sliderContainer.disableProperty()
        .bind(previousMapName.textProperty().isNotEmpty()
            .or(generationTypeComboBox.valueProperty().isNotEqualTo(GenerationType.CASUAL))
            .or(commandLineArgsText.textProperty().isNotEmpty())
            .or(mapStyleComboBox.valueProperty().isNotNull())
                .and(mapStyleComboBox.valueProperty().isNotEqualTo(MapGeneratorService.GENERATOR_RANDOM_STYLE)));
    randomContainer.disableProperty()
        .bind(previousMapName.textProperty().isNotEmpty()
            .or(generationTypeComboBox.valueProperty().isNotEqualTo(GenerationType.CASUAL))
            .or(commandLineArgsText.textProperty().isNotEmpty())
            .or(mapStyleComboBox.valueProperty().isNotNull())
                .and(mapStyleComboBox.valueProperty().isNotEqualTo(MapGeneratorService.GENERATOR_RANDOM_STYLE)));
  }

  private Optional<Float> getSliderValue(Slider slider, CheckBox checkBox) {
    if (checkBox.isSelected() || generationTypeComboBox.getValue() != GenerationType.CASUAL) {
      return Optional.empty();
    }
    return Optional.of(((byte) slider.getValue()) / 127f);
  }

  private GeneratorOptions getGeneratorOptions() {
    GeneratorOptions.GeneratorOptionsBuilder optionsBuilder = GeneratorOptions.builder();
    if (!commandLineArgsText.getText().isBlank()) {
      optionsBuilder.commandLineArgs(commandLineArgsText.getText());
    }

    optionsBuilder.spawnCount(spawnCountSpinner.getValue());
    optionsBuilder.mapSize((int) (mapSizeSpinner.getValue() * KM_TO_PIXEL_FACTOR));
    optionsBuilder.numTeams(numTeamsSpinner.getValue());
    optionsBuilder.generationType(generationTypeComboBox.getValue());
    optionsBuilder.style(mapStyleComboBox.getValue());
    getSliderValue(waterSlider, waterRandom).ifPresent(value -> optionsBuilder.landDensity(1 - value));
    getSliderValue(plateauSlider, plateauRandom).ifPresent(optionsBuilder::plateauDensity);
    getSliderValue(mountainSlider, mountainRandom).ifPresent(optionsBuilder::mountainDensity);
    getSliderValue(rampSlider, rampRandom).ifPresent(optionsBuilder::rampDensity);
    getSliderValue(mexSlider, mexRandom).ifPresent(optionsBuilder::mexDensity);
    getSliderValue(reclaimSlider, reclaimRandom).ifPresent(optionsBuilder::reclaimDensity);
    return optionsBuilder.build();
  }

  public void onCloseButtonClicked() {
    if (onCloseButtonClickedListener != null) {
      onCloseButtonClickedListener.run();
    }
  }

  public void onGenerateMapButtonClicked() {
    onGenerateMap();
  }

  public void onGenerateMap() {
    CompletableFuture<String> generateFuture;
    if (!previousMapName.getText().isEmpty()) {
      if (!mapGeneratorService.isGeneratedMap(previousMapName.getText())) {
        log.warn(String.format("Invalid Generated Map Name %s", previousMapName.getText()));
        notificationService.addImmediateWarnNotification("mapGenerator.invalidName");
        return;
      }
      generateFuture = mapGeneratorService.generateMap(previousMapName.getText());
    } else {
      generateFuture = mapGeneratorService.generateMap(getGeneratorOptions());
    }

    generateFuture.exceptionally(throwable -> {
      handleGenerationException(throwable);
      return null;
    });
    onCloseButtonClicked();
  }

  private void handleGenerationException(Throwable e) {
    Throwable cause = e.getCause();
    if (cause instanceof InvalidParameterException) {
      log.error("Map generation failed due to invalid parameter", e);
      notificationService.addImmediateErrorNotification(e, "mapGenerator.invalidName");
    } else if (cause instanceof UnsupportedVersionException) {
      log.warn("Map generation failed due to unsupported version", e);
      notificationService.addImmediateWarnNotification("mapGenerator.tooNewVersion");
    } else if (cause instanceof OutdatedVersionException) {
      log.warn("Map generation failed due to outdated version", e);
      notificationService.addImmediateWarnNotification("mapGenerator.tooOldVersion");
    } else {
      log.error("Map generation failed", e);
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

  @Override
  public Pane getRoot() {
    return generateMapRoot;
  }

  void setOnCloseButtonClickedListener(Runnable onCloseButtonClickedListener) {
    this.onCloseButtonClickedListener = onCloseButtonClickedListener;
  }
}
