package com.cohort.util;

class HashDigestTests {
  @org.junit.jupiter.api.Test
  /** This tests this class. */
  void basicTest() throws Throwable {
    System.out.println("*** HashDigest.basicTest");
    String tName = HashDigestTests.class.getResource("/data/LICENSE.txt").getPath();
    Test.ensureEqual(
        HashDigest.doIt(new String[] {"type:MD5"}),
        "Neither password or filename was specified.\n" + HashDigest.usage,
        "");
    Test.ensureEqual(
        HashDigest.doIt(new String[] {"password:myPassword", "type:MD-5"}),
        "Invalid algorithm.\n" + HashDigest.usage,
        "");
    Test.ensureEqual(
        HashDigest.doIt(new String[] {"password:myPassword", "type:MD5"}),
        "deb1536f480475f7d593219aa1afd74c",
        "");
    Test.ensureEqual(
        HashDigest.doIt(new String[] {"password:myPassword", "type:SHA-1"}),
        "5413ee24723bba2c5a6ba2d0196c78b3ee4628d1",
        "");
    Test.ensureEqual(
        HashDigest.doIt(new String[] {"password:myPassword", "type:SHA-256"}),
        "76549b827ec46e705fd03831813fa52172338f0dfcbd711ed44b81a96dac51c6",
        "");
    Test.ensureEqual(
        HashDigest.doIt(new String[] {"filename:" + tName, "type:MD5"}),
        "96e9b4d974b734874a66f433b276a41a",
        "");
    Test.ensureEqual(
        HashDigest.doIt(new String[] {"filename:" + tName, "type:SHA-256"}),
        "f6afea3f5fa69c8e54b14b3fdb86a9aa261a423cea957c9c87b55b92441d56bb",
        "");
    Test.ensureEqual(
        HashDigest.doIt(new String[] {"filename:" + tName, "type:SHA-256", "-file"}),
        "Created " + tName + ".sha256",
        "");
    Test.ensureEqual(
        File2.readFromFileUtf8(tName + ".sha256")[1],
        "f6afea3f5fa69c8e54b14b3fdb86a9aa261a423cea957c9c87b55b92441d56bb  LICENSE.txt\n",
        "");
    File2.delete(tName + ".sha256");
  }
}
