package com.faforever.client.patch;


import com.faforever.commons.mod.MountInfo;
import lombok.Data;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.List;

@Data
public class PatchResult {
  private final ComparableVersion version;
  private final List<MountInfo> mountInfos;
  private final List<String> hookDirectories;
}
