package com.faforever.client.fa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the smallest Windows executable {@link FafReExecutable} can read: headers, one resource section and a version
 * resource that holds only a product name.
 */
public final class TestExecutables {

  private static final int SECTION_RVA = 0x1000;
  private static final int SECTION_OFFSET = 0x200;

  private TestExecutables() {
  }

  /**
   * @param productName the product name in the version resource, or null for an exe without resources
   */
  public static Path write(Path file, boolean pe32Plus, String productName) throws IOException {
    byte[] resources = productName == null ? new byte[0] : resourceSection(versionInfo(productName));
    int optionalHeaderSize = pe32Plus ? 240 : 224;

    ByteBuffer exe = ByteBuffer.allocate(SECTION_OFFSET + resources.length).order(ByteOrder.LITTLE_ENDIAN);
    exe.putShort(0, (short) 0x5A4D).putInt(0x3C, 0x40);
    exe.putInt(0x40, 0x4550)
        .putShort(0x40 + 4, (short) (pe32Plus ? 0x8664 : 0x14C))
        .putShort(0x40 + 6, (short) 1)
        .putShort(0x40 + 20, (short) optionalHeaderSize);
    int optionalHeader = 0x40 + 24;
    exe.putShort(optionalHeader, (short) (pe32Plus ? 0x20B : 0x10B));
    if (productName != null) {
      int resourceDirectory = optionalHeader + (pe32Plus ? 112 : 96) + 2 * 8;
      exe.putInt(resourceDirectory, SECTION_RVA).putInt(resourceDirectory + 4, resources.length);
    }
    int section = optionalHeader + optionalHeaderSize;
    exe.put(section, ".rsrc".getBytes(StandardCharsets.US_ASCII))
        .putInt(section + 8, resources.length)
        .putInt(section + 12, SECTION_RVA)
        .putInt(section + 16, resources.length)
        .putInt(section + 20, SECTION_OFFSET);
    exe.put(SECTION_OFFSET, resources);
    return Files.write(file, exe.array());
  }

  /**
   * One resource of type RT_VERSION, id 1, language 0x409, followed by its data.
   */
  private static byte[] resourceSection(byte[] versionInfo) {
    ByteBuffer section = ByteBuffer.allocate(88 + versionInfo.length).order(ByteOrder.LITTLE_ENDIAN);
    directory(section, 0, 16, 0x80000000 | 24);
    directory(section, 24, 1, 0x80000000 | 48);
    directory(section, 48, 0x409, 72);
    section.putInt(72, SECTION_RVA + 88).putInt(76, versionInfo.length);
    section.put(88, versionInfo);
    return section.array();
  }

  private static void directory(ByteBuffer section, int offset, int id, int target) {
    section.putShort(offset + 14, (short) 1).putInt(offset + 16, id).putInt(offset + 20, target);
  }

  private static byte[] versionInfo(String productName) {
    byte[] productNameString = node("ProductName", 1, text(productName), productName.length() + 1);
    byte[] stringTable = node("040904b0", 1, new byte[0], 0, productNameString);
    byte[] stringFileInfo = node("StringFileInfo", 1, new byte[0], 0, stringTable);
    return node("VS_VERSION_INFO", 0, new byte[52], 52, stringFileInfo);
  }

  /**
   * A version resource node: length, value length, type, key, value and children, each part aligned to 4 bytes.
   */
  private static byte[] node(String key, int type, byte[] value, int valueLength, byte[]... children) {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    body.writeBytes(new byte[6]);
    body.writeBytes(text(key));
    pad(body);
    body.writeBytes(value);
    pad(body);
    for (byte[] child : children) {
      body.writeBytes(child);
    }
    byte[] node = body.toByteArray();
    ByteBuffer.wrap(node)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putShort(0, (short) node.length)
        .putShort(2, (short) valueLength)
        .putShort(4, (short) type);
    return node;
  }

  private static byte[] text(String text) {
    return (text + "\0").getBytes(StandardCharsets.UTF_16LE);
  }

  private static void pad(ByteArrayOutputStream body) {
    while (body.size() % 4 != 0) {
      body.write(0);
    }
  }
}
