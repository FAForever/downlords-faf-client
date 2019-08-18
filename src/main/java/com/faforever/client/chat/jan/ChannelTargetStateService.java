package com.faforever.client.chat.jan;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.google.common.collect.Iterables;

/**
 * Contains all state about which channels the user should currently be or continue to be in and allows
 * to change this state independently from the chat implementation. The order of channels should correlate
 * with the priority of the channel for the user. Permanent channels have priority over temporary channels.
 * 
 * Thread-safe.
 */
@Service
public class ChannelTargetStateService {
  
  // Channels the user should be in permanently for the entire duration of the clients login.
  // For example, the decision of the user to leave one of them may be ignored on reconnect.
  private Set<String> permanentChannels;
  
  // Channels that the user wants to be in, but that he may decide to leave and then should not
  // be rejoined into.
  private Set<String> temporaryChannels;
  
  // Note: Permanent and temporary channel sets are always distinct.
  
  public ChannelTargetStateService() {
    // TODO iterators are not thread-safe
    permanentChannels = Collections.synchronizedSet(new LinkedHashSet<String>());
    temporaryChannels = Collections.synchronizedSet(new LinkedHashSet<String>());
  }
  
  protected boolean addPermanentChannel(String channelName) {
    Objects.requireNonNull(channelName);
    temporaryChannels.remove(channelName);
    return permanentChannels.add(channelName);
  }
  
  protected boolean addTemporaryChannel(String channelName) {
    Objects.requireNonNull(channelName);
    if (permanentChannels.contains(channelName)) {
      return false;
    }
    return temporaryChannels.add(channelName);
  }
  
  protected boolean removePermanentChannel(String channelName) {
    Objects.requireNonNull(channelName);
    return permanentChannels.remove(channelName);
  }
  
  protected boolean removeTemporaryChannel(String channelName) {
    Objects.requireNonNull(channelName);
    return temporaryChannels.remove(channelName);
  }
  
  protected Set<String> getPermanentChannels() {
    return Collections.unmodifiableSet(permanentChannels);
  }
  
  protected Set<String> getTemporaryChannels() {
    return Collections.unmodifiableSet(temporaryChannels);
  }
  
  protected boolean shouldUserBeIn(String channelName) {
    Objects.requireNonNull(channelName);
    return permanentChannels.contains(channelName) || temporaryChannels.contains(channelName);
  }
  
  protected Iterable<String> getAllChannels() {
    return Iterables.unmodifiableIterable(Iterables.concat(permanentChannels, temporaryChannels));
  }
  
  @Override
  public String toString() {
    String result = "";
    for (String channelName : getAllChannels()) {
      result += " " + channelName;
    }
    return result;
  }
}
