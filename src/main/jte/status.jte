@import gov.noaa.pfel.erddap.jte.Status
@import gov.noaa.pfel.erddap.jte.YouAreHere
@param YouAreHere youAreHere
@param Status status
@template.youarehere(youAreHere)
<pre>
Current time is ${Status.getCurrentTimeLocal()}
Startup was at  ${Status.getStartupTimeLocal()}
Last major LoadDatasets started ${Status.getTimeSinceLastMajorLoadDatasetsStarted()} ago and@if(Status.isLoadDatasetsRunning()) is still running.@else finshed after ${Status.getLoadDatasetsElapsedTimeSeconds()} seconds.@endif
@if(!Status.isDatasetLoading())Currently, no dataset is loading.@elseCurrently @if(Status.isDatasetLoadingMajor())major@else minor @endif LoadDatasets is loading dataset #${Status.getDatasetLoadingNumber()}=${Status.getDatasetLoadingID()} (${Status.getDatasetLoadingTime()}).@endif
nGridDatasets  = ${status.getGridDatasetCount()}
nTableDatasets = ${status.getTableDatasetCount()}
nTotalDatasets = ${status.getGridDatasetCount()+status.getTableDatasetCount()}
${Status.getDatasetsFailedToLoad()}@if(Status.getShowLoadErrorsOnStatusPage())${Status.getFailedDatasetsWithErrors()}@endif${Status.getErrorsDuringMajorReload()}
Unique users (since startup)                            n = ${Status.getIpAddressQueue()}

Response Failed    Time (since last major LoadDatasets) ${Status.getFailureTimesDistributionLoadDatasets()}
Response Failed    Time (since last Daily Report)       ${Status.getFailureTimesDistribution24()}
Response Failed    Time (since startup)                 ${Status.getFailureTimesDistributionTotal()}
Response Succeeded Time (since last major LoadDatasets) ${Status.getResponseTimesDistributionLoadDatasets()}
Response Succeeded Time (since last Daily Report)       ${Status.getResponseTimesDistribution24()}
Response Succeeded Time (since startup)                 ${Status.getResponseTimesDistributionTotal()}

TaskThread has finished ${Status.getLastFinishedTaskPlusOne()} out of ${Status.getTaskListSize()} tasks.  @if(Status.getTaskThreadStatusMessage()==null)Currently, no task is running.@elseThe current task has been running for ${Status.getTaskThreadStatusMessage()}@endif
TaskThread Failed    Time (since last Daily Report)     ${Status.getTaskThreadFailedDistribution24()}
TaskThread Failed    Time (since startup)               ${Status.getTaskThreadFailedDistributionTotal()}
TaskThread Succeeded Time (since last Daily Report)     ${Status.getTaskThreadSucceededDistribution24()}
TaskThread Succeeded Time (since startup)               ${Status.getTaskThreadSucceededDistributionTotal()}

@if(Status.isEmailSystemActive())
EmailThread has sent ${Status.getLastFinishedEmailPlusOne()} out of ${Status.getEmailListSize()} emails. @if(Status.getEmailThreadStatusMessage()==null)Currently, the thread is sleeping.@elseThe current email session has been running for ${Status.getEmailThreadStatusMessage()}.@endif
EmailThread Failed    Time (since last Daily Report)    ${Status.getEmailThreadFailedDistribution24()}
EmailThread Succeeded Time (since last Daily Report)    ${Status.getEmailThreadSucceededDistribution24()}
@elseThe email system is inactive.@endif
TouchThread has finished ${Status.getLastFinishedTouchPlusOne()} out of ${Status.getTouchListSize()} touches.  @if(Status.getTouchThreadStatusMessage()==null)Currently, the thread is sleeping.@elseThe current touch has been running for ${Status.getTouchThreadStatusMessage()}@endif
TouchThread Failed    Time (since last Daily Report)    ${Status.getTouchThreadFailedDistribution24()}
TouchThread Succeeded Time (since last Daily Report)    ${Status.getTouchThreadSucceededDistribution24()}

OS info: totalCPULoad=${Status.getTotalCpuLoad()} processCPULoad=${Status.getProcessCpuLoad()} totalMemory=${Status.getTotalMemoryMB()}MB freeMemory=${Status.getFreeMemoryMB()}MB totalSwapSpace=${Status.getTotalSwapMB()}MB freeSwapSpace=${Status.getFreeSwapMB()}MB
Number of active requests=${Status.getActiveRequestsCount()}
${Status.getStackTraceHeader()}
${Status.getGcAndRequestShedStatus()}
${Status.getMemoryStatus()}
@if(Status.getLoadDatasetsTimeSeriesSBIsEmpty())
Major LoadDatasets Time Series: MLD    Datasets Loaded               Requests (median times in ms)                Number of Threads      MB    gc   Open
  timestamp                    time   nTry nFail nTotal  nSuccess (median) nFail (median) shed memFail tooMany  tomWait inotify other  inUse Calls Files
----------------------------  -----   -----------------  -----------------------------------------------------  ---------------------  ----- ----- -----
${Status.getLoadDatasetsTimeSeriesSB()}   
@endif 


Major LoadDatasets Times Distribution (since last Daily Report):
${Status.getMajorLoadDatasetsTimeDistribution24()}
Major LoadDatasets Times Distribution (since startup):
${Status.getMajorLoadDatasetsTimeDistributionTotal()}

Minor LoadDatasets Times Distribution (since last Daily Report):
${Status.getMinorLoadDatasetsTimeDistribution24()}
Minor LoadDatasets Times Distribution (since startup):
${Status.getMinorLoadDatasetsTimeDistributionTotal()}

Response Failed Time Distribution (since last major LoadDatasets):
${Status.getFailureTimeDistributionLoadDatasets()}
Response Failed Time Distribution (since last Daily Report):
${Status.getFailureTimeDistribution24()}
Response Failed Time Distribution (since startup):
${Status.getFailureTimeDistributionTotal()}

Response Succeeded Time Distribution (since last major LoadDatasets):
${Status.getResponseTimeDistributionLoadDatasets()}
Response Succeeded Time Distribution (since last Daily Report):
${Status.getResponseTimeDistribution24()}
Response Succeeded Time Distribution (since startup):
${Status.getResponseTimeDistributionTotal()}

EmailThread Failed Time Distribution (since last Daily Report):
${Status.getEmailThreadFailedDistributions24()}
EmailThread Failed Time Distribution (since startup):
${Status.getEmailThreadFailedDistributionsTotal()}
EmailThread Succeeded Time Distribution (since last Daily Report):
${Status.getEmailThreadSucceededDistributions24()}
EmailThread Succeeded Time Distribution (since startup):
${Status.getEmailThreadSucceededDistributionsTotal()}
EmailThread nEmails/Session Distribution (since last Daily Report):
${Status.getEmailThreadNEmailsDistributions24()}
EmailThread nEmails/Session Distribution (since startup):
${Status.getEmailThreadNEmailsDistributionsTotal()}

TaskThread Failed Time Distribution (since last Daily Report):
${Status.getTaskThreadFailedDistributions24()}
TaskThread Failed Time Distribution (since startup):
${Status.getTaskThreadFailedDistributionsTotal()}
TaskThread Succeeded Time Distribution (since last Daily Report):
${Status.getTaskThreadSucceededDistributions24()}
TaskThread Succeeded Time Distribution (since startup):
${Status.getTaskThreadSucceededDistributionsTotal()}

TouchThread Failed Time Distribution (since last Daily Report):
${Status.getTouchThreadFailedDistributions24()}
TouchThread Failed Time Distribution (since startup):
${Status.getTouchThreadFailedDistributionsTotal()}
TouchThread Succeeded Time Distribution (since last Daily Report):
${Status.getTouchThreadSucceededDistributions24()}
TouchThread Succeeded Time Distribution (since startup):
${Status.getTouchThreadSucceededDistributionsTotal()}

${Status.getLanguageTallyDistributions24()}${Status.getLanguageTallyDistributionsTotal()}
${Status.getTopographyStats()}
${Status.getGshhsStats()}
${Status.getNationalBoundariesStats()}
${Status.getStateBoundariesStats()}
${Status.getRiverStats()}
${Status.getImageAccelerationStatus()}
${Status.getCanonicalStats()}

${Status.getTraces()}
</pre>