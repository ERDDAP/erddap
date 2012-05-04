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

    /** "ERROR" is defined here (from String2.ERROR) so that it is consistent in log files. */
    public final static String ERROR = String2.ERROR;

    /** If taskOA[0].equals(TASK_MAKE_A_DATAFILE), then make
     * taskOA[1]=edd, taskOA[2]=query, taskOA[3]=fileDir, taskOA[4]=fileName, taskOA[5]=fileType 
     */
    public final static Integer TASK_MAKE_A_DATAFILE = new Integer(0);

    /** If taskOA[1].equals(TASK_SET_FLAG), then make taskOA[1]=datasetID 
     */
    public final static Integer TASK_SET_FLAG = new Integer(1);

    /**
     * TASK_NAMES parallels the TASK Integers.
     */
    public final static String[] TASK_NAMES = new String[]{
        "MAKE_A_DATAFILE",
        "SET_FLAG"};

    /**
     * Set this to true (by calling verbose=true in your program, 
     * not but changing the code here)
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

                } else if (taskType.equals(TASK_SET_FLAG)) {
                    String datasetID = (String)taskOA[1];
                    taskSummary = "  TASK_SET_FLAG datasetID=" + datasetID;
                    String2.log(taskSummary);

                    //It doesn't really matter if lastFinishedTask is set below before construction starts.
                    //All of the files are copied, so all will be detected.
                    EDD.requestReloadASAP(datasetID);

                } else {
                    //unknown taskType
                    String2.log("TaskThread error: Unknown taskType=" + taskType + 
                        " for task #" + (EDStatic.nextTask - 1) + ".");
                    continue;
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
                EDStatic.email(EDStatic.emailEverythingTo, subject, content);
            }

            //whether succeeded or failed
            EDStatic.lastFinishedTask = EDStatic.nextTask - 1;
            EDStatic.taskList.set(EDStatic.nextTask - 1, null);  //throw away the task info (gc)
        }
    }

}
