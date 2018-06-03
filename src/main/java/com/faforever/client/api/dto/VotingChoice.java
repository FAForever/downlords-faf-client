package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type(VotingChoice.TYPE_NAME)
public class VotingChoice {
  public static final String TYPE_NAME = "votingChoice";

  @Id
  private String id;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;
  private String choiceTextKey;
  private String choiceText;
  private String descriptionKey;
  private String description;
  private int numberOfAnswers;
  private int ordinal;
  @Relationship("votingQuestion")
  private VotingQuestion votingQuestion;
}
