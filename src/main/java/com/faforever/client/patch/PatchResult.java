package com.faforever.client.patch;


import org.apache.maven.artifact.versioning.ComparableVersion;

import java.nio.file.Path;

public record PatchResult(ComparableVersion version, Path initFile) {}
