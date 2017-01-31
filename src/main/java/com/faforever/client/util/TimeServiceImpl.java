package com.faforever.client.util;

import com.faforever.client.i18n.I18n;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.TimeZone;


@Lazy
@Service
public class TimeServiceImpl implements TimeService {

  private final I18n i18n;
  private final Locale locale;

  @Inject
  public TimeServiceImpl(I18n i18n, Locale locale) {
    this.i18n = i18n;
    this.locale = locale;
  }

  @Override
  public String timeAgo(Instant instant) {
    if (instant == null) {
      return "";
    }

    Duration ago = Duration.between(instant, Instant.now());

    if (Duration.ofMinutes(1).compareTo(ago) > 0) {
      return i18n.getQuantized("secondAgo", "secondsAgo", ago.getSeconds());
    }
    if (Duration.ofHours(1).compareTo(ago) > 0) {
      return i18n.getQuantized("minuteAgo", "minutesAgo", ago.toMinutes());
    }
    if (Duration.ofDays(1).compareTo(ago) > 0) {
      return i18n.getQuantized("hourAgo", "hoursAgo", ago.toHours());
    }
    if (Duration.ofDays(30).compareTo(ago) > 0) {
      return i18n.getQuantized("dayAgo", "daysAgo", ago.toDays());
    }
    if (Duration.ofDays(365).compareTo(ago) > 0) {
      return i18n.getQuantized("monthAgo", "monthsAgo", ago.toDays() / 30);
    }

    return i18n.getQuantized("yearAgo", "yearsAgo", ago.toDays() / 365);
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
  public String asDate(TemporalAccessor temporalAccessor) {
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
        .withLocale(locale)
        .withZone(TimeZone.getDefault().toZoneId())
        .format(temporalAccessor);
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
