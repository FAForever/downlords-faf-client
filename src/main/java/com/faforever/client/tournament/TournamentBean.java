package com.faforever.client.tournament;

import com.faforever.client.api.dto.Tournament;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.OffsetDateTime;

public class TournamentBean {
  private final StringProperty id;
  private final StringProperty name;
  private final StringProperty description;
  private final StringProperty tournamentType;
  private final IntegerProperty participantCount;
  private final ObjectProperty<OffsetDateTime> startedAt;
  private final ObjectProperty<OffsetDateTime> completedAt;
  private final StringProperty challongeUrl;
  private final StringProperty liveImageUrl;
  private final StringProperty signUpUrl;

  public TournamentBean() {
    id = new SimpleStringProperty();
    name = new SimpleStringProperty();
    description = new SimpleStringProperty();
    tournamentType = new SimpleStringProperty();
    participantCount = new SimpleIntegerProperty();
    startedAt = new SimpleObjectProperty<>();
    completedAt = new SimpleObjectProperty<>();
    challongeUrl = new SimpleStringProperty();
    liveImageUrl = new SimpleStringProperty();
    signUpUrl = new SimpleStringProperty();
  }


  public static TournamentBean fromTournamentDto(Tournament tournament) {
    TournamentBean tournamentBean = new TournamentBean();

    tournamentBean.setId(tournament.getId());
    tournamentBean.setName(tournament.getName());
    tournamentBean.setDescription(tournament.getDescription());
    tournamentBean.setTournamentType(tournament.getTournamentType());
    tournamentBean.setParticipantCount(tournament.getParticipantCount());
    tournamentBean.setStartedAt(tournament.getStartedAt());
    tournamentBean.setCompletedAt(tournament.getCompletedAt());
    tournamentBean.setChallongeUrl(tournament.getChallongeUrl());
    tournamentBean.setLiveImageUrl(tournament.getLiveImageUrl());
    tournamentBean.setSignUpUrl(tournament.getSignUpUrl());

    return tournamentBean;
  }

  public String getId() {
    return id.get();
  }

  public void setId(String id) {
    this.id.set(id);
  }

  public StringProperty idProperty() {
    return id;
  }

  public String getName() {
    return name.get();
  }

  public void setName(String name) {
    this.name.set(name);
  }

  public StringProperty nameProperty() {
    return name;
  }

  public String getDescription() {
    return description.get();
  }

  public void setDescription(String description) {
    this.description.set(description);
  }

  public StringProperty descriptionProperty() {
    return description;
  }

  public String getTournamentType() {
    return tournamentType.get();
  }

  public void setTournamentType(String tournamentType) {
    this.tournamentType.set(tournamentType);
  }

  public StringProperty tournamentTypeProperty() {
    return tournamentType;
  }

  public int getParticipantCount() {
    return participantCount.get();
  }

  public void setParticipantCount(int participantCount) {
    this.participantCount.set(participantCount);
  }

  public IntegerProperty participantCountProperty() {
    return participantCount;
  }

  public OffsetDateTime getStartedAt() {
    return startedAt.get();
  }

  public void setStartedAt(OffsetDateTime startedAt) {
    this.startedAt.set(startedAt);
  }

  public ObjectProperty<OffsetDateTime> startedAtProperty() {
    return startedAt;
  }

  public OffsetDateTime getCompletedAt() {
    return completedAt.get();
  }

  public void setCompletedAt(OffsetDateTime completedAt) {
    this.completedAt.set(completedAt);
  }

  public ObjectProperty<OffsetDateTime> completedAtProperty() {
    return completedAt;
  }

  public String getChallongeUrl() {
    return challongeUrl.get();
  }

  public void setChallongeUrl(String url) {
    this.challongeUrl.set(url);
  }

  public StringProperty challongeUrlProperty() {
    return challongeUrl;
  }

  public String getLiveImageUrl() {
    return liveImageUrl.get();
  }

  public void setLiveImageUrl(String liveImageUrl) {
    this.liveImageUrl.set(liveImageUrl);
  }

  public StringProperty liveImageUrlProperty() {
    return liveImageUrl;
  }

  public String getSignUpUrl() {
    return signUpUrl.get();
  }

  public void setSignUpUrl(String signUpUrl) {
    this.signUpUrl.set(signUpUrl);
  }

  public StringProperty signUpUrlProperty() {
    return signUpUrl;
  }
}
