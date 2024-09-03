package com.cohort.util;

import java.io.StringReader;

class ResourceBundle2Tests {
  /**
   * Test the methods in this class.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    String2.log("\n*** ResourceBundle2.basicTest");

    ResourceBundle2 rb2 =
        ResourceBundle2.fromXml(
            XML.parseXml(
                new StringReader(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<testr>\n"
                        + "  <level1 att1=\"value1\" att2=\"value 2\" > level 1 &amp; <!-- comment < > -->text  \n"
                        + "  </level1>\n"
                        + "  <levela />\n"
                        + // "empty tag" appears as two tags, begin and end
                        "  <levelb> 16</levelb>\n"
                        + "  <bool> true</bool>\n"
                        + "  <dbl> 17.1</dbl>\n"
                        + "\n"
                        + "\n"
                        + "\n"
                        + "</testr>"),
                false));
    Test.ensureEqual(rb2.getString("level1", ""), "level 1 & text", "");
    Test.ensureEqual(rb2.getString("levela", ""), "", "");
    Test.ensureEqual(rb2.getBoolean("bool", false), true, "");
    Test.ensureEqual(rb2.getInt("levelb", 5), 16, "");
    Test.ensureEqual(rb2.getLong("levelb", 5), 16, "");
    Test.ensureEqual(rb2.getDouble("dbl", 5.5), 17.1, "");
    Test.ensureEqual(rb2.getString("testr", ""), "", "");

    Test.ensureEqual(rb2.getBoolean("Z", true), true, "");
    Test.ensureEqual(rb2.getInt("Z", 5), 5, "");
    Test.ensureEqual(rb2.getDouble("Z", 5.5), 5.5, "");
    Test.ensureEqual(rb2.getString("Z", "word"), "word", "");
  }
}
