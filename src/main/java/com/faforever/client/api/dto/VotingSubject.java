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
@Type(VotingSubject.TYPE_NAME)
public class VotingSubject {
  public static final String TYPE_NAME = "votingSubject";

  @Id
  private String id;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;
  private String subjectKey;
  private String subject;
  private int numberOfVotes;
  private String topicUrl;
  private OffsetDateTime beginOfVoteTime;
  private OffsetDateTime endOfVoteTime;
  private int minGamesToVote;
  private String descriptionKey;
  private String description;
  @Relationship("votingQuestions")
  private List<VotingQuestion> votingQuestions;
}
