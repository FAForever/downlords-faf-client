package com.faforever.client.ice;

import com.faforever.client.config.BaseConfig;
import com.faforever.client.fa.LaunchCommandBuilder;
import com.faforever.client.game.GameType;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.github.nocatch.NoCatch.noCatch;

public class Test {
  public static void main(String[] args) throws IOException, InterruptedException {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(BaseConfig.class);
    context.register(IceConfig.class);
    context.registerShutdownHook();
    context.refresh();

    CountDownLatch countDownLatch = new CountDownLatch(1);

    WindowsIceAdapterService windowsIceAdapterService = context.getBean(WindowsIceAdapterService.class);

    windowsIceAdapterService.start().thenAccept(iceAdapterClient -> {
      List<String> launchCommand = LaunchCommandBuilder.create()
          .executable(Paths.get("C:/ProgramData/FAForever/bin/ForgedAlliance.exe"))
          .executableDecorator("%s")
          .localGpgPort(7237)
          .gameType(GameType.DEFAULT.getString())
          .build();

      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.inheritIO();
      processBuilder.directory(new File("C:/ProgramData/FAForever/bin/"));
      processBuilder.command(launchCommand);
      noCatch(() -> processBuilder.start().waitFor());

    }).exceptionally(throwable -> {
      throwable.printStackTrace();
      return null;
    });

    countDownLatch.await();
  }
}
