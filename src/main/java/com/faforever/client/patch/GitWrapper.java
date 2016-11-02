package com.faforever.client.patch;

import java.io.IOException;
import java.nio.file.Path;

public interface GitWrapper {

  void clone(String repositoryUri, Path targetDirectory);

  String getRemoteHead(Path repoDirectory) throws IOException;

  String getLocalHead(Path repoDirectory) throws IOException;

  void fetch(Path repoDirectory) throws IOException;

  void clean(Path repoDirectory);

  void reset(Path repoDirectory);

  void checkoutTag(Path repoDirectory, String tagName);
}
