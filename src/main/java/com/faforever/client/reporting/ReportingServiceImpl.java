package com.faforever.client.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;


@Lazy
@Service
public class ReportingServiceImpl implements ReportingService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void reportError(Throwable e) {
    logger.warn("Reporting has not yet been implemented");
  }
}
