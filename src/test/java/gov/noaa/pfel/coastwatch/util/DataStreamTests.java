package gov.noaa.pfel.coastwatch.util;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import org.junit.jupiter.api.BeforeAll;
import testDataset.Initialization;

class DataStreamTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** This runs a small test of the methods in this class. */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    // write bytes to a file
    String fileName =
        EDStatic.getWebInfParentDirectory()
            + // with / separator and / at the end
            "WEB-INF/temp/TestDataStream";
    DataOutputStream dos =
        new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
    byte buffer[] = new byte[128];
    dos.writeByte(1);
    dos.writeByte(-1);
    DataStream.writeShort(false, dos, 30003);
    DataStream.writeShort(false, dos, -30003);
    DataStream.writeShort(true, dos, 30003);
    DataStream.writeShort(true, dos, -30003);
    DataStream.writeInt(false, dos, 1000000003);
    DataStream.writeInt(false, dos, -1000000003);
    DataStream.writeInt(true, dos, 1000000003);
    DataStream.writeInt(true, dos, -1000000003);
    DataStream.writeLong(false, dos, 1000000003006007L);
    DataStream.writeLong(false, dos, -1000000003006007L);
    DataStream.writeLong(true, dos, 1000000003006007L);
    DataStream.writeLong(true, dos, -1000000003006007L);
    DataStream.writeFloat(false, dos, 123.456e29f);
    DataStream.writeFloat(false, dos, -123.456e29f);
    DataStream.writeFloat(true, dos, 123.456e29f);
    DataStream.writeFloat(true, dos, -123.456e29f);
    DataStream.writeDouble(false, dos, 123.45678987e294);
    DataStream.writeDouble(false, dos, -123.45678987e294);
    DataStream.writeDouble(true, dos, 123.45678987e294);
    DataStream.writeDouble(true, dos, -123.45678987e294);
    String s = "This is a\ntest!";
    DataStream.writeZString(dos, s);
    byte ba[] = {(byte) 1, (byte) 91, (byte) -5};
    dos.write(ba, 0, ba.length);
    dos.close();

    // read bytes from a file
    String2.log("hex dump of " + fileName + "\n" + File2.hexDump(fileName, 500));

    DataInputStream dis = new DataInputStream(File2.getDecompressedBufferedInputStream(fileName));
    Test.ensureEqual(1, dis.readByte(), "a");
    Test.ensureEqual(-1, dis.readByte(), "b");
    Test.ensureEqual(30003, DataStream.readShort(false, dis, buffer), "e");
    Test.ensureEqual(-30003, DataStream.readShort(false, dis, buffer), "f");
    Test.ensureEqual(30003, DataStream.readShort(true, dis, buffer), "c");
    Test.ensureEqual(-30003, DataStream.readShort(true, dis, buffer), "d");
    Test.ensureEqual(1000000003, DataStream.readInt(false, dis, buffer), "i");
    Test.ensureEqual(-1000000003, DataStream.readInt(false, dis, buffer), "j");
    Test.ensureEqual(1000000003, DataStream.readInt(true, dis, buffer), "g");
    Test.ensureEqual(-1000000003, DataStream.readInt(true, dis, buffer), "h");
    Test.ensureEqual(1000000003006007L, DataStream.readLong(false, dis, buffer), "m");
    Test.ensureEqual(-1000000003006007L, DataStream.readLong(false, dis, buffer), "n");
    Test.ensureEqual(1000000003006007L, DataStream.readLong(true, dis, buffer), "k");
    Test.ensureEqual(-1000000003006007L, DataStream.readLong(true, dis, buffer), "l");
    Test.ensureEqual(123.456e29f, DataStream.readFloat(false, dis, buffer), "q");
    Test.ensureEqual(-123.456e29f, DataStream.readFloat(false, dis, buffer), "r");
    Test.ensureEqual(123.456e29f, DataStream.readFloat(true, dis, buffer), "o");
    Test.ensureEqual(-123.456e29f, DataStream.readFloat(true, dis, buffer), "p");
    Test.ensureEqual(123.45678987e294, DataStream.readDouble(false, dis, buffer), "u");
    Test.ensureEqual(-123.45678987e294, DataStream.readDouble(false, dis, buffer), "v");
    Test.ensureEqual(123.45678987e294, DataStream.readDouble(true, dis, buffer), "s");
    Test.ensureEqual(-123.45678987e294, DataStream.readDouble(true, dis, buffer), "t");
    Test.ensureEqual(s, DataStream.readZString(dis), "w");
    byte ba2[] = new byte[ba.length];
    dis.readFully(ba2, 0, ba.length);
    Test.ensureEqual(ba.length, ba2.length, "x");
    for (int i = 0; i < ba.length; i++) Test.ensureEqual(ba[i], ba2[i], "y" + i);
    dis.close();

    // do speed tests    array vs. individual reads
    // This is important because many methods in this class read several
    // bytes at a time.
    dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
    ba2 = new byte[] {1, 27, 86, 44};
    int myByte = 87;
    for (int i = 0; i < 100000; i++) { // warmup
      dos.write(myByte);
      dos.write(ba2);
    }

    // write individual bytes   typical = 562ms
    long time = System.currentTimeMillis();
    for (int i = 0; i < 4000000; i++) dos.write(myByte);
    String2.log("4000000 byte writes, time=" + (System.currentTimeMillis() - time) + "ms");

    // write individual bytes   typical = 562ms
    time = System.currentTimeMillis();
    for (int i = 0; i < 1000000; i++) dos.write(ba2);
    String2.log("1000000 byte[4] writes, time=" + (System.currentTimeMillis() - time) + "ms");
    dos.close();

    // delete the temp file
    Test.ensureTrue(File2.delete(fileName), "error while deleting " + fileName);
  }
}
