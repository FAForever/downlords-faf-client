package com.faforever.client.builders;

import com.faforever.client.domain.TournamentBean;

import java.time.OffsetDateTime;


public class TournamentBeanBuilder {
  public static TournamentBeanBuilder create() {
    return new TournamentBeanBuilder();
  }

  private final TournamentBean tournamentBean = new TournamentBean();

  public TournamentBeanBuilder defaultValues() {
    id(0);
    name("test");
    description("test tourney");
    tournamentType("FFA");
    return this;
  }

  public TournamentBeanBuilder id(Integer id) {
    tournamentBean.setId(id);
    return this;
  }

  public TournamentBeanBuilder name(String name) {
    tournamentBean.setName(name);
    return this;
  }

  public TournamentBeanBuilder description(String description) {
    tournamentBean.setDescription(description);
    return this;
  }

  public TournamentBeanBuilder tournamentType(String tournamentType) {
    tournamentBean.setTournamentType(tournamentType);
    return this;
  }

  public TournamentBeanBuilder createdAt(OffsetDateTime createdAt) {
    tournamentBean.setCreatedAt(createdAt);
    return this;
  }

  public TournamentBeanBuilder participantCount(int participantCount) {
    tournamentBean.setParticipantCount(participantCount);
    return this;
  }

  public TournamentBeanBuilder startingAt(OffsetDateTime startingAt) {
    tournamentBean.setStartingAt(startingAt);
    return this;
  }

  public TournamentBeanBuilder completedAt(OffsetDateTime completedAt) {
    tournamentBean.setCompletedAt(completedAt);
    return this;
  }

  public TournamentBeanBuilder challongeUrl(String challongeUrl) {
    tournamentBean.setChallongeUrl(challongeUrl);
    return this;
  }

  public TournamentBeanBuilder liveImageUrl(String liveImageUrl) {
    tournamentBean.setLiveImageUrl(liveImageUrl);
    return this;
  }

  public TournamentBeanBuilder signUpUrl(String signUpUrl) {
    tournamentBean.setSignUpUrl(signUpUrl);
    return this;
  }

  public TournamentBeanBuilder openForSignup(boolean openForSignup) {
    tournamentBean.setOpenForSignup(openForSignup);
    return this;
  }

  public TournamentBean get() {
    return tournamentBean;
  }

}

