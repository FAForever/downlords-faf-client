package com.faforever.client.game;

import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MapFilterController implements Controller<Node> {

  public VBox mapFilterRoot;
  public TextField numberOfPlayersTextField;
  public TextField mapWidthInKmTextField;
  public TextField mapHeightInKmTextField;
  private TextField mapNameTextField;
  private final BooleanProperty filterAppliedProperty = new SimpleBooleanProperty(false);
  private FilteredList<MapVersionBean> filteredMaps;

  @Override
  public void initialize() {
    JavaFxUtil.makeNumericTextField(numberOfPlayersTextField, 2, false);
    JavaFxUtil.makeNumericTextField(mapWidthInKmTextField, 2, false);
    JavaFxUtil.makeNumericTextField(mapHeightInKmTextField, 2, false);
    JavaFxUtil.addListener(numberOfPlayersTextField.textProperty(), (observable, oldValue, newValue) -> runFilter());
    JavaFxUtil.addListener(mapWidthInKmTextField.textProperty(), (observable, oldValue, newValue) -> runFilter());
    JavaFxUtil.addListener(mapHeightInKmTextField.textProperty(), (observable, oldValue, newValue) -> runFilter());
  }

  public void setMapNameTextField(TextField textField) {
    mapNameTextField = textField;
    JavaFxUtil.addListener(mapNameTextField.textProperty(), (observable, oldValue, newValue) -> runFilter());
  }

  public void setFilteredMapList(FilteredList<MapVersionBean> filteredMaps) {
    this.filteredMaps = filteredMaps;
  }

  public BooleanProperty getFilterAppliedProperty() {
    return filterAppliedProperty;
  }

  public void runFilter() {
    filteredMaps.setPredicate((map) ->
        isEqualToNumberOfPlayers(map) && isEqualToMapWidth(map) && isEqualToMapHeight(map) && containsMapName(map));
    filterAppliedProperty.set(!mapNameTextField.getText().isEmpty() || !numberOfPlayersTextField.getText().isEmpty() ||
            !mapWidthInKmTextField.getText().isEmpty() || !mapHeightInKmTextField.getText().isEmpty());
  }

  private boolean containsMapName(MapVersionBean mapVersion) {
    String value = mapNameTextField.getText().toLowerCase();
    return mapVersion.getMap().getDisplayName().toLowerCase().contains(value) || mapVersion.getFolderName().toLowerCase().contains(value);
  }

  private boolean isEqualToNumberOfPlayers(MapVersionBean mapVersion) {
    String value = numberOfPlayersTextField.getText();
    return value.isEmpty() || mapVersion.getMaxPlayers() == Integer.parseInt(value);
  }

  private boolean isEqualToMapWidth(MapVersionBean mapVersion) {
    String value = mapWidthInKmTextField.getText();
    return value.isEmpty() || mapVersion.getSize().getWidthInKm() == Integer.parseInt(value);
  }

  private boolean isEqualToMapHeight(MapVersionBean mapVersion) {
    String value = mapHeightInKmTextField.getText();
    return value.isEmpty() || mapVersion.getSize().getHeightInKm() == Integer.parseInt(value);
  }

  @Override
  public Node getRoot() {
    return mapFilterRoot;
  }
}
