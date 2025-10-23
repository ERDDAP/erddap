package gov.noaa.pfel.coastwatch.util;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
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
    String fileName = SSR.getTempDirectory() + "TestDataStream";
    DataOutputStream dos =
        new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
    dos.writeByte(1);
    dos.writeByte(-1);
    byte ba[] = {(byte) 1, (byte) 91, (byte) -5};
    dos.write(ba, 0, ba.length);
    dos.close();

    // read bytes from a file
    String2.log("hex dump of " + fileName + "\n" + File2.hexDump(fileName, 500));

    DataInputStream dis = new DataInputStream(File2.getDecompressedBufferedInputStream(fileName));
    Test.ensureEqual(1, dis.readByte(), "a");
    Test.ensureEqual(-1, dis.readByte(), "b");
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
