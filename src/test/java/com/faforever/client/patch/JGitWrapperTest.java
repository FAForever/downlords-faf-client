package com.faforever.client.patch;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JGitWrapperTest {

  @Test
  public void testBasicCheckout() {
    Path cloneDirectory = Paths.get("build/git");

    JGitWrapper jGitWrapper = new JGitWrapper();

    if (Files.notExists(cloneDirectory)) {
      jGitWrapper.clone("https://github.com/FAForever/fa.git", cloneDirectory);
    }
    jGitWrapper.fetch(cloneDirectory);
    jGitWrapper.checkoutTag(cloneDirectory, "3660");
  }
}
