package com.faforever.client.patch;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;

public class JGitWrapper implements GitWrapper {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void clone(String repositoryUri, Path targetDirectory) {
    logger.debug("Cloning {} into {}", repositoryUri, targetDirectory);

    try {
      Git.cloneRepository()
          .setURI(repositoryUri)
          .setDirectory(targetDirectory.toFile())
          .call();
    } catch (GitAPIException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getRemoteHead(Path repoDirectory) throws IOException {
    Git git = Git.open(repoDirectory.toFile());
    String remoteHead = null;
    try {
      for (Ref ref : git.lsRemote().call()) {
        if (Constants.HEAD.equals(ref.getName())) {
          remoteHead = ref.getObjectId().name();
          break;
        }
      }
    } catch (GitAPIException e) {
      throw new IOException(e);
    }
    return remoteHead;
  }

  @Override
  public String getLocalHead(Path repoDirectory) throws IOException {
    Git git = Git.open(repoDirectory.toFile());

    ObjectId head = git.getRepository().resolve(Constants.HEAD);
    if (head == null) {
      return null;
    }

    return head.name();
  }
}
