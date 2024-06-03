package gov.noaa.pfel.coastwatch.pointdata;

import java.util.BitSet;

import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;

class PointIndexTests {

  /**
     * A main method -- used to test the methods in this class.
     *
     * @param args is ignored  (use null)
     * @throws Exception if trouble
     */
    @org.junit.jupiter.api.Test
    void basicTest() throws Exception {
        PointIndex.verbose = true;

        //testLittleMethods
        

        //setup for tests of one trajectory
        DoubleArray t = new DoubleArray(new double[]{1e8,   2e8, 3e8, 4e8, 5e8, 6e8});
        FloatArray  x = new FloatArray( new float[]{ -180, -120,   0, 120, 120, 180});
        FloatArray  y = new FloatArray( new float[]{  -20,  -45,   0,  45,  45,  90});
        FloatArray  z = new FloatArray( new float[]{    0,    0,   0,   0,   5,   0});
        String dir = File2.getSystemTempDirectory();
        String name = "PointIndexTest1";
        PointIndex index = new PointIndex(dir + name + ".index", "T", t);
        index.addIndex("X", x);
        index.addIndex("Y", y);
        index.addIndex("Z", z);

        //get all
        BitSet results = index.subset(
            new double[]{1e8, -180, -90, 0},
            new double[]{6e8,  180,  90, 5});
        Test.ensureEqual(String2.toString(results), "0, 1, 2, 3, 4, 5", "");

        //2 x's
        results = index.subset(
            new double[]{-1,   120, -90, 0},
            new double[]{1e10, 120,  90, 5});
        Test.ensureEqual(String2.toString(results), "0, 1, 2, 3, 4, 5", ""); //accepts entire station; should be "3, 4", "");

        //1 y
        results = index.subset(
            new double[]{  0, -180, -50, 0},
            new double[]{1e10, 180, -40, 5});
        Test.ensureEqual(String2.toString(results), "0, 1, 2, 3, 4, 5", ""); //accepts entire station; should be "1", "");

        //1 z
        results = index.subset(
            new double[]{  0, -180, -90, 4},
            new double[]{1e10, 180,  90, 7});
        Test.ensureEqual(String2.toString(results), "0, 1, 2, 3, 4, 5", ""); //accepts entire station; should be "4", "");

        //1 t
        results = index.subset(
            new double[]{  0, -180, -90, 0},
            new double[]{1.5e8,180,  90, 5});
        Test.ensureEqual(String2.toString(results), "0", "");

        //nothing (time)
        results = index.subset(
            new double[]{0, -180, -90, 0},
            new double[]{1,  180,  90, 5});
        Test.ensureEqual(String2.toString(results), "", "");

        index.close();


        //***test no variation
        //setup for tests
        t = new DoubleArray(new double[]{4,4});
        x = new FloatArray( new float[]{ 1,1});
        y = new FloatArray( new float[]{ 2,2});
        z = new FloatArray( new float[]{ 3,3});
        name = "PointIndexTest2";
        index = new PointIndex(dir + name + ".index", "T", t);
        index.addIndex("X", x);
        index.addIndex("Y", y);
        index.addIndex("Z", z);

        //get all
        results = index.subset(
            new double[]{ 0,  0,  0,  0},
            new double[]{ 5,  5,  5,  5});
        Test.ensureEqual(String2.toString(results), "0, 1", "");

        //reject based on t, x, y, z
        results = index.subset(
            new double[]{ 5,  0,  0,  0},
            new double[]{10,  5,  5,  5});
        Test.ensureEqual(String2.toString(results), "", "");
        results = index.subset(
            new double[]{ 0, -1,  0,  0},
            new double[]{ 5,  0,  5,  5});
        Test.ensureEqual(String2.toString(results), "", "");
        results = index.subset(
            new double[]{ 0,  0,  5,  0},
            new double[]{ 5,  5, 10,  5});
        Test.ensureEqual(String2.toString(results), "", "");
        results = index.subset(
            new double[]{ 0,  0,  0, -1},
            new double[]{ 5,  5,  5,  0});
        Test.ensureEqual(String2.toString(results), "", "");

        index.close();

        //*** test lots of data
        results = null;
        index = null;
        Math2.incgc("PointIndex (between tests)", 500); //in a test
        Math2.incgc("PointIndex (between tests)", 500); //in a test
        long memoryInUse = Math2.getMemoryInUse();
        int nGroups = 100;
        int nPerGroup = 10000;
        int n = nGroups * nPerGroup;
        String2.log("create PointIndex " + n + ";  Time should be < 2 s for main index"); 
        name = "PointIndexTest3";
        IntArray x2 = new IntArray(n, false);
        IntArray y2 = new IntArray(n, false);
        IntArray z2 = new IntArray(n, false);
        IntArray t2 = new IntArray(n, false);
        for (int i = 0; i < nGroups; i++) {       //1000x 0..999
            int i2 = i*2;
            int i4 = i*4;
            for (int j = 0; j < nPerGroup; j++) {
                t2.add(j);
                x2.add(i);
                y2.add(i2);
                z2.add(i4);
            }
        }
        String2.log("addIndex 1,000,000."); 
        index = new PointIndex(dir + name + ".index", "T", t2);
        //t is                   //0,1,2, ... 0,1,2, ... 0,1,2
        index.addIndex("X", x2); //0,0,0, ... 1,1,1, ... 2,2,2
        index.addIndex("Y", y2); //0,0,0, ... 2,2,2, ... 4,4,4
        index.addIndex("Z", z2); //0,0,0, ... 4,4,4, ... 8,8,8

        //find some
        String2.log("subset times should be <20 ms"); 
        results = index.subset(
            new double[]{ 1, 1,  2,  4},
            new double[]{ 1, 1,  2,  4});
        Test.ensureEqual(String2.toString(results), "10001", "1 time, 1 station");  //row#
        results = index.subset(
            new double[]{ 50, 0, 0, 0},
            new double[]{ 50, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE});
        Test.ensureEqual(results.cardinality(), nGroups, "one time, all stations");  //row#
        int next = -1;
        Test.ensureEqual(next = results.nextSetBit(next + 1),    50, ""); 
        Test.ensureEqual(next = results.nextSetBit(next + 1), 10050, ""); 
        Test.ensureEqual(next = results.nextSetBit(next + 1), 20050, ""); 
        Test.ensureEqual(next = results.nextSetBit(next + 1), 30050, ""); 
        results = index.subset(
            new double[]{ 0, 2, 4, 8},
            new double[]{ Integer.MAX_VALUE, 2, 4, 8});
        Test.ensureEqual(results.cardinality(), nPerGroup, "all times, one station");  //row#
        next = -1;
        Test.ensureEqual(next = results.nextSetBit(next + 1), 20000, ""); 
        Test.ensureEqual(next = results.nextSetBit(next + 1), 20001, ""); 
        Test.ensureEqual(next = results.nextSetBit(next + 1), 20002, ""); 
        Test.ensureEqual(next = results.nextSetBit(next + 1), 20003, ""); 
        Test.ensureEqual(results.get(19999), false, ""); 
        Test.ensureEqual(results.get(30000), false, ""); 

        //reject based on x, y, z, t
        results = index.subset(
            new double[]{ 1.1,  2,  4,  8},
            new double[]{ 1.1,  2,  4,  8});
        Test.ensureEqual(String2.toString(results), "", "");
        results = index.subset(
            new double[]{ 1,  2.1,  4,  8},
            new double[]{ 1,  2.1,  4,  8});
        Test.ensureEqual(String2.toString(results), "", "");
        results = index.subset(
            new double[]{ 1,  2,  4.1,  8},
            new double[]{ 1,  2,  4.1,  8});
        Test.ensureEqual(String2.toString(results), "", "");
        results = index.subset(
            new double[]{ 1,  2,  4,  8.1},
            new double[]{ 1,  2,  4,  8.1});
        Test.ensureEqual(String2.toString(results), "", "");

        index.close();

        //done
        x2=null;
        y2=null;
        z2=null;
        t2=null;
        results = null;
        index = null;
        Math2.incgc("PointIndex (between tests)", 500); //in a test
        Math2.incgc("PointIndex (between tests)", 500); //in a test
        memoryInUse = Math2.getMemoryInUse() - memoryInUse;
        String2.log("\n***** PointIndex.main finished successfully; memory not freed = " + memoryInUse);

    }
  
}
