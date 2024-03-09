package com.faforever.client.leaderboard;

import com.faforever.client.domain.api.Division;
import com.faforever.client.domain.api.LeagueEntry;
import com.faforever.client.domain.api.Subdivision;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.ObservableConstant;
import com.faforever.client.fx.StringCell;
import com.faforever.client.fx.ToStringOnlyConverter;
import com.faforever.client.fx.contextmenu.AddFoeMenuItem;
import com.faforever.client.fx.contextmenu.AddFriendMenuItem;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.CopyUsernameMenuItem;
import com.faforever.client.fx.contextmenu.RemoveFoeMenuItem;
import com.faforever.client.fx.contextmenu.RemoveFriendMenuItem;
import com.faforever.client.fx.contextmenu.ShowPlayerInfoMenuItem;
import com.faforever.client.fx.contextmenu.ViewReplaysMenuItem;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import impl.org.controlsfx.autocompletion.SuggestionProvider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LeaderboardRankingsController extends NodeController<VBox> {

  private final I18n i18n;
  private final PlayerService playerService;
  private final ContextMenuBuilder contextMenuBuilder;

  public VBox rankingsRoot;
  public HBox subdivisionButtons;
  public ToggleGroup subdivisionToggleGroup;
  public TextField searchTextField;
  public ComboBox<Division> divisionPicker;
  public TableColumn<LeagueEntry, Number> rankColumn;
  public TableColumn<LeagueEntry, String> nameColumn;
  public TableColumn<LeagueEntry, Number> gamesPlayedColumn;
  public TableColumn<LeagueEntry, Integer> scoreColumn;
  public TableView<LeagueEntry> ratingTable;

  private final ObjectProperty<List<LeagueEntry>> leagueEntries = new SimpleObjectProperty<>(List.of());
  private final ObjectProperty<List<Subdivision>> subdivisions = new SimpleObjectProperty<>(List.of());
  private final ObjectProperty<LeagueEntry> selectedLeagueEntry = new SimpleObjectProperty<>();
  private final SuggestionProvider<String> usernameSuggestionProvider = SuggestionProvider.create(List.of());
  private final Map<Toggle, Subdivision> toggleSubdivisionMap = new HashMap<>();
  private final Map<Subdivision, Toggle> subdivisionToggleMap = new HashMap<>();

  @Override
  protected void onInitialize() {
    searchTextField.setPromptText(i18n.get("leaderboard.searchPrompt").toUpperCase());

    subdivisionButtons.prefWidthProperty().bind(ratingTable.widthProperty());

    divisionPicker.setConverter(new ToStringOnlyConverter<>(
        division -> i18n.get("leagues.divisionName.%s".formatted(division.nameKey())).toUpperCase()));

    subdivisions.map(this::getDivisions).map(FXCollections::observableList).when(showing).subscribe(divisions -> {
      divisionPicker.setItems(divisions);
      Division selectedDivision = selectedLeagueEntry.map(LeagueEntry::subdivision)
                                                     .map(Subdivision::division)
                                                     .getValue();
      divisions.stream()
               .filter(division -> division.equals(selectedDivision))
               .findFirst()
               .ifPresentOrElse(division -> divisionPicker.getSelectionModel().select(division),
                                () -> divisionPicker.getSelectionModel().selectFirst());
    });

    AutoCompletionBinding<String> usernamesAutoCompletion = TextFields.bindAutoCompletion(searchTextField,
                                                                                          usernameSuggestionProvider);
    usernamesAutoCompletion.setDelay(0);
    usernamesAutoCompletion.setOnAutoCompleted(event -> processSearchInput());

    leagueEntries.map(this::getPlayerNames).when(showing).subscribe(newNames -> {
      usernameSuggestionProvider.clearSuggestions();
      usernameSuggestionProvider.addPossibleSuggestions(newNames);
    });

    ObservableValue<List<Subdivision>> selectedSubdivisions = subdivisions.flatMap(
        subdivisions -> divisionPicker.getSelectionModel()
                                      .selectedItemProperty()
                                      .map(division -> subdivisions.stream()
                                                                   .filter(subdivision -> division.equals(
                                                                       subdivision.division()))
                                                                   .toList())).orElse(List.of());

    selectedSubdivisions.when(showing).subscribe(this::createSubdivisionButtons);

    subdivisionToggleGroup.selectedToggleProperty().when(showing).subscribe((oldToggle, newToggle) -> {
      if (newToggle == null) {
        subdivisionToggleGroup.selectToggle(oldToggle);
      }
    });

    leagueEntries.flatMap(leagueEntries -> subdivisionToggleGroup.selectedToggleProperty()
                                                                 .map(toggleSubdivisionMap::get)
                                                                 .map(subdivision -> leagueEntries.stream()
                                                                                                  .filter(
                                                                                                      leagueEntry1 -> leagueEntry1.subdivision()
                                                                                                                                  .equals(
                                                                                                                                      subdivision))
                                                                                                  .toList()))
                 .orElse(List.of())
                 .map(FXCollections::observableList)
                 .when(showing)
                 .subscribe(leagueEntries -> {
                   ratingTable.setItems(leagueEntries);
                   ratingTable.getSelectionModel().select(selectedLeagueEntry.get());
                 });

    leagueEntries.when(showing)
                 .subscribe(leagueEntries -> leagueEntries.stream()
                                                          .filter(leagueEntry -> playerService.getCurrentPlayer()
                                                                                              .equals(
                                                                                                  leagueEntry.player()))
                                                          .findFirst()
                                                          .ifPresentOrElse(selectedLeagueEntry::set,
                                                                           () -> selectedLeagueEntry.set(null)));

    selectedLeagueEntry.when(showing).subscribe(leagueEntry -> {
      if (leagueEntry != null) {
        divisionPicker.getSelectionModel().select(leagueEntry.subdivision().division());
        subdivisionToggleGroup.selectToggle(subdivisionToggleMap.get(leagueEntry.subdivision()));
        ratingTable.scrollTo(leagueEntry);
        ratingTable.getSelectionModel().select(leagueEntry);
      }
    });

    ratingTable.setRowFactory(param -> entriesRowFactory());

    rankColumn.setCellValueFactory(param -> ObservableConstant.valueOf(param.getValue().rank()));
    rankColumn.setCellFactory(param -> new StringCell<>(rank -> i18n.number(rank.intValue())));

    nameColumn.setCellValueFactory(param -> param.getValue().player().usernameProperty());
    nameColumn.setCellFactory(param -> new StringCell<>(name -> name));
    nameColumn.prefWidthProperty().bind(ratingTable.widthProperty().subtract(250));

    gamesPlayedColumn.setCellValueFactory(param -> ObservableConstant.valueOf(param.getValue().gamesPlayed()));
    gamesPlayedColumn.setCellFactory(param -> new StringCell<>(count -> i18n.number(count.intValue())));

    scoreColumn.setCellValueFactory(param -> ObservableConstant.valueOf(param.getValue().score()));
    scoreColumn.setCellFactory(param -> new StringCell<>(i18n::number));
  }

  private void createSubdivisionButtons(List<Subdivision> subdivisions) {
    subdivisionToggleGroup.getToggles().clear();
    toggleSubdivisionMap.clear();
    subdivisionToggleMap.clear();
    subdivisionButtons.getChildren().clear();

    if (subdivisions.isEmpty()) {
      return;
    }

    subdivisions.forEach(subdivision -> {
      ToggleButton toggleButton = new ToggleButton(subdivision.nameKey());
      toggleButton.getStyleClass().add("main-navigation-button");
      toggleButton.prefWidthProperty().bind(ratingTable.widthProperty().divide(subdivisions.size()));
      subdivisionToggleMap.put(subdivision, toggleButton);
      toggleSubdivisionMap.put(toggleButton, subdivision);
      subdivisionToggleGroup.getToggles().add(toggleButton);
      subdivisionButtons.getChildren().add(toggleButton);
    });

    Subdivision selectedSubdivision = selectedLeagueEntry.map(LeagueEntry::subdivision).getValue();
    subdivisions.stream()
                .filter(subdivision -> subdivision.equals(selectedSubdivision))
                .findFirst()
                .ifPresentOrElse(
                    subdivision -> subdivisionToggleGroup.selectToggle(subdivisionToggleMap.get(subdivision)),
                    () -> subdivisionToggleGroup.selectToggle(subdivisionToggleMap.get(subdivisions.getLast())));
  }

  private TableRow<LeagueEntry> entriesRowFactory() {
    TableRow<LeagueEntry> row = new TableRow<>();
    row.setOnContextMenuRequested(event -> {
      LeagueEntry leagueEntry = row.getItem();
      if (leagueEntry == null) {
        return;
      }
      PlayerInfo player = leagueEntry.player();
      contextMenuBuilder.newBuilder()
                        .addItem(ShowPlayerInfoMenuItem.class, player)
                        .addItem(CopyUsernameMenuItem.class, player.getUsername())
                        .addSeparator()
                        .addItem(AddFriendMenuItem.class, player)
                        .addItem(RemoveFriendMenuItem.class, player)
                        .addItem(AddFoeMenuItem.class, player)
                        .addItem(RemoveFoeMenuItem.class, player)
                        .addSeparator()
                        .addItem(ViewReplaysMenuItem.class, player)
                        .build()
                        .show(getRoot().getScene().getWindow(), event.getScreenX(), event.getScreenY());
    });

    return row;
  }

  private Set<String> getPlayerNames(List<LeagueEntry> entries) {
    return entries.stream().map(LeagueEntry::player).map(PlayerInfo::getUsername).collect(Collectors.toSet());
  }

  private List<Division> getDivisions(List<Subdivision> subdivisions) {
    return subdivisions.stream()
                       .map(Subdivision::division)
                       .distinct()
                       .sorted(Comparator.comparing(Division::index).reversed())
                       .toList();
  }

  public void processSearchInput() {
    String searchedUsername = searchTextField.getText();
    if (searchedUsername.isBlank()) {
      return;
    }

    leagueEntries.getValue()
                 .stream().filter(leagueEntry -> leagueEntry.player().getUsername().equals(searchedUsername))
                 .findFirst()
                 .ifPresent(selectedLeagueEntry::set);
  }

  @Override
  public VBox getRoot() {
    return rankingsRoot;
  }

  public List<LeagueEntry> getLeagueEntries() {
    return leagueEntries.get();
  }

  public ObjectProperty<List<LeagueEntry>> leagueEntriesProperty() {
    return leagueEntries;
  }

  public void setLeagueEntries(List<LeagueEntry> leagueEntries) {
    this.leagueEntries.set(leagueEntries);
  }

  public List<Subdivision> getSubdivisions() {
    return subdivisions.get();
  }

  public ObjectProperty<List<Subdivision>> subdivisionsProperty() {
    return subdivisions;
  }

  public void setSubdivisions(List<Subdivision> subdivisions) {
    this.subdivisions.set(subdivisions);
  }
}
