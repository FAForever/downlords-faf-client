package com.faforever.client.net;

import org.bridj.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.util.StringTokenizer;

public final class GatewayUtil {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int GATEWAY_COLUMN = Platform.isWindows() ? 3 : 2;

  private GatewayUtil() {
    // Utility class
  }

  public static InetAddress findGateway() throws IOException {
    logger.debug("Trying to detect default gateway");
    Process result;
    try {
      result = Runtime.getRuntime().exec("netstat -rn");
    } catch (IOException e) {
      logger.debug("Could not execute netstat ({})", e.getMessage());
      return null;
    }

    BufferedReader output = new BufferedReader(new InputStreamReader(result.getInputStream()));

    String line;
    while ((line = output.readLine()) != null) {
      if (line.contains("0.0.0.0") || line.contains("::/0")) {
        StringTokenizer stringTokenizer = new StringTokenizer(line);
        for (int i = 1; i < GATEWAY_COLUMN; i++) {
          stringTokenizer.nextElement();
        }
        InetAddress inetAddress = InetAddress.getByName(stringTokenizer.nextToken());

        logger.debug("Detected default gateway: {}", inetAddress);
        return inetAddress;
      }
    }

    logger.debug("No default gateway configured");
    return null;
  }
}
