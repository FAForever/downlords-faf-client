package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type(VotingQuestion.TYPE_NAME)
public class VotingQuestion {
  public static final String TYPE_NAME = "votingQuestion";

  @Id
  private String id;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;
  private int numberOfAnswers;
  private String question;
  private String description;
  private String questionKey;
  private String descriptionKey;
  private int maxAnswers;
  private VotingSubject votingSubject;
  @Relationship("votingChoices")
  private List<VotingChoice> votingChoices;
}
