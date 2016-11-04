package com.faforever.client.patch;

import java.io.IOException;
import java.nio.file.Path;

public interface GitWrapper {

  void clone(String repositoryUri, Path targetDirectory);

  void fetch(Path repoDirectory) throws IOException;

  void clean(Path repoDirectory);

  void reset(Path repoDirectory);

  void checkoutRef(Path repoDirectory, String ref);
}
