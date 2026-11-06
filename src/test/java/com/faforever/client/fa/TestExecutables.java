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
 * resource that holds only a product name. The other methods write the same file with one part missing or moved.
 */
public final class TestExecutables {

  private static final int RT_VERSION = 16;
  private static final int RT_ICON = 3;
  private static final int SECTION_RVA = 0x1000;
  private static final int SECTION_OFFSET = 0x200;
  private static final int PE_HEADER = 0x40;

  private TestExecutables() {
  }

  /**
   * @param productName the product name in the version resource, or null for an exe without resources
   */
  public static Path write(Path file, boolean pe32Plus, String productName) throws IOException {
    return write(file, pe32Plus, productName == null ? null : resourceSection(RT_VERSION, versionInfo(productName)));
  }

  /**
   * An exe whose version resource has a VarFileInfo block before its StringFileInfo, as resource compilers may write it.
   */
  public static Path writeVarFileInfoFirst(Path file, String productName) throws IOException {
    byte[] varFileInfo = node("VarFileInfo", 1, new byte[0], 0, node("Translation", 0, new byte[]{9, 4, (byte) 0xB0, 4}, 4));
    byte[] stringFileInfo = node("StringFileInfo", 1, new byte[0], 0, node("040904b0", 1, new byte[0], 0, string("ProductName", productName)));
    byte[] versionInfo = node("VS_VERSION_INFO", 0, new byte[52], 52, varFileInfo, stringFileInfo);
    return write(file, true, resourceSection(RT_VERSION, versionInfo));
  }

  /**
   * An exe whose version resource has strings, but no ProductName among them.
   */
  public static Path writeWithoutProductName(Path file) throws IOException {
    byte[] stringFileInfo = node("StringFileInfo", 1, new byte[0], 0, node("040904b0", 1, new byte[0], 0, string("FileVersion", "1.0")));
    return write(file, true, resourceSection(RT_VERSION, node("VS_VERSION_INFO", 0, new byte[52], 52, stringFileInfo)));
  }

  /**
   * An exe with resources, but only an icon.
   */
  public static Path writeIconOnly(Path file) throws IOException {
    return write(file, true, resourceSection(RT_ICON, new byte[16]));
  }

  /**
   * An exe whose resource directory points outside its only section.
   */
  public static Path writeResourceOutsideSections(Path file) throws IOException {
    Path written = write(file, true, "faf-re");
    byte[] exe = Files.readAllBytes(written);
    ByteBuffer.wrap(exe).order(ByteOrder.LITTLE_ENDIAN).putInt(PE_HEADER + 24 + 112 + 2 * 8, 0x9000);
    return Files.write(written, exe);
  }

  /**
   * A file that starts like an exe (MZ), but has no PE header where the DOS header points.
   */
  public static Path writeWithoutPeSignature(Path file) throws IOException {
    Path written = write(file, true, "faf-re");
    byte[] exe = Files.readAllBytes(written);
    ByteBuffer.wrap(exe).order(ByteOrder.LITTLE_ENDIAN).putInt(PE_HEADER, 0);
    return Files.write(written, exe);
  }

  private static Path write(Path file, boolean pe32Plus, byte[] resources) throws IOException {
    byte[] section = resources == null ? new byte[0] : resources;
    int optionalHeaderSize = pe32Plus ? 240 : 224;

    ByteBuffer exe = ByteBuffer.allocate(SECTION_OFFSET + section.length).order(ByteOrder.LITTLE_ENDIAN);
    exe.putShort(0, (short) 0x5A4D).putInt(0x3C, PE_HEADER);
    exe.putInt(PE_HEADER, 0x4550)
        .putShort(PE_HEADER + 4, (short) (pe32Plus ? 0x8664 : 0x14C))
        .putShort(PE_HEADER + 6, (short) 1)
        .putShort(PE_HEADER + 20, (short) optionalHeaderSize);
    int optionalHeader = PE_HEADER + 24;
    exe.putShort(optionalHeader, (short) (pe32Plus ? 0x20B : 0x10B));
    if (resources != null) {
      int resourceDirectory = optionalHeader + (pe32Plus ? 112 : 96) + 2 * 8;
      exe.putInt(resourceDirectory, SECTION_RVA).putInt(resourceDirectory + 4, section.length);
    }
    int sectionHeader = optionalHeader + optionalHeaderSize;
    exe.put(sectionHeader, ".rsrc".getBytes(StandardCharsets.US_ASCII))
        .putInt(sectionHeader + 8, section.length)
        .putInt(sectionHeader + 12, SECTION_RVA)
        .putInt(sectionHeader + 16, section.length)
        .putInt(sectionHeader + 20, SECTION_OFFSET);
    exe.put(SECTION_OFFSET, section);
    return Files.write(file, exe.array());
  }

  /**
   * One resource of this type, id 1, language 0x409, followed by its data.
   */
  private static byte[] resourceSection(int type, byte[] data) {
    ByteBuffer section = ByteBuffer.allocate(88 + data.length).order(ByteOrder.LITTLE_ENDIAN);
    directory(section, 0, type, 0x80000000 | 24);
    directory(section, 24, 1, 0x80000000 | 48);
    directory(section, 48, 0x409, 72);
    section.putInt(72, SECTION_RVA + 88).putInt(76, data.length);
    section.put(88, data);
    return section.array();
  }

  private static void directory(ByteBuffer section, int offset, int id, int target) {
    section.putShort(offset + 14, (short) 1).putInt(offset + 16, id).putInt(offset + 20, target);
  }

  private static byte[] versionInfo(String productName) {
    byte[] stringTable = node("040904b0", 1, new byte[0], 0, string("ProductName", productName));
    byte[] stringFileInfo = node("StringFileInfo", 1, new byte[0], 0, stringTable);
    return node("VS_VERSION_INFO", 0, new byte[52], 52, stringFileInfo);
  }

  private static byte[] string(String key, String value) {
    return node(key, 1, text(value), value.length() + 1);
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
