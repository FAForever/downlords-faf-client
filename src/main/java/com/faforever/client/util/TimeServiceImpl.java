package com.faforever.client.util;

import com.faforever.client.i18n.I18n;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.TimeZone;

public class TimeServiceImpl implements TimeService {

  @Resource
  I18n i18n;

  @Resource
  Locale locale;

  @Override
  public String timeAgo(Instant instant) {
    if (instant == null) {
      return "";
    }

    Duration ago = Duration.between(instant, Instant.now());

    if (Duration.ofMinutes(1).compareTo(ago) > 0) {
      return i18n.get("secondsAgo", ago.getSeconds());
    }
    if (Duration.ofHours(1).compareTo(ago) > 0) {
      return i18n.get("minutesAgo", ago.toMinutes());
    }
    if (Duration.ofDays(1).compareTo(ago) > 0) {
      return i18n.get("hoursAgo", ago.toHours());
    }
    if (Duration.ofDays(30).compareTo(ago) > 0) {
      return i18n.get("daysAgo", ago.toDays());
    }
    if (Duration.ofDays(365).compareTo(ago) > 0) {
      return i18n.get("monthsAgo", ago.toDays() / 30);
    }

    return i18n.get("yearsAgo", ago.toDays() / 365);
  }

  @Override
  public String lessThanOneDayAgo(Instant instant) {
    if (instant == null) {
      return "";
    }

    Duration ago = Duration.between(instant, Instant.now());

    if (ago.compareTo(Duration.ofDays(1)) <= 0) {
      return timeAgo(instant);
    }

    return asDate(instant);
  }

  @Override
  public String asDate(Instant instant) {
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(
        ZonedDateTime.ofInstant(instant, TimeZone.getDefault().toZoneId())
    );
  }

  @Override
  public String asShortTime(Instant instant) {
    return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(
        ZonedDateTime.ofInstant(instant, TimeZone.getDefault().toZoneId())
    );
  }

  @Override
  public String shortDuration(Duration duration) {
    if (duration == null) {
      return "";
    }

    if (Duration.ofMinutes(1).compareTo(duration) > 0) {
      return i18n.get("duration.seconds", duration.getSeconds());
    }
    if (Duration.ofHours(1).compareTo(duration) > 0) {
      return i18n.get("duration.minutesSeconds", duration.toMinutes(), duration.getSeconds());
    }

    return i18n.get("duration.hourMinutes", duration.toMinutes() / 60, duration.toMinutes() % 60);
  }
}
