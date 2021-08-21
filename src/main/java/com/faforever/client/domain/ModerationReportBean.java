package com.faforever.client.domain;

import com.faforever.commons.api.dto.ModerationReportStatus;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Value
public class ModerationReportBean extends AbstractEntityBean<ModerationReportBean>  {
  StringProperty reportDescription = new SimpleStringProperty();
  ObjectProperty<ModerationReportStatus> reportStatus = new SimpleObjectProperty<>();
  StringProperty gameIncidentTimeCode = new SimpleStringProperty();
  StringProperty moderatorNotice = new SimpleStringProperty();
  ObjectProperty<PlayerBean> lastModerator = new SimpleObjectProperty<>();
  ObjectProperty<PlayerBean> reporter = new SimpleObjectProperty<>();
  SetProperty<PlayerBean> reportedUsers = new SimpleSetProperty<>(FXCollections.observableSet());
  ObjectProperty<ReplayBean> game = new SimpleObjectProperty<>();

  public ModerationReportStatus getReportStatus() {
    return reportStatus.get();
  }

  public void setReportStatus(ModerationReportStatus reportStatus) {
    this.reportStatus.set(reportStatus);
  }

  public ObjectProperty<ModerationReportStatus> reportStatusProperty() {
    return reportStatus;
  }

  public String getReportDescription() {
    return reportDescription.get();
  }

  public void setReportDescription(String reportDescription) {
    this.reportDescription.set(reportDescription);
  }

  public StringProperty reportDescriptionProperty() {
    return reportDescription;
  }

  public String getGameIncidentTimeCode() {
    return gameIncidentTimeCode.get();
  }

  public void setGameIncidentTimeCode(String gameIncidentTimeCode) {
    this.gameIncidentTimeCode.set(gameIncidentTimeCode);
  }

  public StringProperty gameIncidentTimeCodeProperty() {
    return gameIncidentTimeCode;
  }

  public String getModeratorNotice() {
    return moderatorNotice.get();
  }

  public void setModeratorNotice(String moderatorNotice) {
    this.moderatorNotice.set(moderatorNotice);
  }

  public StringProperty moderatorNoticeProperty() {
    return moderatorNotice;
  }

  public PlayerBean getLastModerator() {
    return lastModerator.get();
  }

  public void setLastModerator(PlayerBean lastModerator) {
    this.lastModerator.set(lastModerator);
  }

  public ObjectProperty<PlayerBean> lastModeratorProperty() {
    return lastModerator;
  }

  public PlayerBean getReporter() {
    return reporter.get();
  }

  public void setReporter(PlayerBean reporter) {
    this.reporter.set(reporter);
  }

  public ObjectProperty<PlayerBean> reporterProperty() {
    return reporter;
  }

  public ObservableSet<PlayerBean> getReportedUsers() {
    return reportedUsers.get();
  }

  public SetProperty<PlayerBean> reportedUsersProperty() {
    return reportedUsers;
  }

  public void setReportedUsers(Set<PlayerBean> reportedUsers) {
    this.reportedUsers.set(FXCollections.observableSet(reportedUsers));
  }

  public ReplayBean getGame() {
    return game.get();
  }

  public void setGame(ReplayBean game) {
    this.game.set(game);
  }

  public ObjectProperty<ReplayBean> gameProperty() {
    return game;
  }
}
