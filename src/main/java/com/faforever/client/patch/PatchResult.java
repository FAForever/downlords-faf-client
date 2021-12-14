package com.faforever.client.patch;


import lombok.Value;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.nio.file.Path;

@Value
public class PatchResult {
  ComparableVersion version;
  Path initFile;
}
