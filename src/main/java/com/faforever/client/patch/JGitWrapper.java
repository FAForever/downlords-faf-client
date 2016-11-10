package com.faforever.client.patch;

import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;

import static com.github.nocatch.NoCatch.noCatch;
import static org.eclipse.jgit.api.Git.cloneRepository;
import static org.eclipse.jgit.api.Git.open;

public class JGitWrapper implements GitWrapper {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void clone(String repositoryUri, Path targetDirectory) {
    logger.debug("Cloning {} into {}", repositoryUri, targetDirectory);

    noCatch(() -> cloneRepository()
        .setURI(repositoryUri)
        .setDirectory(targetDirectory.toFile())
        .call());
  }

  @Override
  public void fetch(Path repoDirectory) {
    noCatch(() -> open(repoDirectory.toFile())
        .fetch()
        .call());
  }

  @Override
  public void clean(Path repoDirectory) {
    noCatch(() -> open(repoDirectory.toFile())
        .clean()
        .setCleanDirectories(true)
        .call());
  }

  @Override
  public void reset(Path repoDirectory) {
    noCatch(() -> open(repoDirectory.toFile())
        .reset()
        .setMode(ResetType.HARD)
        .call());
  }

  @Override
  public void checkoutRef(Path repoDirectory, String ref) {
    noCatch(() -> open(repoDirectory.toFile())
        .checkout()
        .setName(ref)
        .call());
  }
}
