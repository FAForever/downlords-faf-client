package com.faforever.client.domain;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

import java.time.OffsetDateTime;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Value
public class TournamentBean {
  @EqualsAndHashCode.Include
  ObjectProperty<Integer> id = new SimpleObjectProperty<>();
  StringProperty name = new SimpleStringProperty();
  StringProperty description = new SimpleStringProperty();
  StringProperty tournamentType = new SimpleStringProperty();
  ObjectProperty<OffsetDateTime> createdAt = new SimpleObjectProperty<>();
  IntegerProperty participantCount = new SimpleIntegerProperty();
  ObjectProperty<OffsetDateTime> startingAt = new SimpleObjectProperty<>();
  ObjectProperty<OffsetDateTime> completedAt = new SimpleObjectProperty<>();
  StringProperty challongeUrl = new SimpleStringProperty();
  StringProperty liveImageUrl = new SimpleStringProperty();
  StringProperty signUpUrl = new SimpleStringProperty();
  BooleanProperty openForSignup = new SimpleBooleanProperty();
  ObjectProperty<Status> status = new SimpleObjectProperty<>();

  public TournamentBean() {
    status.bind(Bindings.createObjectBinding(() -> {
      if (getCompletedAt() != null) {
        return Status.FINISHED;
      } else if (getStartingAt() != null && getStartingAt().isBefore(OffsetDateTime.now())) {
        return Status.RUNNING;
      } else if (getOpenForSignup()) {
        return Status.OPEN_FOR_REGISTRATION;
      } else {
        return Status.CLOSED_FOR_REGISTRATION;
      }
    }, startingAt, completedAt, openForSignup));
  }

  public Integer getId() {
    return id.get();
  }

  public void setId(Integer id) {
    this.id.set(id);
  }

  public ObjectProperty<Integer> idProperty() {
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

  public OffsetDateTime getCreatedAt() {
    return createdAt.get();
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt.set(createdAt);
  }

  public ObjectProperty<OffsetDateTime> createdAtProperty() {
    return createdAt;
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

  public OffsetDateTime getStartingAt() {
    return startingAt.get();
  }

  public void setStartingAt(OffsetDateTime startingAt) {
    this.startingAt.set(startingAt);
  }

  public ObjectProperty<OffsetDateTime> startingAtProperty() {
    return startingAt;
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

  public boolean getOpenForSignup() {
    return openForSignup.get();
  }

  public void setOpenForSignup(boolean openForSignup) {
    this.openForSignup.set(openForSignup);
  }

  public BooleanProperty openForSignupProperty() {
    return openForSignup;
  }

  public Status getStatus() {
    return status.get();
  }

  public ObjectProperty<Status> statusProperty() {
    return status;
  }

  @AllArgsConstructor
  @Getter
  public enum Status {
    FINISHED("tournament.status.finished", 1),
    RUNNING("tournament.status.running", 2),
    OPEN_FOR_REGISTRATION("tournament.status.openForRegistration", 4),
    CLOSED_FOR_REGISTRATION("tournament.status.closedForRegistration", 3);

    private final String messageKey;
    private final int sortOrderPriority;
  }
}
