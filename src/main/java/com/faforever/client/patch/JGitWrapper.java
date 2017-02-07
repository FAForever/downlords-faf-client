package com.faforever.client.patch;

import com.faforever.client.task.ResourceLocks;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

import static org.eclipse.jgit.api.Git.cloneRepository;
import static org.eclipse.jgit.api.Git.open;

@Lazy
@Component
@Slf4j
public class JGitWrapper implements GitWrapper {

  @Override
  @SneakyThrows
  public void clone(String repositoryUri, Path targetDirectory, ProgressMonitor progressMonitor) {
    ResourceLocks.acquireDownloadLock();
    ResourceLocks.acquireDiskLock();
    try {
      log.debug("Cloning {} into {}", repositoryUri, targetDirectory);
      cloneRepository()
          .setProgressMonitor(progressMonitor)
          .setURI(repositoryUri)
          .setDirectory(targetDirectory.toFile())
          .call();
    } finally {
      ResourceLocks.freeDiskLock();
      ResourceLocks.freeDownloadLock();
    }
  }

  @Override
  @SneakyThrows
  public void fetch(Path repoDirectory, PropertiesProgressMonitor progressMonitor) {
    ResourceLocks.acquireDownloadLock();
    ResourceLocks.acquireDiskLock();
    try {
      log.debug("Fetching into {}", repoDirectory);
      try (Git git = open(repoDirectory.toFile())) {
        git.fetch()
            .setProgressMonitor(progressMonitor)
            .call();
      }
    } finally {
      ResourceLocks.freeDiskLock();
      ResourceLocks.freeDownloadLock();
    }
  }

  @Override
  @SneakyThrows
  public void checkoutRef(Path repoDirectory, String ref) {
    ResourceLocks.acquireDiskLock();
    try {
      try (Git git = open(repoDirectory.toFile())) {
        git.checkout()
            .setForce(true)
            .setName(ref)
            .call();
      }
    } finally {
      ResourceLocks.freeDiskLock();
    }
  }
}
