package com.faforever.client.fa;

import com.faforever.client.fa.Kernel32Ex.WindowsPriority;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.bridj.Platform;

import java.lang.reflect.Field;

@UtilityClass
@Slf4j
public class ProcessUtils {

  void setProcessPriority(Process process, WindowsPriority priority) {
    if (!Platform.isWindows()) {
      log.debug("Setting process priority is only supported on Windows");
      return;
    }

    log.debug("Settings priority of process {} to {}", process.pid(), priority);
    try {
      DWORD dwPriorityClass = priority.dword();
      BOOL success = Kernel32Ex.INSTANCE.SetPriorityClass(getProcessHandle(process), dwPriorityClass);
      if (!success.booleanValue()) {
        int lastError = Kernel32.INSTANCE.GetLastError();
        log.warn("Could not set priority of process {} (error {})", process.pid(), lastError);
      }
    } catch (Exception e) {
      log.warn("Could not set priority of process {}", process.pid(), e);
    }
  }

  private HANDLE getProcessHandle(Process process) throws Exception {
    Field f = process.getClass().getDeclaredField("handle");
    f.setAccessible(true);
    long handle = f.getLong(process);

    WinNT.HANDLE hProcess = new WinNT.HANDLE();
    hProcess.setPointer(Pointer.createConstant(handle));
    return hProcess;
  }
}
