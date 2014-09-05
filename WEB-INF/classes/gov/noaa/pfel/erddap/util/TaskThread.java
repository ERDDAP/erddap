/* 
 * TaskThread Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;


import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;

import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;

import java.util.ArrayList;

/**
 * This does a series of tasks.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-05-20
 */
public class TaskThread extends Thread {

//PLEASE: make parameters from objects that have nice .toString(),
//e.g., StringArray instead of String[].

    /** If taskOA[0].equals(TASK_MAKE_A_DATAFILE), then make
     * taskOA[1]=edd, taskOA[2]=query, taskOA[3]=fileDir, taskOA[4]=fileName, taskOA[5]=fileType 
     */
    public final static Integer TASK_MAKE_A_DATAFILE = new Integer(0);

    /** If taskOA[0].equals(TASK_SET_FLAG), then make taskOA[1]=datasetID 
     */
    public final static Integer TASK_SET_FLAG = new Integer(1);

    /** If taskOA[0].equals(TASK_DAP_TO_NC), then make
     * taskOA[1]=dapUrl, taskOA[2]=StringArray(vars), taskOA[3]=projection, 
     * taskOA[4]=fullFileName, taskOA[5]=jplMode (Boolean.TRUE|FALSE),
     * taskOA[6]=lastModified (Long)
     */
    public final static Integer TASK_DAP_TO_NC = new Integer(2);

    /** If taskOA[0].equals(TASK_ALL_DAP_TO_NC), then make
     * taskOA[1]=dapUrl, taskOA[2]=fullFileName,
     * taskOA[3]=lastModified (Long)
     */
    public final static Integer TASK_ALL_DAP_TO_NC = new Integer(3);

    /**
     * TASK_NAMES parallels the TASK Integers.
     */
    public final static String[] TASK_NAMES = new String[]{
        "MAKE_A_DATAFILE",
        "SET_FLAG",
        "DAP_TO_NC",
        "ALL_DAP_TO_NC"};

    /**
     * Set this to true (by calling verbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 
    public static boolean reallyVerbose = false; 

    //set while running
    private long lastStartTime;


    /**
     * The constructor.
     * TaskThread uses task variables in EDStatic.
     *
     */
    public TaskThread(int tNextTask) {
        EDStatic.nextTask = tNextTask;
        EDStatic.lastFinishedTask = tNextTask - 1;
        setName("TaskThread");
    }

    /** 
     * This returns elapsed time for the current task (or -1 if no task is running).
     */
    public long elapsedTime() {
        return System.currentTimeMillis() - lastStartTime;
    }

    /**
     * This does any pending tasks, then exits.
     */
    public void run() {
        while (EDStatic.nextTask < EDStatic.taskList.size()) {
            String taskSummary = null;
            try {
                //check isInterrupted
                if (isInterrupted()) { 
                    String2.log("%%% TaskThread was interrupted at " + 
                        Calendar2.getCurrentISODateTimeStringLocal());
                    return;
                }

                //start to do the task
                //do these things quickly to keep internal consistency
                lastStartTime = System.currentTimeMillis();  
                String2.log("\n%%% TaskThread started task #" + EDStatic.nextTask + 
                    " of " + (EDStatic.taskList.size() - 1) +
                    " at " + Calendar2.getCurrentISODateTimeStringLocal());
                EDStatic.nextTask++;

                //get the task settings
                Object taskOA[] = (Object[])EDStatic.taskList.get(EDStatic.nextTask - 1);
                if (taskOA == null) {
                    String2.log("task #" + (EDStatic.nextTask - 1) + " was null.");
                    continue;
                }
                Integer taskType = (Integer)taskOA[0];

                //TASK_MAKE_A_DATAFILE
                if (taskType.equals(TASK_MAKE_A_DATAFILE)) {

                    EDD edd         = (EDD)taskOA[1];
                    String query    = (String)taskOA[2];
                    String fileDir  = (String)taskOA[3];
                    String fileName = (String)taskOA[4];
                    String fileType = (String)taskOA[5];
                    taskSummary = 
                        "  TASK_MAKE_A_DATAFILE datasetID=" + edd.datasetID() + "\n" +
                        "    query=" + query + "\n" + 
                        "    file=" + fileDir + fileName + fileType;
                    String2.log(taskSummary);

                    edd.reuseOrMakeFileForDapQuery(null, EDStatic.loggedInAsSuperuser, //request, loggedInAs
                        query, fileDir, fileName, fileType);

                //TASK_SET_FLAG
                } else if (taskType.equals(TASK_SET_FLAG)) {
                    String datasetID = (String)taskOA[1];
                    taskSummary = "  TASK_SET_FLAG datasetID=" + datasetID;
                    String2.log(taskSummary);

                    //It doesn't really matter if lastFinishedTask is set below before construction starts.
                    //All of the files are copied, so all will be detected.
                    EDD.requestReloadASAP(datasetID);

                //TASK_DAP_TO_NC
                } else if (taskType.equals(TASK_DAP_TO_NC)) {

                    String      dapUrl       = (String)taskOA[1];
                    StringArray vars         = (StringArray)taskOA[2];
                    String      projection   = (String)taskOA[3];
                    String      fullFileName = (String)taskOA[4];
                    Boolean     jplMode      = (Boolean)taskOA[5];
                    Long        lastModified = (Long)taskOA[6];
                    taskSummary = 
                        "  TASK_DAP_TO_NC \n" + 
                        "    dapUrl=" + dapUrl +
                        "    vars=" + vars + " projection=" + projection +
                        "    file=" + fullFileName + 
                            " lastMod=" + Calendar2.safeEpochSecondsToIsoStringT( //local time, or Z?
                                lastModified.longValue() / 1000.0, "NaN");
                    String2.log(taskSummary);

                    OpendapHelper.dapToNc(dapUrl, vars.toArray(),
                        projection, fullFileName, jplMode.booleanValue());
                    File2.setLastModified(fullFileName, lastModified.longValue());

                //TASK_ALL_DAP_TO_NC
                } else if (taskType.equals(TASK_ALL_DAP_TO_NC)) {

                    String      dapUrl       = (String)taskOA[1];
                    String      fullFileName = (String)taskOA[2];
                    Long        lastModified = (Long)taskOA[3];
                    taskSummary = 
                        "  TASK_ALL_DAP_TO_NC \n" + 
                        "    dapUrl=" + dapUrl +
                        "    file=" + fullFileName + 
                            " lastMod=" + Calendar2.safeEpochSecondsToIsoStringT( //local time, or Z?
                                lastModified.longValue() / 1000.0, "NaN");
                    String2.log(taskSummary);

                    OpendapHelper.allDapToNc(dapUrl, fullFileName);
                    File2.setLastModified(fullFileName, lastModified.longValue());

                //UNKNOWN taskType
                } else {
                    String2.log("TaskThread error: Unknown taskType=" + taskType + 
                        " for task #" + (EDStatic.nextTask - 1) + ".");
                }

                //task finished successfully
                long tElapsedTime = elapsedTime();
                String2.log("%%% TaskThread task #" + (EDStatic.nextTask - 1) + 
                    " of " + (EDStatic.taskList.size() - 1) +
                    " succeeded.  elapsedTime = " + Calendar2.elapsedTimeString(tElapsedTime));
                String2.distribute(tElapsedTime, EDStatic.taskThreadSucceededDistribution24);
                String2.distribute(tElapsedTime, EDStatic.taskThreadSucceededDistributionTotal);

            } catch (Throwable t) {
                long tElapsedTime = elapsedTime();
                String2.distribute(tElapsedTime, EDStatic.taskThreadFailedDistribution24);
                String2.distribute(tElapsedTime, EDStatic.taskThreadFailedDistributionTotal);
                String subject = "TaskThread error: task #" + (EDStatic.nextTask - 1) + 
                    " failed after " + Calendar2.elapsedTimeString(tElapsedTime);
                String content = "" + taskSummary + "\n" +
                    MustBe.throwableToString(t);
                String2.log("%%% " + subject + "\n" + content);
                EDStatic.email(EDStatic.emailEverythingToCsv, subject, content);
            }

            //whether succeeded or failed
            synchronized(EDStatic.taskList) { //all task-related things synch on taskList
                EDStatic.lastFinishedTask = EDStatic.nextTask - 1;
                EDStatic.taskList.set(EDStatic.nextTask - 1, null);  //throw away the task info (gc)
            }
        }
    }

}
