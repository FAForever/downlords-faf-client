package com.faforever.client.ice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class WindowsIceAdapterService implements IceAdapterService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("${stun.host}")
  String stunServerAddress;

  @Value("${turn.host}")
  String turnServerAddress;

  @Resource
  ApplicationContext applicationContext;

  @Override
  public CompletableFuture<IceAdapterClient> start() {
    CompletableFuture<IceAdapterClient> future = new CompletableFuture<>();
    new Thread(() -> {
      // TODO rename to nativeDir and reuse in UID service
      String uidDir = System.getProperty("uid.dir", "lib");
      String[] cmd = new String[]{
          Paths.get(uidDir, "faf-ice-adapter.exe").toAbsolutePath().toString(),
          "-s", stunServerAddress,
          "-t", turnServerAddress
      };

      try {
        logger.debug("Starting ICE adapter with command: {}", (Object[]) cmd);

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.inheritIO();
        processBuilder.command(cmd);

        Process process = processBuilder.start();
        future.complete(applicationContext.getBean(IceAdapterClient.class));

        int exitCode = process.waitFor();
        if (exitCode == 0) {
          logger.debug("ICE adapter terminated normally");
        } else {
          logger.warn("ICE adapter terminated with exit code: {}", exitCode);
        }
      } catch (Exception e) {
        future.completeExceptionally(e);
      }
    }).start();

    return future;
  }
}
