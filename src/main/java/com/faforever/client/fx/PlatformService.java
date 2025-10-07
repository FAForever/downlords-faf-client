package com.faforever.client.fx;

import com.faforever.client.os.OperatingSystem;
import com.faforever.client.os.OsPosix;
import com.faforever.client.os.OsUnknown;
import com.faforever.client.os.OsWindows;
import com.faforever.client.ui.StageHolder;
import com.google.common.collect.Sets;
import com.sun.jna.platform.DesktopWindow;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.WINDOWPLACEMENT;
import com.sun.jna.ptr.IntByReference;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Slf4j
@Lazy
@Component
@RequiredArgsConstructor
public class PlatformService {

  // Taken from https://stackoverflow.com/questions/163360/regular-expression-to-match-urls-in-java
  public static final Pattern STRICT_URL_REGEX_PATTERN = Pattern.compile(
      "^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

  public static final Pattern LENIENT_URL_REGEX_PATTERN = Pattern.compile(
      "^(?:(https?)://)?(?:[-a-zA-Z0-9]+\\.?)+\\.[a-zA-Z]{2,}(?::[0-9]+)?(?:(?:/[^/]*)?)*(?:\\?.*)?$");

  private final OperatingSystem operatingSystem;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  /**
   * Opens the specified URI in a new browser window or tab. Note: The code is copied from
   * {@link com.sun.javafx.application.HostServicesDelegate#showDocument(String)} The only fix is that all any
   * exceptions are intercepted by our side, and we can tell the user what happened wrong.
   */
  public void showDocument(String url) {
    final String[] browsers = {"xdg-open", "google-chrome", "firefox", "opera", "konqueror", "mozilla"};

    String osName = System.getProperty("os.name");
    try {
      if (osName.startsWith("Mac OS")) {
        Runtime.getRuntime().exec("open " + url);
      } else if (osName.startsWith("Windows")) {
        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
      } else { //assume Unix or Linux
        String browser = null;
        for (String b : browsers) {
          if (browser == null && Runtime.getRuntime().exec(new String[]{"which", b}).getInputStream().read() != -1) {
            Runtime.getRuntime().exec(new String[]{browser = b, url});
          }
        }
        if (browser == null) {
          throw new Exception("No web browser found");
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Show a file in its parent directory, if possible selecting the file (not possible on all platforms).
   */
  public void reveal(Path path) {
    try {
      if (Files.isRegularFile(path)) {
        path = path.getParent();
      }

      switch (operatingSystem) {
        case OsWindows osWindows -> {
          ProcessBuilder builder = new ProcessBuilder("explorer.exe", "/select,", path.toAbsolutePath().toString());
          builder.start();
        }
        case OsPosix osPosix -> {
          //Might not work on all linux distros but let's give it a try
          ProcessBuilder builder = new ProcessBuilder("xdg-open", path.toAbsolutePath().toString());
          builder.start();
        }
        case OsUnknown osUnknown -> {
          log.warn("Unknown OS, unable to reveal path");
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Show a Window, restore it to it's state before minimizing (normal/restored or maximized) and move it to foreground
   * will only work on windows systems Note: An application cannot force a window to the foreground while the user is
   * working with another window. Instead, Windows flashes the taskbar button of the window to notify the user.
   */
  public void focusWindow(String windowTitle) {
    focusWindow(windowTitle, null);
  }


  public void focusWindow(String windowTitle, @Nullable Long processId) {
    if (!(operatingSystem instanceof OsWindows)) {
      return;
    }
    log.debug("Focus '{}' window", windowTitle);
    focusWindow(getWindow(windowTitle, processId));
  }

  private void focusWindow(HWND window) {
    if (window == null) {
      log.warn("No window to focus");
      return;
    }

    User32 user32 = User32.INSTANCE;

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
    if (foregroundWindowTitle == null || !getForegroundWindowTitle().equals(WindowUtils.getWindowTitle(window))) {
      user32.SetForegroundWindow(window);
      user32.SetFocus(window);
    }
  }

  public void minimizeFocusedWindow() {
    if (operatingSystem instanceof OsWindows) {
      User32.INSTANCE.ShowWindow(getFocusedWindow(), User32.SW_MINIMIZE);
    }
  }

  private HWND getFocusedWindow() {
    return operatingSystem instanceof OsWindows ? User32.INSTANCE.GetForegroundWindow() : null;
  }

  public void startFlashingWindow(String windowTitle, @Nullable Long processId) {
    if (!(operatingSystem instanceof OsWindows)) {
      return;
    }

    HWND window = getWindow(windowTitle, processId);

    if (window == null) {
      log.warn("No window to start flashing");
      return;
    }

    WinUser.FLASHWINFO flashwinfo = new WinUser.FLASHWINFO();
    flashwinfo.hWnd = window;
    flashwinfo.dwFlags = WinUser.FLASHW_TRAY;
    flashwinfo.uCount = Integer.MAX_VALUE;
    flashwinfo.dwTimeout = 500;
    flashwinfo.cbSize = flashwinfo.size();

    User32.INSTANCE.FlashWindowEx(flashwinfo);
  }

  public void stopFlashingWindow(String windowTitle, @Nullable Long processId) {
    if (!(operatingSystem instanceof OsWindows)) {
      return;
    }

    HWND window = getWindow(windowTitle, processId);

    if (window == null) {
      log.warn("No window to stop flashing");
      return;
    }

    WinUser.FLASHWINFO flashwinfo = new WinUser.FLASHWINFO();
    flashwinfo.hWnd = window;
    flashwinfo.dwFlags = WinUser.FLASHW_STOP;
    flashwinfo.cbSize = flashwinfo.size();

    User32.INSTANCE.FlashWindowEx(flashwinfo);
  }

  @Nullable
  private String getForegroundWindowTitle() {
    return operatingSystem instanceof OsWindows ? WindowUtils.getWindowTitle(
        User32.INSTANCE.GetForegroundWindow()) : null;
  }

  public long getFocusedWindowProcessId() {
    return operatingSystem instanceof OsWindows ? getWindowProcessId(User32.INSTANCE.GetForegroundWindow()) : -1;
  }

  private int getWindowProcessId(HWND window) {
    IntByReference processId = new IntByReference();
    User32.INSTANCE.GetWindowThreadProcessId(window, processId);
    return processId.getValue();
  }

  @Nullable
  private HWND getWindow(String windowTitle, @Nullable Long processId) {
    if (processId != null) {
      return WindowUtils.getAllWindows(false)
                        .stream()
                        .filter(desktopWindow -> desktopWindow.getTitle().equals(windowTitle))
                        .filter(desktopWindow -> getWindowProcessId(desktopWindow.getHWND()) == processId)
                        .findFirst()
                        .map(DesktopWindow::getHWND)
                        .orElse(null);
    }
    return User32.INSTANCE.FindWindow(null, windowTitle);
  }


  public boolean isWindowFocused(String windowTitle) {
    return isWindowFocused(windowTitle, null);
  }

  public boolean isWindowFocused(String windowTitle, @Nullable Long processId) {
    boolean isWindowFocused = windowTitle.equals(getForegroundWindowTitle());
    if (processId != null) {
      return isWindowFocused && getFocusedWindowProcessId() == processId;
    }
    return isWindowFocused;
  }

  public void setUnixExecutableAndWritableBits(Path exePath) throws IOException {
    // client needs executable bit for running and the writeable bit set to alter the version
    if (SystemUtils.IS_OS_UNIX) {
      Files.setPosixFilePermissions(exePath, Sets.immutableEnumSet(PosixFilePermission.OWNER_READ,
                                                                   PosixFilePermission.OWNER_WRITE,
                                                                   PosixFilePermission.OWNER_EXECUTE));
    }
  }

  public CompletableFuture<Optional<Path>> askForPath(String title) {
    return askForPath(title, null);
  }

  public CompletableFuture<Optional<Path>> askForPath(String title, Path initialDirectory) {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle(title);
    if (initialDirectory != null) {
      directoryChooser.setInitialDirectory(initialDirectory.toFile());
    }

    return CompletableFuture.supplyAsync(
                                () -> directoryChooser.showDialog(StageHolder.getStage().getScene().getWindow()), fxApplicationThreadExecutor)
                            .thenApply(file -> Optional.ofNullable(file).map(File::toPath));
  }

  public Optional<Path> askForFile(String title, @Nullable Path initialDirectoryOrFile,
                                   ExtensionFilter extensionFilter) {
    AtomicReference<File> result = new AtomicReference<>();
    CountDownLatch waitForUserInput = new CountDownLatch(1);
    fxApplicationThreadExecutor.execute(() -> {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle(title);
      fileChooser.getExtensionFilters().add(extensionFilter);

      if (initialDirectoryOrFile != null) {
        File file = initialDirectoryOrFile.toFile();
        if (!file.isDirectory()) {
          file = file.getParentFile();
        }
        fileChooser.setInitialDirectory(file);
      }
      result.set(fileChooser.showOpenDialog(StageHolder.getStage().getScene().getWindow()));
      waitForUserInput.countDown();
    });
    try {
      waitForUserInput.await();
    } catch (InterruptedException e) {
      log.warn("Thread interrupted while waiting for user file selection", e);
    }
    return Optional.ofNullable(result.get()).map(File::toPath);
  }
}

