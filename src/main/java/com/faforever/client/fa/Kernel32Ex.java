package com.faforever.client.fa;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.win32.W32APIOptions;

interface Kernel32Ex extends Kernel32 {

  Kernel32Ex INSTANCE = Native.load("kernel32", Kernel32Ex.class, W32APIOptions.DEFAULT_OPTIONS);

  /**
   * Sets the priority class for the specified process. This value together with the priority value of each thread of
   * the process determines each thread's base priority level.
   *
   * @return If the function succeeds, the return value is the previous value of the specified offset. If the function
   * fails, the return value is zero. To get extended error information, call GetLastError.
   */
  BOOL SetPriorityClass(HANDLE hProcess, DWORD dwPriorityClass);

  enum WindowsPriority {
    REALTIME_PRIORITY_CLASS(0x00000100),
    HIGH_PRIORITY_CLASS(0x00000080),
    ABOVE_NORMAL_PRIORITY_CLASS(0x00008000),
    NORMAL_PRIORITY_CLASS(0x00000020),
    BELOW_NORMAL_PRIORITY_CLASS(0x00004000),
    IDLE_PRIORITY_CLASS(0x00000040);

    private final int value;

    WindowsPriority(int value) {
      this.value = value;
    }

    public DWORD dword() {
      return new DWORD(value);
    }
  }
}