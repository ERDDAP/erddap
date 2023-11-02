package gov.noaa.pfel.erddap.util;

public class RequestDetails {

    public String dateTime;
    public String dataSetId;
    public String ipAddress;
    public String variables;
    public String queryParams;
    public String response;

    public RequestDetails() {};

    public RequestDetails(String dateTime, String dataSetId, String ipAddress, String variables, String queryParams, String response) {
        this.dateTime = dateTime;
        this.dataSetId = dataSetId;
        this.ipAddress = ipAddress;
        this.variables = variables;
        this.queryParams = queryParams;
        this.response = response;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(String dataSetId) {
        this.dataSetId = dataSetId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getVariables() {
        return variables;
    }

    public void setUrl(String variables) {
        this.variables = variables;
    }

    public String getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(String queryParams) {
        this.queryParams = queryParams;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return """
                { "dateTime": "%s", "dataSetId": "%s", "ipAddress": "%s", "variables": "%s", "queryParams": "%s", "response": "%s"}
               """
                .formatted(dateTime, dataSetId, ipAddress, variables, queryParams, response);
    }
}
