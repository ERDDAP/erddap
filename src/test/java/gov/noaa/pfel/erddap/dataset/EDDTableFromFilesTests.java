package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.String2;
import com.cohort.util.Test;
import org.junit.jupiter.api.BeforeAll;
import testDataset.Initialization;

class EDDTableFromFilesTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** Test isOK() */
  @org.junit.jupiter.api.Test
  void testIsOK() {
    String2.log("\n* EDDTableFromFiles.testIsOK");
    // 0"!=", 1REGEX_OP, 2"<=", 3">=", 4"=", 5"<", 6">"};
    // isOK(String min, String max, int hasNaN, String conOp, String conValue) {
    // isOK(double min, double max, int hasNaN, String conOp, double conValue) {
    String ROP = PrimitiveArray.REGEX_OP;

    Test.ensureEqual(
        String2.max("a", ""), "a", ""); // "" sorts lower than any string with characters

    // simple tests String
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, "=", "c"), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, "=", "5"), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, "!=", "c"), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, "!=", "5"), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, "<=", "|"), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, "<=", "c"), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, "<=", "a"), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, "<=", "5"), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, "<", "|"), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, "<", "c"), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, "<", "a"), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, "<", "5"), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, ">=", "|"), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, ">=", "z"), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, ">=", "c"), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, ">=", "5"), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, ">", "|"), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, ">", "z"), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, ">", "c"), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, ">", "5"), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, ROP, "(5)"), true, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK("a", "a", 0, ROP, "(a)"), true, ""); // only really tests if min=max
    Test.ensureEqual(
        EDDTableFromFiles.isOK("a", "a", 0, ROP, "(5)"), false, ""); // only really tests if min=max

    // simple tests numeric
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "=", 3), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "=", 0), false, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "=", 1.99999), false, ""); // important
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "=", 1.999999), true, ""); // important
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "!=", 3), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "!=", 0), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "<=", 6), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "<=", 3), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "<=", 2.0000000001), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "<=", 2), true, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "<=", 1.99999), false, ""); // important
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "<=", 1.999999), true, ""); // important
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "<=", 0), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "<", 6), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "<", 3), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "<", 2.0000000001), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "<", 2), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "<", 1.9999999999), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "<", 0), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, ">=", 6), false, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, ">=", 4.0001), false, ""); // important
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, ">=", 4.00001), true, ""); // important
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, ">=", 4), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, ">=", 3.9999999999), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, ">=", 3), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, ">=", 0), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, ">", 6), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, ">", 4.0000000001), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, ">", 4), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, ">", 3.9999999999), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, ">", 3), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, ">", 0), true, "");

    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.DOUBLE, 2, 4, 0, "=", 1.99999999), false, ""); // important
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.DOUBLE, 2, 4, 0, "=", 1.999999999), true, ""); // important
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.DOUBLE, 2, 4, 0, "<=", 2.0000000001), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.DOUBLE, 2, 4, 0, "<=", 2), true, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.DOUBLE, 2, 4, 0, "<=", 1.99999999), false, ""); // important
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.DOUBLE, 2, 4, 0, "<=", 1.999999999), true, ""); // important
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.DOUBLE, 2, 4, 0, "<", 2.0000000001), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.DOUBLE, 2, 4, 0, "<", 2), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.DOUBLE, 2, 4, 0, "<", 1.9999999999), false, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.DOUBLE, 2, 4, 0, ">=", 4.00000001), false, ""); // important
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.DOUBLE, 2, 4, 0, ">=", 4.000000001), true, ""); // important
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.DOUBLE, 2, 4, 0, ">=", 4), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.DOUBLE, 2, 4, 0, ">=", 3.9999999999), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.DOUBLE, 2, 4, 0, ">", 4.0000000001), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.DOUBLE, 2, 4, 0, ">", 4), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.DOUBLE, 2, 4, 0, ">", 3.9999999999), true, "");

    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.INT, 2, 4, 0, "=", 1.9999999999999), false, ""); // important
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.INT, 2, 4, 0, "<=", 2.0000000001), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.INT, 2, 4, 0, "<=", 2), true, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.INT, 2, 4, 0, "<=", 1.9999999999999), false, ""); // important
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.INT, 2, 4, 0, "<", 2.0000000001), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.INT, 2, 4, 0, "<", 2), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.INT, 2, 4, 0, "<", 1.9999999999), false, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.INT, 2, 4, 0, ">=", 4.0000000000001), false, ""); // important
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.INT, 2, 4, 0, ">=", 4), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.INT, 2, 4, 0, ">=", 3.9999999999), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.INT, 2, 4, 0, ">", 4.0000000001), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.INT, 2, 4, 0, ">", 4), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.INT, 2, 4, 0, ">", 3.9999999999), true, "");

    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.STRING, 2, 4, 0, "=", 1.9999999999999),
        false,
        ""); // important
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.STRING, 2, 4, 0, "<=", 2.0000000001), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.STRING, 2, 4, 0, "<=", 2), true, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.STRING, 2, 4, 0, "<=", 1.9999999999999),
        false,
        ""); // important
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.STRING, 2, 4, 0, "<", 2.0000000001), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.STRING, 2, 4, 0, "<", 2), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.STRING, 2, 4, 0, "<", 1.9999999999), false, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.STRING, 2, 4, 0, ">=", 4.0000000000001),
        false,
        ""); // important
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.STRING, 2, 4, 0, ">=", 4), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.STRING, 2, 4, 0, ">=", 3.9999999999), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.STRING, 2, 4, 0, ">", 4.0000000001), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.STRING, 2, 4, 0, ">", 4), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.STRING, 2, 4, 0, ">", 3.9999999999), true, "");

    Test.ensureEqual(EDDTableFromFiles.isOK("2", "4", 0, ROP, "(5)"), true, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK("2", "2", 0, ROP, "(2)"), true, ""); // only really tests if min=max
    Test.ensureEqual(
        EDDTableFromFiles.isOK("2", "2", 0, ROP, "(5)"), false, ""); // only really tests if min=max

    // value="" tests String hasNaN=0=false
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, "=", ""), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, "!=", ""), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, "<=", ""), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, "<", ""), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, ">=", ""), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, ">", ""), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 0, ROP, ""), true, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK("a", "a", 0, ROP, ""), false, ""); // only really tests if min=max

    // value=NaN tests numeric hasNaN=0=false
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "=", Double.NaN), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "!=", Double.NaN), true, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "<=", Double.NaN),
        false,
        ""); // NaN tests other
    // than = !=
    // return false
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, "<", Double.NaN),
        false,
        ""); // NaN tests other
    // than = != return
    // false
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, ">=", Double.NaN),
        false,
        ""); // NaN tests other
    // than = !=
    // return false
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 0, ">", Double.NaN),
        false,
        ""); // NaN tests other
    // than = != return
    // false
    Test.ensureEqual(EDDTableFromFiles.isOK("2", "4", 0, ROP, ""), true, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK("2", "2", 0, ROP, ""), false, ""); // only really tests if min=max

    // value="" tests String hasNaN=1=true
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 1, "=", ""), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 1, "!=", ""), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 1, "<=", ""), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 1, "<", ""), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 1, ">=", ""), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 1, ">", ""), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("a", "z", 1, ROP, ""), true, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK("a", "a", 1, ROP, ""), true, ""); // only really tests if min=max

    // value=NaN tests numeric hasNaN=1=true
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 1, "=", Double.NaN), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 1, "!=", Double.NaN), true, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 1, "<=", Double.NaN), true, ""); // =
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 1, "<", Double.NaN),
        false,
        ""); // NaN tests other
    // than = != return
    // false
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 1, ">=", Double.NaN), true, ""); // =
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, 2, 4, 1, ">", Double.NaN),
        false,
        ""); // NaN tests other
    // than = != return
    // false
    Test.ensureEqual(EDDTableFromFiles.isOK("2", "4", 1, ROP, ""), true, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK("2", "2", 1, ROP, ""), true, ""); // only really tests if min=max

    // *** DATA IS ALL "" hasNaN must be 1
    // DATA IS ALL "" value="c" tests String
    Test.ensureEqual(EDDTableFromFiles.isOK("", "", 1, "=", "c"), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("", "", 1, "!=", "c"), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("", "", 1, "<=", "c"), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("", "", 1, "<", "c"), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("", "", 1, ">=", "c"), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("", "", 1, ">", "c"), false, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK("", "", 1, ROP, "(c)"), false, ""); // only really tests if min=max

    // DATA IS ALL "" value=5 tests numeric
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, Double.NaN, Double.NaN, 1, "=", 5), false, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, Double.NaN, Double.NaN, 1, "!=", 5), true, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, Double.NaN, Double.NaN, 1, "<=", 5), false, ""); // NaN
    // tests
    // other
    // than =
    // !=
    // return
    // false
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, Double.NaN, Double.NaN, 1, "<", 5), false, ""); // NaN
    // tests
    // other
    // than =
    // !=
    // return
    // false
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, Double.NaN, Double.NaN, 1, ">=", 5), false, ""); // NaN
    // tests
    // other
    // than =
    // !=
    // return
    // false
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, Double.NaN, Double.NaN, 1, ">", 5), false, ""); // NaN
    // tests
    // other
    // than =
    // !=
    // return
    // false
    Test.ensureEqual(EDDTableFromFiles.isOK("", "", 1, ROP, ""), true, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK("", "", 1, ROP, ""), true, ""); // only really tests if min=max

    // DATA IS ALL "" value="" tests String hasNaN=1=true
    Test.ensureEqual(EDDTableFromFiles.isOK("", "", 1, "=", ""), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("", "", 1, "!=", ""), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("", "", 1, "<=", ""), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("", "", 1, "<", ""), false, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("", "", 1, ">=", ""), true, "");
    Test.ensureEqual(EDDTableFromFiles.isOK("", "", 1, ">", ""), false, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK("", "", 1, ROP, ""), true, ""); // only really tests if min=max

    // DATA IS ALL "" value=NaN tests numeric hasNaN=1=true
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, Double.NaN, Double.NaN, 1, "=", Double.NaN), true, "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, Double.NaN, Double.NaN, 1, "!=", Double.NaN),
        false,
        "");
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, Double.NaN, Double.NaN, 1, "<=", Double.NaN),
        true,
        ""); // =
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, Double.NaN, Double.NaN, 1, "<", Double.NaN),
        false,
        ""); // NaN
    // tests
    // other
    // than
    // =
    // !=
    // return
    // false
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, Double.NaN, Double.NaN, 1, ">=", Double.NaN),
        true,
        ""); // =
    Test.ensureEqual(
        EDDTableFromFiles.isOK(PAType.FLOAT, Double.NaN, Double.NaN, 1, ">", Double.NaN),
        false,
        ""); // NaN
    // tests
    // other
    // than
    // =
    // !=
    // return
    // false
    Test.ensureEqual(
        EDDTableFromFiles.isOK("", "", 1, ROP, ""), true, ""); // only really tests if min=max
  }

  /** Quick test of regex */
  @org.junit.jupiter.api.Test
  void testRegex() {

    String2.log("\n*** EDDTableFromFiles.testRegex()");
    String s = "20070925_41001_5day.csv";
    Test.ensureEqual(String2.extractRegex(s, "^[0-9]{8}_", 0), "20070925_", "");
    Test.ensureEqual(String2.extractRegex(s, "_5day\\.csv$", 0), "_5day.csv", "");
  }
}
