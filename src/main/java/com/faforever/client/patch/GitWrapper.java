package com.faforever.client.patch;

import org.eclipse.jgit.lib.ProgressMonitor;

import java.io.IOException;
import java.nio.file.Path;

public interface GitWrapper {

  void clone(String repositoryUri, Path targetDirectory, ProgressMonitor progressMonitor);

  void fetch(Path repoDirectory, PropertiesProgressMonitor progressMonitor) throws IOException;

  void checkoutRef(Path repoDirectory, String ref);
}
