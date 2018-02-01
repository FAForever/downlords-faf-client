package com.faforever.client.units;

import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CookieServiceImpl implements CookieService {

  private final PreferencesService preferencesService;
  private final Map<URI, ArrayList<HttpCookie>> storedCookies;

  @Inject
  public CookieServiceImpl(PreferencesService preferencesService) {
    this.preferencesService = preferencesService;
    Preferences preferences = preferencesService.getPreferences();
    storedCookies = preferences.getStoredCookies();
  }

  @Override
  public void setUpCookieManger() {
    CookieManager manager = new CookieManager(new MyCookieStore(), CookiePolicy.ACCEPT_ALL);
    CookieHandler.setDefault(manager);
  }

  public class MyCookieStore implements CookieStore {
    @Override
    public void add(URI uri, HttpCookie cookie) {
      URI base = URI.create(uri.getHost());
      if (!storedCookies.containsKey(base)) {
        storedCookies.put(base, new ArrayList<>());
      }
      storedCookies.get(base).stream()
          .filter(httpCookie -> httpCookie.equals(cookie))
          .findFirst()
          .ifPresent(httpCookie -> storedCookies.get(base).remove(httpCookie));
      storedCookies.get(base).add(cookie);
      preferencesService.storeInBackground();
    }

    @Override
    public List<HttpCookie> get(URI uri) {
      return storedCookies.containsKey(URI.create(uri.getHost())) ? storedCookies.get(URI.create(uri.getHost())) : Collections.emptyList();
    }

    @Override
    public List<HttpCookie> getCookies() {
      return storedCookies.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public List<URI> getURIs() {
      return new ArrayList<>(preferencesService.getPreferences().getStoredCookies().keySet());
    }

    @Override
    public boolean remove(URI uri, HttpCookie cookie) {
      if (storedCookies.containsKey(URI.create(uri.getHost()))) {
        boolean remove = storedCookies.get(URI.create(uri.getHost())).remove(cookie);
        preferencesService.storeInBackground();
        return remove;
      }
      return false;
    }

    @Override
    public boolean removeAll() {
      storedCookies.clear();
      preferencesService.storeInBackground();
      return true;
    }
  }
}
