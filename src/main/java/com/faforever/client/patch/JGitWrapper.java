package com.faforever.client.patch;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;

import static com.github.nocatch.NoCatch.noCatch;
import static org.eclipse.jgit.api.Git.cloneRepository;
import static org.eclipse.jgit.api.Git.open;
import static org.eclipse.jgit.lib.Constants.HEAD;

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
  public String getRemoteHead(Path repoDirectory) {
    return noCatch(() -> {
      Git git = open(repoDirectory.toFile());
      String remoteHead = null;
      for (Ref ref : git.lsRemote().call()) {
        if (HEAD.equals(ref.getName())) {
          remoteHead = ref.getObjectId().name();
          break;
        }
      }
      return remoteHead;
    });
  }

  @Override
  public String getLocalHead(Path repoDirectory) {
    return noCatch(() -> {
      Git git = open(repoDirectory.toFile());

      ObjectId head = git.getRepository().resolve(HEAD);
      if (head == null) {
        return null;
      }

      return head.name();
    });
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
  public void checkoutTag(Path repoDirectory, String tagName) {
    noCatch(() -> open(repoDirectory.toFile())
        .checkout()
        .setName(tagName)
        .getResult());
  }
}
