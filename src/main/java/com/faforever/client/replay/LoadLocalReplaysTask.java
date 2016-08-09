package com.faforever.client.replay;

import com.faforever.client.i18n.I18n;
import com.faforever.client.task.CompletableTask;

import javax.annotation.Resource;
import java.util.Collection;

public class LoadLocalReplaysTask extends CompletableTask<Collection<ReplayInfoBean>> {

  @Resource
  ReplayService replayService;

  @Resource
  I18n i18n;


  public LoadLocalReplaysTask() {
    super(Priority.HIGH);
  }

  @Override
  protected Collection<ReplayInfoBean> call() throws Exception {
    updateTitle(i18n.get("replays.loadingLocalTask.title"));
    return replayService.getLocalReplays();
  }

}
