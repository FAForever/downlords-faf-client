package com.faforever.client.tournament;


import com.faforever.client.fx.AbstractViewController;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TournamentsController extends AbstractViewController<Node> {
  private final TournamentService tournamentService;

  public TableView<TournamentBean> tournamentsTable;
  public TableColumn<TournamentBean, String> nameColumn;
  public TableColumn<TournamentBean, OffsetDateTime> startDateColumn;
  public TableColumn<TournamentBean, OffsetDateTime> endDateColumn;
  public TableColumn<TournamentBean, Number> participantCountColumn;
  public TableColumn<TournamentBean, String> gameModeColumn;
  public TableColumn<TournamentBean, String> detailsColumn;

  public TournamentsController(TournamentService tournamentService) {
    this.tournamentService = tournamentService;
  }

  @Override
  public Node getRoot() {
    return tournamentsTable;
  }

  @Override
  public void initialize() {
    super.initialize();

    nameColumn.setCellValueFactory(param -> param.getValue().nameProperty());
    startDateColumn.setCellValueFactory(param -> param.getValue().startedAtProperty());
    endDateColumn.setCellValueFactory(param -> param.getValue().completedAtProperty());
    participantCountColumn.setCellValueFactory(param -> param.getValue().participantCountProperty());
    gameModeColumn.setCellValueFactory(param -> param.getValue().tournamentTypeProperty());
    detailsColumn.setCellValueFactory(param -> param.getValue().challongeUrlProperty());

    tournamentService.getAllTournaments()
        .thenAccept(tournamentBeans -> tournamentsTable.getItems().addAll(tournamentBeans));
  }
}
