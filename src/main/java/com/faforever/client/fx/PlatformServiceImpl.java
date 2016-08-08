package com.faforever.client.fx;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import javafx.application.HostServices;

import java.nio.file.Path;

import static com.github.nocatch.NoCatch.noCatch;
import static org.bridj.Platform.show;

public class PlatformServiceImpl implements PlatformService {

  private final HostServices hostServices;

  public PlatformServiceImpl(HostServices hostServices) {
    this.hostServices = hostServices;
  }

  /**
   * Opens the specified URI in a new browser window or tab.
   */
  @Override
  public void showDocument(String url) {
    hostServices.showDocument(url);
  }

  /**
   * Show a file in its parent directory, if possible selecting the file (not possible on all platforms).
   */
  @Override
  public void reveal(Path path) {
    noCatch(() -> show(path.toFile()));
  }

  @Override
  public void showWindow(String windowTitle) {
    HWND window = User32.INSTANCE.FindWindow(null, windowTitle);
    // SW_SHOW should be used instead, but in my tests it didn't work
    User32.INSTANCE.ShowWindow(window, User32.SW_SHOWMAXIMIZED);
  }

  @Override
  public void startFlashingWindow(String windowTitle) {
    HWND window = User32.INSTANCE.FindWindow(null, windowTitle);

    WinUser.FLASHWINFO flashwinfo = new WinUser.FLASHWINFO();
    flashwinfo.hWnd = window;
    flashwinfo.dwFlags = WinUser.FLASHW_TRAY;
    flashwinfo.uCount = Integer.MAX_VALUE;
    flashwinfo.dwTimeout = 500;
    flashwinfo.cbSize = flashwinfo.size();

    User32.INSTANCE.FlashWindowEx(flashwinfo);
  }

  @Override
  public void stopFlashingWindow(String windowTitle) {
    HWND window = User32.INSTANCE.FindWindow(null, windowTitle);

    WinUser.FLASHWINFO flashwinfo = new WinUser.FLASHWINFO();
    flashwinfo.hWnd = window;
    flashwinfo.dwFlags = WinUser.FLASHW_STOP;
    flashwinfo.cbSize = flashwinfo.size();

    User32.INSTANCE.FlashWindowEx(flashwinfo);
  }

  @Override
  public String getForegroundWindowTitle() {
    HWND hwnd = User32.INSTANCE.GetForegroundWindow();
    char[] textBuffer = new char[255];
    User32.INSTANCE.GetWindowText(hwnd, textBuffer, 255);
    return new String(textBuffer).trim();
  }
}
