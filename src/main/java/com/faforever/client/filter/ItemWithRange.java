package com.faforever.client.filter;

import org.apache.commons.lang3.Range;

public record ItemWithRange<I, T>(I item, Range<T> range) { }
