package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("mapVersion")
public class MapVersion {

  @Id
  private String id;
  private String description;
  private Integer maxPlayers;
  private Integer width;
  private Integer height;
  private ComparableVersion version;
  private String folderName;
  // TODO name consistently with folderName
  private String filename;
  private Boolean ranked;
  private Boolean hidden;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;
  private URL thumbnailUrlSmall;
  private URL thumbnailUrlLarge;
  private URL downloadUrl;

  @Relationship("map")
  private Map map;

  @Relationship("statistics")
  private MapVersionStatistics statistics;

  @Relationship("reviews")
  private List<MapVersionReview> reviews;
}
