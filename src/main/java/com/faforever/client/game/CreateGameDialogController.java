package com.faforever.client.game;

import com.faforever.client.map.MapService;
import com.faforever.client.preferences.PreferencesService;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import org.controlsfx.control.RangeSlider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Objects;

public class CreateGameDialogController {

  @Autowired
  PreferencesService preferenceService;

  public interface OnCreateGameListener {

    void onCreateGame(NewGameInfo newGameInfo);
  }

  @FXML
  public TextField titleTextField;

  @FXML
  public TextField passwordTextField;

  @FXML
  public ComboBox<ModInfoBean> modComboBox;

  @FXML
  public ComboBox<MapInfoBean> mapComboBox;

  @FXML
  public TextField minRankingTextField;

  @FXML
  public RangeSlider rankingSlider;

  @FXML
  public TextField maxRankingTextField;

  @FXML
  private Region createGameRoot;

  @Autowired
  MapService mapService;

  private OnCreateGameListener onGameCreateListener;

  public Region getRoot() {
    return createGameRoot;
  }

  @FXML
  void initialize() {
    rankingSlider.setFocusTraversable(false);
    rankingSlider.lowValueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.intValue() < rankingSlider.getHighValue()) {
        minRankingTextField.setText(String.valueOf(newValue.intValue()));
      }
    });
    rankingSlider.highValueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.intValue() > rankingSlider.getLowValue()) {
        maxRankingTextField.setText(String.valueOf(newValue.intValue()));
      }
    });
    minRankingTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      rankingSlider.setLowValue(Double.parseDouble(newValue));
    });
    maxRankingTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      rankingSlider.setHighValue(Double.parseDouble(newValue));
    });

    rankingSlider.setMax(2500);
    rankingSlider.setMin(0);
    rankingSlider.setHighValue(1300);
    rankingSlider.setLowValue(800);

    mapComboBox.setButtonCell(new ListCell<MapInfoBean>() {
      @Override
      protected void updateItem(MapInfoBean item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          return;
        }

        Image mapPreview = mapService.loadSmallPreview(item.getName());
        setGraphic(new ImageView(mapPreview));
        setText(item.getName());
      }
    });

    modComboBox.setButtonCell(new ListCell<ModInfoBean>() {
      @Override
      protected void updateItem(ModInfoBean item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          return;
        }

        setText(item.getFullName());
      }
    });
  }

  public void setMods(Collection<ModInfoBean> knownMods) {
    modComboBox.getItems().setAll(knownMods);
    modComboBox.setCellFactory(param -> new ModListCell());

    String lastGameMod = preferenceService.getPreferences().getLastGameMod();
    for (ModInfoBean mod : knownMods) {
      if (Objects.equals(mod.getName(), lastGameMod)) {
        modComboBox.getSelectionModel().select(mod);
        break;
      }
    }
  }


  public void setMaps(ObservableList<MapInfoBean> maps) {
    mapComboBox.getItems().setAll(maps);
    mapComboBox.setCellFactory(param -> new MapListCell());

    String lastMap = preferenceService.getPreferences().getLastMap();
    for (MapInfoBean map : maps) {
      if (Objects.equals(map.getName(), lastMap)) {
        mapComboBox.getSelectionModel().select(map);
        break;
      }
    }
  }

  public void onCreateGameButtonClicked(ActionEvent actionEvent) {
    onGameCreateListener.onCreateGame(
        new NewGameInfo(
            titleTextField.getText(),
            passwordTextField.getText(),
            modComboBox.getValue().getName(),
            mapComboBox.getValue().getName()
        )
    );
  }

  public void setOnGameCreateListener(OnCreateGameListener listener) {
    onGameCreateListener = listener;
  }
}
