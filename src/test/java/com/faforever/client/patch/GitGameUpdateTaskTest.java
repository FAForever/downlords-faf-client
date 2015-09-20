package com.faforever.client.patch;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

public class GitGameUpdateTaskTest {

  private GitGameUpdateTask instance;

  @Before
  public void setUp() throws Exception {
    instance = new GitGameUpdateTask();
  }

  @Test
  public void testAvailable() throws Exception {
    instance.call();

    verify(gitWrapper).clone(GIT_PATCH_URL, binaryPatchRepoDirectory);
    verify(taskService).submitTask(any(), any());
  }
}
