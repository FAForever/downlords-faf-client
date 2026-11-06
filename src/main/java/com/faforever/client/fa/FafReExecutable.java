package com.faforever.client.fa;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Tells the faf-re engine (https://github.com/Draiget/faf-re) from the original ForgedAlliance.exe. Both are named
 * ForgedAlliance.exe and both can be 32-bit, so this reads the product name from the exe's version resource: "faf-re"
 * there, "Supreme Commander Forged Alliance" in the original. The file is parsed here rather than through Windows,
 * as the client also runs on Linux.
 */
@Slf4j
@UtilityClass
public class FafReExecutable {

  private final String PRODUCT_NAME = "faf-re";
  private final int RT_VERSION = 16;

  public boolean isFafRe(Path executable) {
    return readProductName(executable).filter(PRODUCT_NAME::equals).isPresent();
  }

  public Optional<String> readProductName(Path executable) {
    try (FileChannel channel = FileChannel.open(executable)) {
      ByteBuffer versionInfo = readVersionResource(channel);
      return versionInfo == null ? Optional.empty() : findProductName(versionInfo);
    } catch (IOException | RuntimeException e) {
      log.debug("Could not read the version resource of `{}`", executable, e);
      return Optional.empty();
    }
  }

  private ByteBuffer readVersionResource(FileChannel channel) throws IOException {
    ByteBuffer dosHeader = read(channel, 0, 64);
    if (dosHeader.getShort(0) != 0x5A4D) {
      return null;
    }
    int peOffset = dosHeader.getInt(0x3C);
    ByteBuffer fileHeader = read(channel, peOffset, 24);
    if (fileHeader.getInt(0) != 0x4550) {
      return null;
    }
    int sectionCount = Short.toUnsignedInt(fileHeader.getShort(6));
    int optionalHeaderSize = Short.toUnsignedInt(fileHeader.getShort(20));
    ByteBuffer optionalHeader = read(channel, peOffset + 24, optionalHeaderSize);
    int dataDirectories = optionalHeader.getShort(0) == 0x20B ? 112 : 96;
    int resourceRva = optionalHeader.getInt(dataDirectories + 2 * 8);

    ByteBuffer sections = read(channel, peOffset + 24 + optionalHeaderSize, sectionCount * 40);
    for (int section = 0; section < sectionCount; section++) {
      int virtualAddress = sections.getInt(section * 40 + 12);
      int rawSize = sections.getInt(section * 40 + 16);
      if (resourceRva < virtualAddress || resourceRva >= virtualAddress + rawSize) {
        continue;
      }
      ByteBuffer resources = read(channel, sections.getInt(section * 40 + 20), rawSize);
      int entry = findDirectoryEntry(resources, resourceRva - virtualAddress, RT_VERSION);
      for (int level = 0; entry >= 0 && level < 2; level++) {
        entry = findDirectoryEntry(resources, entry, -1);
      }
      if (entry < 0) {
        return null;
      }
      int dataRva = resources.getInt(entry);
      int dataSize = resources.getInt(entry + 4);
      return resources.slice(dataRva - virtualAddress, dataSize).order(ByteOrder.LITTLE_ENDIAN);
    }
    return null;
  }

  /**
   * The offset of what the directory entry with this id points to, or of the first entry's for id -1; -1 if there is none.
   */
  private int findDirectoryEntry(ByteBuffer resources, int directory, int id) {
    int entryCount = Short.toUnsignedInt(resources.getShort(directory + 12))
        + Short.toUnsignedInt(resources.getShort(directory + 14));
    for (int i = 0; i < entryCount; i++) {
      int entry = directory + 16 + i * 8;
      if (id == -1 || resources.getInt(entry) == id) {
        return resources.getInt(entry + 4) & 0x7FFFFFFF;
      }
    }
    return -1;
  }

  /**
   * VS_VERSIONINFO holds StringFileInfo, which holds one string table per language, which holds the strings.
   */
  private Optional<String> findProductName(ByteBuffer versionInfo) {
    for (int fileInfo = firstChild(versionInfo, 0); fileInfo < end(versionInfo, 0); fileInfo = next(versionInfo, fileInfo)) {
      if (!key(versionInfo, fileInfo).equals("StringFileInfo")) {
        continue;
      }
      for (int table = firstChild(versionInfo, fileInfo); table < end(versionInfo, fileInfo); table = next(versionInfo, table)) {
        for (int string = firstChild(versionInfo, table); string < end(versionInfo, table); string = next(versionInfo, string)) {
          if (key(versionInfo, string).equals("ProductName")) {
            return Optional.of(text(versionInfo, valueStart(versionInfo, string), Short.toUnsignedInt(versionInfo.getShort(string + 2))));
          }
        }
      }
    }
    return Optional.empty();
  }

  private String key(ByteBuffer versionInfo, int node) {
    return text(versionInfo, node + 6, Integer.MAX_VALUE);
  }

  private int valueStart(ByteBuffer versionInfo, int node) {
    return align(node + 6 + (key(versionInfo, node).length() + 1) * 2);
  }

  private int firstChild(ByteBuffer versionInfo, int node) {
    int valueLength = Short.toUnsignedInt(versionInfo.getShort(node + 2));
    int valueBytes = versionInfo.getShort(node + 4) == 1 ? valueLength * 2 : valueLength;
    return align(valueStart(versionInfo, node) + valueBytes);
  }

  private int end(ByteBuffer versionInfo, int node) {
    return Math.min(node + Short.toUnsignedInt(versionInfo.getShort(node)), versionInfo.limit());
  }

  private int next(ByteBuffer versionInfo, int node) {
    int length = Short.toUnsignedInt(versionInfo.getShort(node));
    return length == 0 ? versionInfo.limit() : align(node + length);
  }

  private String text(ByteBuffer versionInfo, int offset, int maxChars) {
    StringBuilder text = new StringBuilder();
    for (int i = offset; text.length() < maxChars && i + 1 < versionInfo.limit(); i += 2) {
      char c = versionInfo.getChar(i);
      if (c == 0) {
        break;
      }
      text.append(c);
    }
    return text.toString();
  }

  private int align(int offset) {
    return (offset + 3) & ~3;
  }

  private ByteBuffer read(FileChannel channel, long position, int size) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
    while (buffer.hasRemaining()) {
      if (channel.read(buffer, position + buffer.position()) < 0) {
        throw new EOFException();
      }
    }
    return buffer.flip();
  }
}
