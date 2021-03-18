package com.faforever.client.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;

import java.time.OffsetDateTime;

@Type("banInfo")
@Data
public class BanInfo {
  @Id
  private String id;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;
  @Relationship("player")
  @JsonIgnore
  private Player player;
  @Relationship("author")
  @JsonIgnore
  private Player author;
  private String reason;
  private OffsetDateTime expiresAt;
  private BanLevel level;
  @Relationship("moderationReport")
  @JsonIgnore
  private ModerationReport moderationReport;
  private String revokeReason;
  @Relationship("revokeAuthor")
  @JsonIgnore
  private Player revokeAuthor;
  private OffsetDateTime revokeTime;

  @JsonIgnore
  public BanDurationType getDuration() {
    return expiresAt == null ? BanDurationType.PERMANENT : BanDurationType.TEMPORARY;
  }

  @JsonIgnore
  public BanStatus getBanStatus() {
    if (revokeTime != null && revokeTime.isBefore(OffsetDateTime.now())) {
      return BanStatus.DISABLED;
    }
    if (getDuration() == BanDurationType.PERMANENT) {
      return BanStatus.BANNED;
    }
    return expiresAt.isAfter(OffsetDateTime.now())
        ? BanStatus.BANNED
        : BanStatus.EXPIRED;
  }
}
