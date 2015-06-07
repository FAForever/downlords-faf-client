package com.faforever.client.legacy.update;

import com.faforever.client.game.ModInfoBean;
import com.faforever.client.legacy.writer.ServerWriter;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class UpdateServerAccessorImpl implements UpdateServerAccessor {

  private ServerWriter serverWriter;

  private CountDownLatch serverResponseLatch;

  @Override
  public void requestBinFilesToUpdate(ModInfoBean modInfoBean) throws IOException {
    serverResponseLatch = new CountDownLatch(1);

    serverWriter.write(new RequestFilesToUpdateMessage(modInfoBean.getName()));
  }

  @Override
  public void requestGameDataFilesToUpdate(ModInfoBean modInfoBean) {
    serverWriter.write(new RequestFilesToUpdateMessage(modInfoBean.getName()));
  }
}
