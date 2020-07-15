package com.faforever.client.replay;

import com.faforever.client.i18n.I18n;
import com.faforever.client.task.CompletableTask;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LoadLocalReplaysTask extends CompletableTask<Collection<Replay>> {

  private final ReplayService replayService;
  private final I18n i18n;
  private int pageNum;

  @Inject
  public LoadLocalReplaysTask(ReplayService replayService, I18n i18n) {
    super(Priority.HIGH);
    this.replayService = replayService;
    this.i18n = i18n;
    pageNum = 1;
  }

  @Override
  protected Collection<Replay> call() throws Exception {
    updateTitle(i18n.get("replays.loadingLocalTask.title"));
    return replayService.loadLocalReplays(pageNum).get();
  }

  public LoadLocalReplaysTask setPageNum(int pageNum) {
    this.pageNum = pageNum;
    return this;
  }
}
