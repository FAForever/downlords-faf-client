package com.faforever.client.patch;


import com.faforever.commons.mod.MountInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

@Data
public class PatchResult {
  /** Only set if mod_info.lua was available. */
  @Nullable
  private final ComparableVersion version;

  /** Only set if mod_info.lua was available. */
  @Nullable
  private final List<MountInfo> mountInfos;

  /** Only set if mod_info.lua was available. */
  @Nullable
  private final List<String> hookDirectories;

  /** Only set if no mod_info.lua was available. */
  @Nullable
  private final Path legacyInitFile;

  private PatchResult(@Nullable ComparableVersion version, @Nullable List<MountInfo> mountInfos, @Nullable List<String> hookDirectories, @Nullable Path legacyInitFile) {
    this.version = version;
    this.mountInfos = mountInfos;
    this.hookDirectories = hookDirectories;
    this.legacyInitFile = legacyInitFile;
  }

  public static PatchResult fromModInfo(ComparableVersion version, List<MountInfo> mountInfos, List<String> hookDirectories) {
    return new PatchResult(version, mountInfos, hookDirectories, null);
  }

  public static PatchResult withLegacyInitFile(ComparableVersion version, Path legacyInitFile) {
    return new PatchResult(version, null, null, legacyInitFile);
  }
}
