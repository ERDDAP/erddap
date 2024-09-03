package com.cohort.array;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import java.io.RandomAccessFile;

class PAOneTests {
  /** This tests the methods in this class. */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    String2.log("\n*** PAOne.basicTest()");

    ByteArray ba = new ByteArray(new byte[] {-128, 0, 127});
    ShortArray sa = new ShortArray(new short[] {-32768, 0, 32767});
    PAOne bo = new PAOne(sa, 0);
    Test.ensureEqual(bo.toString(), "-32768", "");
    Test.ensureEqual(bo.compareTo(ba, 0), -1, "");
    bo.readFrom(sa, 1);
    Test.ensureEqual(bo.toString(), "0", "");
    Test.ensureEqual(bo.compareTo(ba, 0), 1, "");
    Test.ensureEqual(bo.compareTo(sa, 0), 1, "");
    Test.ensureEqual(bo.compareTo(sa, 1), 0, "");
    Test.ensureEqual(bo.compareTo(sa, 2), -1, "");

    Test.ensureEqual(ba.missingValue().getString(), "127", "");
    Test.ensureEqual(ba.missingValue().toString(), "127", "");

    bo.addTo(sa);
    Test.ensureEqual(sa.toString(), "-32768, 0, 32767, 0", "");

    // constructors
    PAOne paOne = new PAOne(PAType.BYTE, "-128");
    Test.ensureEqual(paOne.paType(), PAType.BYTE, "");
    Test.ensureEqual(paOne.getInt(), -128, "");

    // raf test
    String raf2Name = File2.getSystemTempDirectory() + "PAOneTest.bin";
    String2.log("rafName=" + raf2Name);
    File2.delete(raf2Name);
    Test.ensureEqual(File2.isFile(raf2Name), false, "");

    RandomAccessFile raf2 = new RandomAccessFile(raf2Name, "rw");
    paOne = new PAOne(ba);
    long bStart = raf2.getFilePointer();
    for (int i = 0; i < 3; i++) {
      paOne.readFrom(ba, i);
      paOne.writeToRAF(raf2); // at current position
    }

    paOne = new PAOne(sa);
    long sStart = raf2.getFilePointer();
    for (int i = 0; i < 4; i++) {
      paOne.readFrom(sa, i);
      paOne.writeToRAF(raf2, sStart, i); // at specified position
    }

    paOne = new PAOne(sa);
    raf2.seek(sStart);
    for (int i = 0; i < 4; i++) {
      paOne.readFromRAF(raf2); // at current position
      Test.ensureEqual(paOne, "" + sa.get(i), "i=" + i);
    }

    paOne = new PAOne(ba);
    raf2.seek(bStart);
    for (int i = 0; i < 3; i++) {
      paOne.readFromRAF(raf2); // at current position
      Test.ensureEqual(paOne, "" + ba.get(i), "i=" + i);
    }

    paOne = new PAOne(ba);
    for (int i = 0; i < 3; i++) {
      paOne.readFromRAF(raf2, bStart, i); // at specified position
      Test.ensureEqual(paOne, "" + ba.get(i), "i=" + i);
    }

    // test almostEqual

  }
}
