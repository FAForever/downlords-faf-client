package com.faforever.client.discord.ipc;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Connection to the local Discord client. All methods are called from a single thread, since a Windows named pipe opened
 * for synchronous I/O can't be read and written concurrently.
 */
interface DiscordIpcPipe extends Closeable {

  int MAX_PIPE_INDEX = 9;

  /** Returns the number of bytes that can be read without blocking. */
  int available() throws IOException;

  void readFully(byte[] bytes) throws IOException;

  void write(byte[] bytes) throws IOException;

  @FunctionalInterface
  interface Opener {
    DiscordIpcPipe open() throws IOException;
  }

  static DiscordIpcPipe open() throws IOException {
    boolean windows = System.getProperty("os.name").toLowerCase().startsWith("windows");
    for (int i = 0; i <= MAX_PIPE_INDEX; i++) {
      try {
        if (windows) {
          return new WindowsPipe(new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw"));
        }
        for (Path path : unixSocketCandidates(i)) {
          if (Files.exists(path)) {
            return UnixPipe.connect(path);
          }
        }
      } catch (IOException e) {
        // Try the next pipe
      }
    }
    throw new IOException("Discord is not running");
  }

  private static List<Path> unixSocketCandidates(int index) {
    String fileName = "discord-ipc-" + index;
    List<Path> candidates = new ArrayList<>();
    Stream.of(System.getenv("XDG_RUNTIME_DIR"), System.getenv("TMPDIR"), System.getenv("TMP"), System.getenv("TEMP"),
              "/tmp")
          .filter(Objects::nonNull)
          .map(Path::of)
          .forEach(directory -> {
            candidates.add(directory.resolve(fileName));
            candidates.add(directory.resolve("app/com.discordapp.Discord").resolve(fileName));
            candidates.add(directory.resolve("snap.discord").resolve(fileName));
          });
    return candidates;
  }

  private static void pause() throws InterruptedIOException {
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InterruptedIOException();
    }
  }

  record WindowsPipe(RandomAccessFile file) implements DiscordIpcPipe {

    @Override
    public int available() throws IOException {
      // For a pipe, the "length" is the number of bytes waiting to be read
      return (int) file.length();
    }

    @Override
    public void readFully(byte[] bytes) throws IOException {
      file.readFully(bytes);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
      file.write(bytes);
    }

    @Override
    public void close() throws IOException {
      file.close();
    }
  }

  final class UnixPipe implements DiscordIpcPipe {

    private final SocketChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocate(64 * 1024).flip();

    private UnixPipe(SocketChannel channel) {
      this.channel = channel;
    }

    static UnixPipe connect(Path path) throws IOException {
      SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
      try {
        channel.connect(UnixDomainSocketAddress.of(path));
        channel.configureBlocking(false);
      } catch (IOException e) {
        channel.close();
        throw e;
      }
      return new UnixPipe(channel);
    }

    @Override
    public int available() throws IOException {
      fill();
      return buffer.remaining();
    }

    @Override
    public void readFully(byte[] bytes) throws IOException {
      int offset = 0;
      while (offset < bytes.length) {
        if (!buffer.hasRemaining()) {
          fill();
          if (!buffer.hasRemaining()) {
            pause();
            continue;
          }
        }
        int length = Math.min(buffer.remaining(), bytes.length - offset);
        buffer.get(bytes, offset, length);
        offset += length;
      }
    }

    @Override
    public void write(byte[] bytes) throws IOException {
      ByteBuffer data = ByteBuffer.wrap(bytes);
      while (data.hasRemaining()) {
        if (channel.write(data) == 0) {
          pause();
        }
      }
    }

    @Override
    public void close() throws IOException {
      channel.close();
    }

    private void fill() throws IOException {
      buffer.compact();
      try {
        if (channel.read(buffer) < 0) {
          throw new EOFException("Discord closed the connection");
        }
      } finally {
        buffer.flip();
      }
    }
  }
}
