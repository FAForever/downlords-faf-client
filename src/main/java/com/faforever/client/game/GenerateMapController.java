package com.faforever.client.game;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.ToStringOnlyConverter;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.generator.GenerationType;
import com.faforever.client.map.generator.GeneratorOptions;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.map.generator.OutdatedVersionException;
import com.faforever.client.map.generator.UnsupportedVersionException;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.GeneratorPrefs;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.ListSpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.CheckModel;
import org.controlsfx.control.RangeSlider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.InvalidParameterException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Random;
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

  public Pane generateMapRoot;
  public Button generateMapButton;
  public TextField previousMapName;
  public Label commandLineLabel;
  public TextField commandLineArgsText;
  public ComboBox<GenerationType> generationTypeComboBox;
  public CheckComboBox<String> mapStyleCheckComboBox;
  public CheckComboBox<String> biomeCheckComboBox;
  public Spinner<Integer> spawnCountSpinner;
  public Spinner<Double> mapSizeSpinner;
  public CheckComboBox<String> symmetryCheckComboBox;
  public CheckBox customStyleCheckBox;
  public CheckBox fixedSeedCheckBox;
  public TextField seedTextField;
  public Button seedRerollButton;
  public CheckComboBox<String> terrainCheckComboBox;
  public CheckComboBox<String> resourcesCheckComboBox;
  public CheckComboBox<String> propsCheckComboBox;
  public RangeSlider reclaimDensitySlider;
  public RangeSlider resourcesDensitySlider;

  private Runnable onCloseButtonClickedListener;
  private final ObservableList<Integer> validTeamSizes = FXCollections.observableList(
      IntStream.range(0, 17).filter(value -> value != 1).boxed().collect(Collectors.toList()));
  private final FilteredList<Integer> selectableTeamSizes = new FilteredList<>(validTeamSizes);
  private final ObservableList<Integer> validSpawnCount = FXCollections.observableList(
      IntStream.range(2, 17).boxed().collect(Collectors.toList()));
  private final FilteredList<Integer> selectableSpawnCounts = new FilteredList<>(validSpawnCount);
  public Spinner<Integer> numTeamsSpinner;
  private final BooleanProperty disableCustomization = new SimpleBooleanProperty();

  @Override
  protected void onInitialize() {
    disableCustomization.bind(previousMapName.textProperty()
                                             .isNotEmpty()
                                             .or(generationTypeComboBox.valueProperty()
                                                                       .isNotEqualTo(GenerationType.CASUAL))
                                             .or(commandLineArgsText.textProperty().isNotEmpty()));

    JavaFxUtil.bindManagedToVisible(commandLineLabel, commandLineArgsText);
    initCommandlineArgs();
    initGenerationTypeComboBox();
    initSymmetryComboBox();
    initMapStyleComboBox();
    initCustomStyleOptions();
    initNumTeamsSpinner();
    initSpawnCountSpinner();
    initMapSizeSpinner();
    initSeedField();

    bindCheckComboBoxTitle(terrainCheckComboBox, biomeCheckComboBox, resourcesCheckComboBox, propsCheckComboBox,
                           resourcesCheckComboBox, symmetryCheckComboBox, mapStyleCheckComboBox);
    bindCustomStyleDisabledPropertyNonSliders(terrainCheckComboBox, biomeCheckComboBox, resourcesCheckComboBox,
                                              propsCheckComboBox);
    bindCustomStyleDisabledPropertySlider(resourcesDensitySlider, resourcesCheckComboBox);
    bindCustomStyleDisabledPropertySlider(reclaimDensitySlider, propsCheckComboBox);
  }

  private void bindCheckComboBoxTitle(CheckComboBox<?>... checkComboBoxes) {
    String emptyTitle = "RANDOM";
    for (CheckComboBox<?> checkComboBox : checkComboBoxes) {
      checkComboBox.setTitle(emptyTitle);
      checkComboBox.getCheckModel().getCheckedItems().subscribe(() -> {
        String title = checkComboBox.getCheckModel().getCheckedItems().isEmpty() ? emptyTitle : null;
        checkComboBox.setTitle(title);
      });
    }
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
                          .bind(previousMapName.textProperty()
                                               .isNotEmpty()
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
                   .bind(
                       previousMapName.textProperty().isNotEmpty().or(commandLineArgsText.textProperty().isNotEmpty()));
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
                     .bind(previousMapName.textProperty()
                                          .isNotEmpty()
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
                  .bind(
                      previousMapName.textProperty().isNotEmpty().or(commandLineArgsText.textProperty().isNotEmpty()));
  }

  private void initSymmetryComboBox() {
    symmetryCheckComboBox.disableProperty().bind(disableCustomization);
  }

  private void initMapStyleComboBox() {
    mapStyleCheckComboBox.disableProperty().bind(disableCustomization
                                                .or(customStyleCheckBox.selectedProperty()));
  }

  private void initCustomStyleOptions() {
    customStyleCheckBox.setSelected(generatorPrefs.getCustomStyle());
    generatorPrefs.customStyleProperty().bind(customStyleCheckBox.selectedProperty());
    customStyleCheckBox.disableProperty().bind(disableCustomization);
    fixedSeedCheckBox.setSelected(generatorPrefs.getFixedSeed());
    generatorPrefs.fixedSeedProperty().bind(fixedSeedCheckBox.selectedProperty());
    fixedSeedCheckBox.disableProperty().bind(disableCustomization);

    reclaimDensitySlider.setLabelFormatter(
        new LessMoreStringConverter(reclaimDensitySlider.getMin(), reclaimDensitySlider.getMax()));
    resourcesDensitySlider.setLabelFormatter(
        new LessMoreStringConverter(resourcesDensitySlider.getMin(), resourcesDensitySlider.getMax()));
    reclaimDensitySlider.setHighValue(generatorPrefs.getReclaimDensityMax());
    generatorPrefs.reclaimDensityMaxProperty().bind(reclaimDensitySlider.highValueProperty());
    reclaimDensitySlider.setLowValue(generatorPrefs.getReclaimDensityMin());
    generatorPrefs.reclaimDensityMinProperty().bind(reclaimDensitySlider.lowValueProperty());
    resourcesDensitySlider.setHighValue(generatorPrefs.getResourceDensityMax());
    generatorPrefs.resourceDensityMaxProperty().bind(resourcesDensitySlider.highValueProperty());
    resourcesDensitySlider.setLowValue(generatorPrefs.getResourceDensityMin());
    generatorPrefs.resourceDensityMinProperty().bind(resourcesDensitySlider.lowValueProperty());
  }

  private void initSeedField() {
    seedTextField.setText(String.valueOf(generatorPrefs.getSeed()));
    generatorPrefs.seedProperty().bind(seedTextField.textProperty());

    BooleanBinding customizationAllowedOrNoFixedSeed = disableCustomization.or(
        fixedSeedCheckBox.selectedProperty().not());

    seedTextField.disableProperty().bind(customizationAllowedOrNoFixedSeed);
    seedRerollButton.disableProperty().bind(customizationAllowedOrNoFixedSeed);
  }

  private void bindCustomStyleDisabledPropertyNonSliders(Node... nodes) {
    for (Node node : nodes) {
      node.disableProperty().bind(disableCustomization.or(customStyleCheckBox.selectedProperty().not()));
    }
  }

  private void bindCustomStyleDisabledPropertySlider(RangeSlider slider, CheckComboBox<String> relatedComboBox) {
    slider.disableProperty()
          .bind(disableCustomization.or(customStyleCheckBox.selectedProperty().not())
                                    .or(BooleanExpression.booleanExpression(
                                        relatedComboBox.checkModelProperty().map(CheckModel::isEmpty))));
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
    Random random = new Random();
    if (generationTypeComboBox.getValue() == GenerationType.CASUAL) {
      ObservableList<String> symmetryCheckedItems = symmetryCheckComboBox.getCheckModel().getCheckedItems();
      if (!symmetryCheckedItems.isEmpty()) {
        optionsBuilder.symmetry(symmetryCheckedItems.get(random.nextInt(symmetryCheckedItems.size())));
      }
      if (fixedSeedCheckBox.isSelected()) {
        optionsBuilder.seed(seedTextField.getText());
      }
      if (customStyleCheckBox.isSelected()) {
        ObservableList<String> terrainCheckedItems = terrainCheckComboBox.getCheckModel().getCheckedItems();
        if (!terrainCheckedItems.isEmpty()) {
          optionsBuilder.terrainStyle(terrainCheckedItems.get(random.nextInt(terrainCheckedItems.size())));
        }
        ObservableList<String> biomeCheckedItems = biomeCheckComboBox.getCheckModel().getCheckedItems();
        if (!biomeCheckedItems.isEmpty()) {
          optionsBuilder.textureStyle(biomeCheckedItems.get(random.nextInt(biomeCheckedItems.size())));
        }
        ObservableList<String> resourcesCheckedItems = resourcesCheckComboBox.getCheckModel().getCheckedItems();
        if (!resourcesCheckedItems.isEmpty()) {
          optionsBuilder.resourceStyle(resourcesCheckedItems.get(random.nextInt(resourcesCheckedItems.size())));
        }
        ObservableList<String> propCheckedItems = propsCheckComboBox.getCheckModel().getCheckedItems();
        if (!propCheckedItems.isEmpty()) {
          optionsBuilder.propStyle(propCheckedItems.get(random.nextInt(propCheckedItems.size())));
        }
        int reclaimLowValue = (int) reclaimDensitySlider.getLowValue();
        int reclaimHighValue = (int) reclaimDensitySlider.getHighValue();
        if (reclaimLowValue == reclaimHighValue) {
          optionsBuilder.reclaimDensity(reclaimLowValue / 127f);
        } else {
          optionsBuilder.reclaimDensity(random.nextInt(reclaimLowValue, reclaimHighValue) / 127f);
        }
        int resourcesLowValue = (int) resourcesDensitySlider.getLowValue();
        int resourcesHighValue = (int) resourcesDensitySlider.getHighValue();
        if (resourcesLowValue == resourcesHighValue) {
          optionsBuilder.resourceDensity(resourcesLowValue / 127f);
        } else {
          optionsBuilder.resourceDensity(random.nextInt(resourcesLowValue, resourcesHighValue) / 127f);
        }
      } else {
        ObservableList<String> styleCheckedItems = mapStyleCheckComboBox.getCheckModel().getCheckedItems();
        if (!styleCheckedItems.isEmpty()) {
          optionsBuilder.style(styleCheckedItems.get(random.nextInt(styleCheckedItems.size())));
        }
      }
    }

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
    Mono<String> generateFuture;
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

    generateFuture.subscribe(null, this::handleGenerationException);
    onCloseButtonClicked();
  }

  private void handleGenerationException(Throwable e) {
    switch (e) {
      case InvalidParameterException ignored -> {
        log.error("Map generation failed due to invalid parameter", e);
        notificationService.addImmediateErrorNotification(e, "mapGenerator.invalidName");
      }
      case UnsupportedVersionException ignored -> {
        log.warn("Map generation failed due to unsupported version", e);
        notificationService.addImmediateWarnNotification("mapGenerator.tooNewVersion");
      }
      case OutdatedVersionException ignored -> {
        log.warn("Map generation failed due to outdated version", e);
        notificationService.addImmediateWarnNotification("mapGenerator.tooOldVersion");
      }
      case null, default -> {
        log.error("Map generation failed", e);
        notificationService.addImmediateErrorNotification(e, "mapGenerator.generationFailed");
      }
    }
  }

  @VisibleForTesting
  void toggleCommandlineInput() {
    commandLineLabel.setVisible(!commandLineLabel.isVisible());
    commandLineArgsText.setVisible(!commandLineArgsText.isVisible());
  }

  protected void setSymmetries(List<String> symmetries) {
    symmetryCheckComboBox.getItems().setAll(symmetries);
    ObservableList<String> savedSymmetries = generatorPrefs.getSymmetries();
    savedSymmetries.forEach(symmetryCheckComboBox.getCheckModel()::check);
    symmetryCheckComboBox.getCheckModel()
                         .getCheckedItems()
                         .subscribe(() -> generatorPrefs.getSymmetries()
                                                        .setAll(
                                                            symmetryCheckComboBox.getCheckModel().getCheckedItems()));
  }

  protected void setStyles(List<String> styles) {
    mapStyleCheckComboBox.getItems().setAll(styles);
    ObservableList<String> mapStyles = generatorPrefs.getMapStyles();
    mapStyles.forEach(mapStyleCheckComboBox.getCheckModel()::check);

    mapStyleCheckComboBox.getCheckModel()
                         .getCheckedItems()
                         .subscribe(() -> generatorPrefs.getMapStyles()
                                                        .setAll(
                                                            mapStyleCheckComboBox.getCheckModel().getCheckedItems()));
  }

  protected void setTerrainStyles(List<String> terrainStyles) {
    terrainCheckComboBox.getItems().setAll(terrainStyles);
    ObservableList<String> savedTerrainStyles = generatorPrefs.getTerrainStyles();
    savedTerrainStyles.forEach(terrainCheckComboBox.getCheckModel()::check);

    terrainCheckComboBox.getCheckModel()
                        .getCheckedItems()
                        .subscribe(() -> generatorPrefs.getTerrainStyles()
                                                       .setAll(terrainCheckComboBox.getCheckModel().getCheckedItems()));
  }

  protected void setTextureStyles(List<String> textureStyles) {
    biomeCheckComboBox.getItems().setAll(textureStyles);
    ObservableList<String> savedTextureStyles = generatorPrefs.getTextureStyles();
    savedTextureStyles.forEach(biomeCheckComboBox.getCheckModel()::check);

    biomeCheckComboBox.getCheckModel()
                      .getCheckedItems()
                      .subscribe(() -> generatorPrefs.getTextureStyles()
                                                     .setAll(biomeCheckComboBox.getCheckModel().getCheckedItems()));
  }

  protected void setResourceStyles(List<String> resourceStyles) {
    resourcesCheckComboBox.getItems().setAll(resourceStyles);
    ObservableList<String> savedResourceStyles = generatorPrefs.getResourceStyles();
    savedResourceStyles.forEach(resourcesCheckComboBox.getCheckModel()::check);

    resourcesCheckComboBox.getCheckModel()
                          .getCheckedItems()
                          .subscribe(() -> generatorPrefs.getResourceStyles()
                                                         .setAll(
                                                             resourcesCheckComboBox.getCheckModel().getCheckedItems()));
  }

  protected void setPropStyles(List<String> propStyles) {
    propsCheckComboBox.getItems().setAll(propStyles);
    ObservableList<String> savedPropStyles = generatorPrefs.getPropStyles();
    savedPropStyles.forEach(propsCheckComboBox.getCheckModel()::check);

    propsCheckComboBox.getCheckModel()
                      .getCheckedItems()
                      .subscribe(() -> generatorPrefs.getPropStyles()
                                                     .setAll(propsCheckComboBox.getCheckModel().getCheckedItems()));
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

  public void onSeedRerollButtonClicked() {
    seedTextField.setText(String.valueOf(new Random().nextLong()));
  }

  private class LessMoreStringConverter extends ToStringOnlyConverter<Number> {
    public LessMoreStringConverter(Number min, Number max) {
      super(number -> {
        if (number.equals(max)) {
          return i18n.get("more");
        }

        if (number.equals(min)) {
          return i18n.get("less");
        }

        return "";
      });
    }
  }
}
