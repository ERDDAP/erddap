package com.cohort.array;

import com.cohort.util.String2;
import com.cohort.util.Test;

class AttributesTests {
  /**
   * This tests the methods in this class.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    String2.log("\n*** Attributes.basicTest()");

    // set and size
    Attributes atts = new Attributes();
    String results;
    Test.ensureEqual(atts.size(), 0, "");
    atts.set("byte", (byte) 1);
    atts.set("char", '\u20ac');
    atts.set("short", (short) 3000);
    atts.set("int", 1000000);
    atts.set("long", 1000000000000L);
    atts.set("float", 2.5f);
    atts.set("double", Math.PI);
    atts.set("String1", "a, csv, ÿ\u20ac, string"); // 1 string with csv content
    atts.set("String4", StringArray.fromCSV("a, csv, ÿ\u20ac, string")); // 4 strings
    atts.set("PA", new IntArray(new int[] {1, 2, 3}));

    Test.ensureEqual(atts.size(), 10, "");

    // add and remove an item
    Test.ensureEqual(atts.set("zz", new IntArray(new int[] {1})), null, "");
    Test.ensureEqual(atts.size(), 11, "");

    Test.ensureEqual(atts.set("zz", (PrimitiveArray) null), new IntArray(new int[] {1}), "");
    Test.ensureEqual(atts.size(), 10, "");

    // add and remove an item
    Test.ensureEqual(atts.set("zz", new IntArray(new int[] {2})), null, "");
    Test.ensureEqual(atts.size(), 11, "");

    Test.ensureEqual(atts.remove("zz"), new IntArray(new int[] {2}), "");
    Test.ensureEqual(atts.size(), 10, "");

    // empty string same as null; attribute removed
    atts.set("zz", "a");
    Test.ensureEqual(atts.size(), 11, "");
    atts.set("zz", "");
    Test.ensureEqual(atts.size(), 10, "");

    // get
    Test.ensureEqual(atts.get("byte"), new ByteArray(new byte[] {(byte) 1}), "");
    Test.ensureEqual(atts.get("char"), new CharArray(new char[] {'\u20ac'}), "");
    Test.ensureEqual(atts.get("short"), new ShortArray(new short[] {(short) 3000}), "");
    Test.ensureEqual(atts.get("int"), new IntArray(new int[] {1000000}), "");
    Test.ensureEqual(atts.get("long"), new LongArray(new long[] {1000000000000L}), "");
    Test.ensureEqual(atts.get("float"), new FloatArray(new float[] {2.5f}), "");
    Test.ensureEqual(atts.get("double"), new DoubleArray(new double[] {Math.PI}), "");
    Test.ensureEqual(
        atts.get("String1"),
        new StringArray(new String[] {"a, csv, ÿ\u20ac, string"}),
        ""); // 1 string
    // with csv
    // content
    Test.ensureEqual(
        atts.get("String4"), StringArray.fromCSV("a, csv, ÿ\u20ac, string"), ""); // 4 strings
    Test.ensureEqual(atts.get("PA"), new IntArray(new int[] {1, 2, 3}), "");

    Test.ensureEqual(
        atts.get("String1").toString(),
        "\"a, csv, \\u00ff\\u20ac, string\"",
        ""); // 1 string with csv
    // content
    Test.ensureEqual(
        atts.get("String1").toNccsvAttString(),
        "\"a, csv, ÿ\u20ac, string\"",
        ""); // 1 string with csv
    // content
    Test.ensureEqual(
        atts.get("String1").toNccsv127AttString(),
        "\"a, csv, \\u00ff\\u20ac, string\"",
        ""); // 1 string
    // with csv
    // content

    Test.ensureEqual(
        atts.get("String4").toString(), "a, csv, \\u00ff\\u20ac, string", ""); // 4 strings
    Test.ensureEqual(
        atts.get("String4").toNccsvAttString(), "a\\ncsv\\nÿ\u20ac\\nstring", ""); // 4 strings
    Test.ensureEqual(
        atts.get("String4").toNccsv127AttString(),
        "a\\ncsv\\n\\u00ff\\u20ac\\nstring",
        ""); // 4 strings

    Test.ensureEqual(atts.getInt("byte"), 1, "");
    Test.ensureEqual(atts.getInt("char"), 8364, "");
    Test.ensureEqual(atts.getInt("short"), 3000, "");
    Test.ensureEqual(atts.getInt("int"), 1000000, "");
    Test.ensureEqual(atts.getLong("int"), 1000000, "");
    Test.ensureEqual(atts.getString("int"), "1000000", "");
    Test.ensureEqual(atts.getLong("long"), 1000000000000L, "");
    Test.ensureEqual(atts.getInt("long"), Integer.MAX_VALUE, "");
    Test.ensureEqual(atts.getFloat("float"), 2.5f, "");
    Test.ensureEqual(atts.getDouble("float"), 2.5f, "");
    Test.ensureEqual(atts.getString("float"), "2.5", "");
    Test.ensureEqual(atts.getDouble("double"), Math.PI, "");
    Test.ensureEqual(atts.getInt("double"), 3, "");
    Test.ensureEqual(atts.getString("String1"), "a, csv, ÿ\u20ac, string", "");
    Test.ensureEqual(
        atts.getStringsFromCSV("String1"), new String[] {"a", "csv", "ÿ\u20ac", "string"}, "");
    Test.ensureEqual(atts.getInt("String1"), Integer.MAX_VALUE, "");
    Test.ensureEqual(atts.get("PA"), new IntArray(new int[] {1, 2, 3}), "");
    Test.ensureEqual(atts.getInt("PA"), 1, "");
    Test.ensureEqual(atts.getDouble("PA"), 1, "");
    Test.ensureEqual(atts.getString("PA"), "1", "");

    // getNames
    Test.ensureEqual(
        atts.getNames(),
        new String[] {
          "byte", "char", "double", "float", "int", "long", "PA", "short", "String1", "String4"
        },
        "");

    // toString
    results = atts.toString();
    Test.ensureEqual(
        results,
        "    byte=1b\n"
            + "    char=\"'\\u20ac'\"\n"
            + "    double=3.141592653589793d\n"
            + "    float=2.5f\n"
            + "    int=1000000i\n"
            + "    long=1000000000000L\n"
            + "    PA=1i,2i,3i\n"
            + "    short=3000s\n"
            + "    String1=\"a, csv, \\u00ff\\u20ac, string\"\n"
            + "    String4=a\\ncsv\\n\\u00ff\\u20ac\\nstring\n",
        "results=\n" + results);

    // clone
    Attributes atts2 = (Attributes) atts.clone();
    Test.ensureEqual(atts2.get("byte"), new ByteArray(new byte[] {(byte) 1}), "");
    Test.ensureEqual(atts2.get("char"), new CharArray(new char[] {'\u20ac'}), "");
    Test.ensureEqual(atts2.get("short"), new ShortArray(new short[] {(short) 3000}), "");
    Test.ensureEqual(atts2.get("int"), new IntArray(new int[] {1000000}), "");
    Test.ensureEqual(atts2.get("long"), new LongArray(new long[] {1000000000000L}), "");
    Test.ensureEqual(atts2.get("float"), new FloatArray(new float[] {2.5f}), "");
    Test.ensureEqual(atts2.get("double"), new DoubleArray(new double[] {Math.PI}), "");
    Test.ensureEqual(
        atts2.get("String1"), new StringArray(new String[] {"a, csv, ÿ\u20ac, string"}), "");
    Test.ensureEqual(atts2.get("String4"), StringArray.fromCSV("a, csv, ÿ\u20ac, string"), "");
    Test.ensureEqual(atts2.get("PA"), new IntArray(new int[] {1, 2, 3}), "");

    Test.ensureEqual(
        atts2.getNames(),
        new String[] {
          "byte", "char", "double", "float", "int", "long", "PA", "short", "String1", "String4"
        },
        "");

    // clear
    atts2.clear();
    Test.ensureEqual(atts2.getNames(), new String[] {}, "");

    // copyTo
    atts.copyTo(atts2);
    Test.ensureEqual(
        atts2.getNames(),
        new String[] {
          "byte", "char", "double", "float", "int", "long", "PA", "short", "String1", "String4"
        },
        "");
    Test.ensureEqual(
        atts2.get("String1"),
        new StringArray(new String[] {"a, csv, \u00ff\u20ac, string"}),
        ""); // 1
    // string
    // with
    // csv
    // content
    Test.ensureEqual(
        atts2.get("String4"), StringArray.fromCSV("a, csv, \u00ff\u20ac, string"), ""); // 4 strings

    // equals
    Test.ensureTrue(atts.equals(atts2), "");

    // add
    Attributes atts3 =
        new Attributes()
            .add("byte", (byte) 1)
            .add("char", '\u20ac')
            .add("short", (short) 3000)
            .add("int", 1000000)
            .add("long", 1000000000000L)
            .add("float", 2.5f)
            .add("double", Math.PI)
            .add("String1", "a, csv, \u00ff\u20ac, string")
            .add("String4", StringArray.fromCSV("a, csv, \u00ff\u20ac, string"))
            .add("PA", new IntArray(new int[] {1, 2, 3}));
    Test.ensureTrue(atts3.equals(atts), "");

    // set(attributes)
    Attributes atts4 =
        new Attributes()
            .add("zztop", 77) // new name
            .add("char", 'd') // diff value
            .add("short", (short) 3000); // same value
    atts3.set(atts4);
    results = atts3.toString();
    Test.ensureEqual(
        results,
        "    byte=1b\n"
            + "    char=\"'d'\"\n"
            + "    double=3.141592653589793d\n"
            + "    float=2.5f\n"
            + "    int=1000000i\n"
            + "    long=1000000000000L\n"
            + "    PA=1i,2i,3i\n"
            + "    short=3000s\n"
            + "    String1=\"a, csv, \\u00ff\\u20ac, string\"\n"
            + // 1 string with 4 values
            "    String4=a\\ncsv\\n\\u00ff\\u20ac\\nstring\n"
            + // 4 strings
            "    zztop=77i\n",
        "results=\n" + results);

    // _FillValue
    atts.clear();
    atts.set("_FillValue", new ShortArray(new short[] {(short) 32767}));
    String2.log("atts.size()=" + atts.size());
    PrimitiveArray fv = atts.get("_FillValue");
    if (fv == null || fv.size() != 1 || !(fv instanceof ShortArray))
      throw new Exception("fv=" + fv);
    atts.remove("_FillValue");

    // keepIfDifferent
    atts.clear();
    atts.add("a", 1);
    atts.add("b", "2");
    atts.add("c", 3f);
    atts2.clear();
    atts2.add("b", "2");
    atts2.add("c", 3d); // changed to d
    atts2.add("d", 4d);
    atts.removeIfSame(atts2);
    Test.ensureEqual(
        atts.toString(),
        "    a=1i\n" + "    c=3.0f\n", // different type
        "");

    // trim
    atts.clear();
    atts.add(" a ", " A ");
    atts.add("b ", "B");
    atts.add("c", "C");
    atts.add("d", 4);
    atts.trim();
    results = atts.toString();
    Test.ensureEqual(
        results, "    a=A\n" + "    b=B\n" + "    c=C\n" + "    d=4i\n", "results=\n" + results);

    // trimIfNeeded
    atts.clear();
    atts.add("a", " A ");
    atts.add("b", "B");
    atts.add("c", " C ");
    atts.add("d", "D");
    atts.add("z4", 4);
    atts2.clear();
    atts.add("a", "A");
    atts.add("b", " B ");
    atts.add("e", " E ");
    atts.add("f", "F");
    atts.add("z5", 5);
    atts.trimIfNeeded(atts2);
    results = atts.toString();
    Test.ensureEqual(
        results,
        "    a=A\n"
            + "    b=B\n"
            + "    c=C\n"
            + "    d=D\n"
            + "    e=E\n"
            + "    f=F\n"
            + "    z4=4i\n"
            + "    z5=5i\n",
        "results=\n" + results);

    // makeALikeB
    Attributes a = new Attributes();
    a.set("s", new StringArray(new String[] {"theString"})); // same
    a.set("number", new DoubleArray(new double[] {11, 22})); // different
    a.set("inA", new IntArray(new int[] {1, 2})); // unique
    Attributes b = new Attributes();
    b.set("s", new StringArray(new String[] {"theString"})); // same
    b.set("number", new IntArray(new int[] {11, 22})); // different
    b.set("inB", new IntArray(new int[] {3, 4, 5})); // unique

    atts = Attributes.makeALikeB(a, b);
    Test.ensureEqual(
        atts.toString(),
        "    inA=\"null\"\n" + "    inB=3i,4i,5i\n" + "    number=11i,22i\n",
        "atts=\n" + atts.toString());
    a.add(atts);
    a.removeValue("\"null\"");
    Test.ensureEqual(a, b, "");
    Test.ensureEqual(a.toString(), b.toString(), "");

    // ***** PrimitiveArray standardizeVariable(int standardizeWhat, String varName,
    // PrimitiveArray dataPa)
    PrimitiveArray pa;

    // standardizeWhat = 1: _Unsigned byte -> ubyte
    atts.clear();
    atts.set("_Unsigned", "true");
    atts.set("data_min", (byte) 0);
    atts.set("missing_value", (byte) 99);
    pa = PrimitiveArray.csvFactory(PAType.BYTE, "-128, -1, 0, 1, 99, 127");
    pa = atts.standardizeVariable(1, "test", pa);
    Test.ensureEqual(pa.elementType(), PAType.UBYTE, "");
    Test.ensureEqual(pa.toString(), "128, 255, 0, 1, 255, 127", "");
    Test.ensureEqual(atts.toString(), "    _FillValue=255ub\n" + "    data_min=0ub\n", "");

    // standardizeWhat = 1: _Unsigned int -> uint
    atts.clear();
    atts.set("_Unsigned", "true");
    atts.set("data_min", 0);
    atts.set("missing_value", 99);
    pa = PrimitiveArray.csvFactory(PAType.INT, "-2000000000, -1, 0, 1, 99, 2000000000");
    pa = atts.standardizeVariable(1, "test", pa);
    Test.ensureEqual(pa.elementType(), PAType.UINT, "");
    Test.ensureEqual(pa.toString(), "2294967296, 4294967295, 0, 1, 4294967295, 2000000000", "");
    Test.ensureEqual(atts.toString(), "    _FillValue=4294967295ui\n" + "    data_min=0ui\n", "");

    // standardizeWhat = 1: scale_factor
    // if scale_factor is defined (e.g., in .nc file), mv's should be too. If not,
    // then all values are valid.
    atts.clear();
    atts.set("data_min", (short) -2);
    atts.set("scale_factor", .01f); // float
    pa = PrimitiveArray.csvFactory(PAType.SHORT, "-2, 0, 300, 32767");
    pa = atts.standardizeVariable(1, "test", pa);
    Test.ensureEqual(pa.toString(), "-0.02, 0.0, 3.0, 327.67", "");
    Test.ensureEqual(
        atts.toString(), "    _FillValue=NaNf\n" + "    data_min=-0.02f\n", ""); // float

    // standardizeWhat = 1: scale_factor
    atts.clear();
    atts.set("data_min", (short) -2);
    atts.set("scale_factor", .01); // double
    atts.set("missing_value", 32767);
    pa = PrimitiveArray.csvFactory(PAType.SHORT, "-2, 0, 300, 32767");
    pa = atts.standardizeVariable(1, "test", pa);
    Test.ensureEqual(pa.toString(), "-0.02, 0.0, 3.0, NaN", "");
    Test.ensureEqual(
        atts.toString(), "    _FillValue=NaNd\n" + "    data_min=-0.02d\n", ""); // double

    // standardizeWhat = 1: add_offset
    atts.clear();
    atts.set("add_offset", 10f);
    atts.set("data_min", (short) -2);
    atts.set("missing_value", 32767);
    pa = PrimitiveArray.csvFactory(PAType.SHORT, "-2, 0, 300, 32767");
    pa = atts.standardizeVariable(1, "test", pa);
    Test.ensureEqual(pa.toString(), "8.0, 10.0, 310.0, NaN", "");
    Test.ensureEqual(atts.toString(), "    _FillValue=NaNf\n" + "    data_min=8.0f\n", "");

    // standardizeWhat = 1: scale_factor, add_offset
    atts.clear();
    atts.set("add_offset", .10f);
    atts.set("data_min", (short) -2);
    atts.set("missing_value", (short) 300);
    atts.set("scale_factor", .01f);
    pa = PrimitiveArray.csvFactory(PAType.SHORT, "-2, 0, 300, 32767");
    pa = atts.standardizeVariable(1, "test", pa);
    Test.ensureEqual(pa.toString(), "0.08, 0.1, NaN, 327.77", "");
    Test.ensureEqual(atts.toString(), "    _FillValue=NaNf\n" + "    data_min=0.08f\n", "");

    // standardizeWhat = 1: missing_value
    atts.clear();
    atts.set("data_min", (short) -2);
    atts.set("missing_value", (short) 300);
    pa = PrimitiveArray.csvFactory(PAType.SHORT, "-2, 0, 300, 32767");
    pa = atts.standardizeVariable(1, "test", pa);
    Test.ensureEqual(pa.toString(), "-2, 0, 32767, 32767", "");
    Test.ensureEqual(
        atts.toString(),
        "    _FillValue=32767s\n"
            + // not missing_value
            "    data_min=-2s\n",
        "");

    // standardizeWhat = 1: _FillValue
    atts.clear();
    atts.set("data_min", (short) -2);
    atts.set("_FillValue", (short) 300);
    pa = PrimitiveArray.csvFactory(PAType.DOUBLE, "-2, 0, 300, 32767");
    pa = atts.standardizeVariable(1, "test", pa);
    Test.ensureEqual(pa.toString(), "-2.0, 0.0, NaN, 32767.0", "");
    Test.ensureEqual(
        atts.toString(),
        "    _FillValue=NaNd\n" + "    data_min=-2s\n",
        ""); // unchanged since data wasn't unpacked

    // standardizeWhat = 1: _FillValue and missing_value
    atts.clear();
    atts.set("_FillValue", 300);
    atts.set("actual_min", (short) -2);
    atts.set("missing_value", 32767);
    pa = PrimitiveArray.csvFactory(PAType.DOUBLE, "-2, 0, 300, 32767");
    pa = atts.standardizeVariable(1, "test", pa);
    Test.ensureEqual(pa.toString(), "-2.0, 0.0, NaN, NaN", "");
    Test.ensureEqual(
        atts.toString(),
        "    _FillValue=NaNd\n" + "    actual_min=-2s\n",
        ""); // unchanged since data wasn't unpacked

    // standardizeWhat = 2: convert defined numeric dateTimes to epochSeconds
    // days since 1900
    atts.clear();
    atts.set("_FillValue", (short) 32767);
    atts.set("actual_min", (short) -2);
    atts.set("units", "days since 1900-1-1");
    Test.ensureEqual(
        atts.toString(),
        "    _FillValue=32767s\n" + "    actual_min=-2s\n" + "    units=days since 1900-1-1\n",
        "");
    pa = PrimitiveArray.csvFactory(PAType.SHORT, "-2, 0, 300, 32767");
    Test.ensureEqual(pa.toString(), "-2, 0, 300, 32767", "");
    Test.ensureEqual(pa.getMaxIsMV(), false, "");
    Test.ensureEqual(atts.getDouble("_FillValue"), 32767.0, "");
    pa = atts.standardizeVariable(1 + 2, "test", pa);
    Test.ensureEqual(pa.toString(), "-2.2091616E9, -2.2089888E9, -2.1830688E9, NaN", "");
    Test.ensureEqual(
        atts.toString(),
        "    _FillValue=NaNd\n"
            + "    actual_min=-2.2091616E9d\n"
            + // related atts are converted
            "    units=seconds since 1970-01-01T00:00:00Z\n",
        "");

    // standardizeWhat = 2: convert defined numeric dateTimes to epochSeconds
    // days since 1900
    atts.clear();
    atts.set("data_min", (short) -2);
    atts.set("missing_value", (short) 300); // different value
    atts.set("units", "days since 1900-1-1");
    pa = PrimitiveArray.csvFactory(PAType.SHORT, "-2, 0, 300, 32767");
    pa = atts.standardizeVariable(1 + 2, "test", pa);
    Test.ensureEqual(pa.toString(), "-2.2091616E9, -2.2089888E9, NaN, NaN", "");
    Test.ensureEqual(
        atts.toString(),
        "    _FillValue=NaNd\n"
            + // always just _FillValue
            "    data_min=-2.2091616E9d\n"
            + // related atts are converted
            "    units=seconds since 1970-01-01T00:00:00Z\n",
        "");

    // standardizeWhat = 4: convert defined String mv to ""
    atts.clear();
    atts.set("_FillValue", "QQQ");
    pa = PrimitiveArray.csvFactory(PAType.STRING, "a, bb, , QQQ, none");
    pa = atts.standardizeVariable(4, "test", pa);
    Test.ensureEqual(pa.toString(), "a, bb, , , none", "");
    Test.ensureEqual(atts.toString(), "", "");

    // standardizeWhat=256: if dataPa is numeric, try to identify an undefined
    // numeric missing_value (-999?, 9999?, 1e37f?)
    atts.clear();
    atts.set("data_min", (short) -2);
    pa = PrimitiveArray.csvFactory(PAType.SHORT, "-2, 0, 300, 9999");
    pa = atts.standardizeVariable(256 + 1, "test", pa);
    Test.ensureEqual(pa.toString(), "-2, 0, 300, 32767", "");
    Test.ensureEqual(
        atts.toString(),
        "    _FillValue=32767s\n" + "    data_min=-2s\n",
        ""); // unchanged since dataPa wasn't packed

    // standardizeWhat=1+256 scale_factor and find 99999
    // if scale_factor is defined (e.g., in .nc file), mv's should be too. If not,
    // then all values are valid.
    atts.clear();
    atts.set("data_min", (short) -2);
    atts.set("scale_factor", 100f); // float
    pa = PrimitiveArray.csvFactory(PAType.INT, "-2, 0, 300, 999");
    pa = atts.standardizeVariable(1 + 256, "test", pa);
    // TODO re-enable this line, different machiens were generating different number of digits in
    // the last value
    // Test.ensureEqual(pa.toString(), "-200.0, 0.0, 30000.0, 2.14748365E11", "");
    Test.ensureEqual(
        atts.toString(), "    _FillValue=NaNf\n" + "    data_min=-200.0f\n", ""); // float

    // standardizeWhat=256: if dataPa is numeric, try to identify an undefined
    // numeric missing_value (-999?, 9999?, 1e37f?)
    atts.clear();
    atts.set("data_min", (short) -2);
    pa = PrimitiveArray.csvFactory(PAType.SHORT, "-2, 0, 300, -99");
    pa = atts.standardizeVariable(256 + 1, "test", pa);
    Test.ensureEqual(pa.toString(), "-2, 0, 300, 32767", "");
    Test.ensureEqual(atts.toString(), "    _FillValue=32767s\n" + "    data_min=-2s\n", "");

    atts.clear();
    atts.set("data_min", -2.0f);
    pa = PrimitiveArray.csvFactory(PAType.FLOAT, "-2, 0, 300, 1.1e37");
    pa = atts.standardizeVariable(256 + 1, "test", pa);
    Test.ensureEqual(pa.toString(), "-2.0, 0.0, 300.0, NaN", "");
    Test.ensureEqual(atts.toString(), "    _FillValue=NaNf\n" + "    data_min=-2.0f\n", "");

    atts.clear();
    atts.set("data_min", (short) -2);
    pa = PrimitiveArray.csvFactory(PAType.SHORT, "-20000, -9999, 0, 20000, 9999");
    pa = atts.standardizeVariable(256, "test", pa);
    Test.ensureEqual(
        pa.toString(), "-20000, -9999, 0, 20000, 9999", ""); // +/-9999 isn't max, so isn't found
    Test.ensureEqual(atts.toString(), "    data_min=-2s\n", "");

    atts.clear();
    atts.set("data_min", (short) -2);
    pa = PrimitiveArray.csvFactory(PAType.SHORT, "-20000, -9999, 0, 20000, 9999");
    pa = atts.standardizeVariable(256 + 1, "test", pa); // 256 + 1
    Test.ensureEqual(
        pa.toString(), "-20000, -9999, 0, 20000, 9999", ""); // +/-9999 isn't max, so isn't found
    Test.ensureEqual(
        atts.toString(),
        "    _FillValue=32767s\n"
            + // standardizeWhat=1 adds this
            "    data_min=-2s\n",
        "");

    // standardizeWhat=512: if dataPa is Strings, convert all common, undefined,
    // missing value strings (e.g., "n/a") to ""
    atts.clear();
    pa = PrimitiveArray.csvFactory(PAType.STRING, "a, bb, , n/a");
    pa = atts.standardizeVariable(512, "test", pa);
    Test.ensureEqual(pa.toString(), "a, bb, , ", "");
    Test.ensureEqual(atts.toString(), "", "");

    // standardizeWhat=1024: if dataPa is Strings, try to convert not-purely-numeric
    // String dateTimes to ISO 8601 String
    atts.clear();
    pa =
        PrimitiveArray.csvFactory(
            PAType.STRING, "\"Jan 2, 1970\", , \"DEC 32, 2000\""); // forgiving
    pa = atts.standardizeVariable(1024, "test", pa);
    Test.ensureEqual(pa.toString(), "1970-01-02, , 2001-01-01", "");
    Test.ensureEqual(atts.toString(), "    units=yyyy-MM-dd\n", "");

    atts.clear();
    pa =
        PrimitiveArray.csvFactory(
            PAType.STRING, "\"Jan 2, 1970 00:00:00\", , \"DEC 32, 2000 01:02:03\""); // forgiving
    pa = atts.standardizeVariable(1024, "test", pa);
    Test.ensureEqual(pa.toString(), "1970-01-02T00:00:00Z, , 2001-01-01T01:02:03Z", "");
    Test.ensureEqual(atts.toString(), "    units=yyyy-MM-dd'T'HH:mm:ssZ\n", "");

    atts.clear();
    pa =
        PrimitiveArray.csvFactory(
            PAType.STRING,
            "\"Jan 2, 1970 00:00:00.0\", , \"DEC 32, 2000 01:02:03.4\""); // forgiving
    pa = atts.standardizeVariable(1024, "test", pa);
    Test.ensureEqual(pa.toString(), "1970-01-02T00:00:00.000Z, , 2001-01-01T01:02:03.400Z", "");
    Test.ensureEqual(atts.toString(), "    units=yyyy-MM-dd'T'HH:mm:ss.SSSZ\n", "");

    atts.clear();
    pa =
        PrimitiveArray.csvFactory(
            PAType.STRING, "\"Jan 2, 1970 00:00:00.0\", , \"DEC 32 , 2000 01:02:03.4\"");
    pa = atts.standardizeVariable(1024, "test", pa); // extra space before comma not matched
    Test.ensureEqual(
        pa.toString(),
        "\"Jan 2, 1970 00:00:00.0\", , \"DEC 32 , 2000 01:02:03.4\"",
        ""); // unchanged
    Test.ensureEqual(atts.toString(), "", "");

    // standardizeWhat=2048: if dataPa is Strings, try to convert purely-numeric
    // String dateTimes to ISO 8601 Strings
    atts.clear();
    pa =
        PrimitiveArray.csvFactory(
            PAType.STRING, "\"19700102\", , \"20001231\""); // not forgiving, so don't try
    pa = atts.standardizeVariable(2048, "test", pa);
    Test.ensureEqual(pa.toString(), "1970-01-02, , 2000-12-31", "");
    Test.ensureEqual(atts.toString(), "    units=yyyy-MM-dd\n", "");

    atts.clear();
    pa =
        PrimitiveArray.csvFactory(
            PAType.STRING, "\"19700102\", , \"20001240\""); // day 40 won't be matched
    pa = atts.standardizeVariable(2048, "test", pa);
    Test.ensureEqual(pa.toString(), "19700102, , 20001240", ""); // unchanged
    Test.ensureEqual(atts.toString(), "", "");

    String2.log("*** Attributes.basicTest finished successfully.");
  }
}
