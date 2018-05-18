package com.faforever.client.fx;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.WINDOWPLACEMENT;
import javafx.application.Application;
import javafx.application.HostServices;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;

import static com.github.nocatch.NoCatch.noCatch;
import static org.bridj.Platform.show;

public class PlatformServiceImpl implements PlatformService {

  private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final HostServices hostServices;

  private final boolean isWindows;
  private final boolean isOpenJRE;

  public PlatformServiceImpl(Application application) {
    isWindows = Platform.isWindows();
    isOpenJRE = System.getProperty("java.runtime.name").contains("OpenJDK") || System.getProperty("java.runtime.name").contains("OpenJRE");

    if (!isOpenJRE) {
      this.hostServices = application.getHostServices();
    } else {
      this.hostServices = null;
    }
  }

  /**
   * Opens the specified URI in a new browser window or tab.
   */
  @Override
  public void showDocument(String url) {
    if (!isOpenJRE) {
      hostServices.showDocument(url);
    } else {
      logger.error("Couldn't open URL in browser due to running on OpenJRE.");
    }
  }

  /**
   * Show a file in its parent directory, if possible selecting the file (not possible on all platforms).
   */
  @Override
  public void reveal(Path path) {
    noCatch(() -> show(path.toFile()));
  }


  /**
   * Show a Window, restore it to it's state before minimizing (normal/restored or maximized) and move it to foreground
   * will only work on windows systems
   */
  @Override
  public void focusWindow(String windowTitle) {
    if (!isWindows) {
      return;
    }

    User32 user32 = User32.INSTANCE;
    HWND window = user32.FindWindow(null, windowTitle);

    // Does only set the window to visible, does not restore/bring it to foreground
    user32.ShowWindow(window, User32.SW_SHOW);

    WINDOWPLACEMENT windowplacement = new WINDOWPLACEMENT();
    user32.GetWindowPlacement(window, windowplacement);

    if (windowplacement.showCmd == User32.SW_SHOWMINIMIZED) {
      // Bit 2 in flags (bitmask 0x2) signals that window should be maximized when restoring
      if ((windowplacement.flags & WINDOWPLACEMENT.WPF_RESTORETOMAXIMIZED) == WINDOWPLACEMENT.WPF_RESTORETOMAXIMIZED) {
        user32.ShowWindow(window, User32.SW_SHOWMAXIMIZED);
      } else {
        user32.ShowWindow(window, User32.SW_SHOWNORMAL);
      }
    }

    String foregroundWindowTitle = getForegroundWindowTitle();
    if (foregroundWindowTitle == null || !getForegroundWindowTitle().equals(windowTitle.trim())) {
      user32.SetForegroundWindow(window);
    }
  }

  @Override
  public void startFlashingWindow(String windowTitle) {
    if (!isWindows) {
      return;
    }

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
    if (!isWindows) {
      return;
    }

    HWND window = User32.INSTANCE.FindWindow(null, windowTitle);

    WinUser.FLASHWINFO flashwinfo = new WinUser.FLASHWINFO();
    flashwinfo.hWnd = window;
    flashwinfo.dwFlags = WinUser.FLASHW_STOP;
    flashwinfo.cbSize = flashwinfo.size();

    User32.INSTANCE.FlashWindowEx(flashwinfo);
  }

  @Nullable
  private String getForegroundWindowTitle() {
    if (!isWindows) {
      return null;
    }

    HWND window = User32.INSTANCE.GetForegroundWindow();

    if (window == null) {
      return null;
    }

    char[] textBuffer = new char[255];
    User32.INSTANCE.GetWindowText(window, textBuffer, 255);
    return new String(textBuffer).trim();
  }

  @Override
  public boolean isWindowFocused(String windowTitle) {
    return windowTitle.equals(getForegroundWindowTitle());
  }
}
