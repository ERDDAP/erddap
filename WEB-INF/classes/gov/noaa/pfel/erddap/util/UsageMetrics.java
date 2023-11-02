package gov.noaa.pfel.erddap.util;

import com.cohort.util.String2;

public class UsageMetrics {

    public UsageMetrics() throws Exception {

        String2.setupLog(false, false,
                "metrics.jsonl", true, 10242);
    }
    public void sendUsageMetrics(RequestDetails requestDetails) throws Exception {

        String2.log(requestDetails.toString());
    }
    public static void extractDataSetId(String dataSetId) {
        // TODO: Include REGEX to extract the dataset id out of the requestUrl
    }


}
