package gov.noaa.pfel.erddap.util;

import com.cohort.util.String2;

public class UsageMetrics {

    public static void sendUsageMetrics(RequestDetails requestDetails) throws Exception {

        String2.setupLog(false, false,
                "metrics.jsonl", true, 10242);

        String2.log(requestDetails.toString());
    }

}
