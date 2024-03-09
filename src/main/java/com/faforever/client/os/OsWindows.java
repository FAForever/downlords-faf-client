package com.faforever.client.os;

import com.faforever.client.os.Kernel32Ex.WindowsPriority;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Scanner;

@Slf4j
public final class OsWindows implements OperatingSystem {
  private static final String APP_DATA_SUB_FOLDER = "Forged Alliance Forever";

  @Override
  public boolean runsAsAdmin() {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe");
      Process process = processBuilder.start();
      PrintStream printStream = new PrintStream(process.getOutputStream(), true);
      Scanner scanner = new Scanner(process.getInputStream());
      printStream.println("@echo off");
      printStream.println(">nul 2>&1 \"%SYSTEMROOT%\\system32\\cacls.exe\" \"%SYSTEMROOT%\\system32\\config\\system\"");
      printStream.println("echo %errorlevel%");

      boolean printedErrorlevel = false;
      while (true) {
        if (!scanner.hasNextLine()) {
          return false;
        }
        String nextLine = scanner.nextLine();
        if (printedErrorlevel) {
          int errorlevel = Integer.parseInt(nextLine);
          return errorlevel == 0;
        } else if (nextLine.equals("echo %errorlevel%")) {
          printedErrorlevel = true;
        }
      }
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public boolean supportsUpdateInstall() {
    return true;
  }

  @Override
  @NotNull
  public Path getLoggingDirectory() {
    return Path.of(System.getenv("APPDATA")).resolve(APP_DATA_SUB_FOLDER).resolve("logs");
  }

  @Override
  @NotNull
  public Path getPreferencesDirectory() {
    return Path.of(System.getenv("APPDATA")).resolve(APP_DATA_SUB_FOLDER);
  }

  @Override
  @NotNull
  public Path getUidExecutablePath() {
    String uidDir = System.getProperty("nativeDir", "natives");
    return Path.of(uidDir).resolve("faf-uid.exe");
  }

  @Override
  public @NotNull Path getJavaExecutablePath() {
    return Path.of(System.getProperty("java.home"))
        .resolve("bin")
        .resolve("java.exe");
  }

  @Override
  public @NotNull String getGithubAssetFileEnding() {
    return ".exe";
  }

  @Override
  public @NotNull Path getDefaultDataDirectory() {
    return Path.of(Shell32Util.getFolderPath(ShlObj.CSIDL_COMMON_APPDATA), "FAForever");
  }

  @Override
  public @NotNull Path getSteamFaDirectory() {
    return Path.of(Shell32Util.getFolderPath(ShlObj.CSIDL_PROGRAM_FILESX86), "Steam", "steamapps", "common", "Supreme Commander Forged Alliance");
  }

  @Override
  public @NotNull Path getLocalFaDataPath() {
    return Path.of(Shell32Util.getFolderPath(ShlObj.CSIDL_LOCAL_APPDATA), "Gas Powered Games", "Supreme Commander Forged Alliance");
  }

  @Override
  public @NotNull Path getDefaultVaultDirectory() {
    return Path.of(Shell32Util.getFolderPath(ShlObj.CSIDL_PERSONAL), "My Games", "Gas Powered Games", "Supreme Commander Forged Alliance");
  }

  @Override
  public void increaseProcessPriority(Process process) {
    setProcessPriority(process, WindowsPriority.HIGH_PRIORITY_CLASS);
  }

  private void setProcessPriority(Process process, WindowsPriority priority) {
    log.debug("Settings priority of process {} to {}", process.pid(), priority);
    try {
      DWORD dwPriorityClass = priority.dword();
      boolean success = Kernel32Ex.INSTANCE.SetPriorityClass(getProcessHandle(process), dwPriorityClass);
      if (!success) {
        int lastError = Kernel32.INSTANCE.GetLastError();
        log.warn("Could not set priority of process {} (error {})", process.pid(), lastError);
      }
    } catch (Exception e) {
      log.warn("Could not set priority of process {}", process.pid(), e);
    }
  }

  private static HANDLE getProcessHandle(Process process) throws Exception {
    Field f = process.getClass().getDeclaredField("handle");
    f.setAccessible(true);
    long handle = f.getLong(process);

    WinNT.HANDLE hProcess = new WinNT.HANDLE();
    hProcess.setPointer(Pointer.createConstant(handle));
    return hProcess;
  }
}
