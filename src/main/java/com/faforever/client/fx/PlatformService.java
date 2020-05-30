package com.faforever.client.fx;

import com.google.common.collect.Sets;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.WINDOWPLACEMENT;
import javafx.application.HostServices;
import lombok.SneakyThrows;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.regex.Pattern;

import static com.github.nocatch.NoCatch.noCatch;
import static org.bridj.Platform.show;

public class PlatformService {
  /**
   * Taken from https://stackoverflow.com/questions/163360/regular-expression-to-match-urls-in-java
   */
  public static final Pattern URL_REGEX_PATTERN = Pattern.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
  private final HostServices hostServices;

  private final boolean isWindows;

  public PlatformService(HostServices hostServices) {
    this.hostServices = hostServices;
    isWindows = Platform.isWindows();
  }

  /**
   * Opens the specified URI in a new browser window or tab.
   */
  
  public void showDocument(String url) {
    hostServices.showDocument(url);
  }

  /**
   * Show a file in its parent directory, if possible selecting the file (not possible on all platforms).
   */
  @SneakyThrows
  public void reveal(Path path) {
    if (isWindows) {
      Path finalPath = path;
      noCatch(() -> show(finalPath.toFile()));
    } else if (Platform.isLinux()) {
      //Might not work on all linux distros but let's give it a try
      if (Files.isRegularFile(path)) {
        path = path.getParent();
      }
      ProcessBuilder builder = new ProcessBuilder("xdg-open", path.toAbsolutePath().toString());
      builder.start();
    }
  }


  /**
   * Show a Window, restore it to it's state before minimizing (normal/restored or maximized) and move it to foreground
   * will only work on windows systems
   */
  
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

  
  public boolean isWindowFocused(String windowTitle) {
    return windowTitle.equals(getForegroundWindowTitle());
  }

  public void setUnixExecutableAndWritableBits(Path exePath) throws IOException {
    // client needs executable bit for running and the writeable bit set to alter the version
    if (SystemUtils.IS_OS_UNIX) {
      Files.setPosixFilePermissions(exePath, Sets.immutableEnumSet(PosixFilePermission.OWNER_READ,
    				  PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
    }
  }
}

