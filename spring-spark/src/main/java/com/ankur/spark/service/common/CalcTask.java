package com.ankur.spark.service.common;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
@Getter
@Setter
public class CalcTask implements Serializable {

    static final String NEEDS_LOCALLY_CACHED_OBJECTS = "cache.on.file.system";

    private static final String TABLE_FILTER = "TABLE FILTER";
    private static final String RESULT_HANDLE_FILTER = "RESULT HANDLE FILTER";
    private static final long serialVersionUID = 43;
    private static final String TABLE_DELIMITER = ".";
    private static final String VALUE_DELIMITER = ",";

    private final UID uid;

    private String baseLogDirectory;
    private String logLevel;
    private String logSubDirectory;
    private String calcLogFileName;

    // Original dictionary reference from dictionary notification or FORP message
    private Datauri dictionaryReference;

    @Setter(AccessLevel.NONE)
    private List<Datauri> originalDictionaryReferences;

    private final Map<String, String> messagingParameters;

    @Setter(AccessLevel.NONE)
    private final List<CalcProcessorParams> processorParams;

    // What sort of data is to be parsed
    private final String calcType;

    // Dbax version to use
    private DbaxVersionInfo dbaxVersion;
    private DbaxVersionInfo altDbaxVersion;

    private boolean isDebugBuildType;

    // Dbax version match algorithm
    private DbaxMatchType dbaxMatchType = DbaxMatchType.EXACT;

    // If our first choice is not available
    private DbaxVersionInfo defaultDbaxVersion;

    // Processing/run context for which this task is a part. Used to enrich messaging data to client applications
    private TaskContext taskContext;

    // Cluster URI to support both hbase and GFS as source of index data
    private String[] connectorString;

    private String tradeSource;

    // Result publisher mode (i.e. file, solace or both)
    private String resultPublisherMode;

    // Publish the exposure dictionary (allows the functionality to be switched off)
    private boolean publishExposureDictionary;

    // Overlap report to be captured, false by default
    private boolean overlapReportEnabled;

    // Use the keystore cache (allows the functionality to be switched off for tests)
    private boolean useKeyStore = true;

    // Enrichment toggle for CCR MKRS-168910
    private boolean enrichRFGMsgWithCoreTradeAndCRMData;

    // Uri for connecting to CPV cache/keystore
    private String cpvCacheBaseUrl;
    private Integer cpvCacheAvailableTimeOutInSeconds;
    private String cpvCacheEnvironment;

    // Check that results were generated for every trade in the input dictionary, or return an error message.
    // Not applicable to NLIRC IndexSwaptions, where a scenario means the dbax results do not match the dictionary.
    private boolean applyTradeCompletenessCheck = true;

    // Use generic XML parser with supplied argument
    private boolean useGenericParser;
    private boolean useGenericDeprecatedParser;

    // Arguments for XML parser
    private final Map<String, String> parserArguments;

    // Custom headers for downstream consumption
    private final Map<String, String> customHeaders;

    private final Map<String, String> calcStatMonitoring = new HashMap<>();
    private final Map<String, String> extraCalcStatMonitoring = new HashMap<>();

    // Trade ids in the job
    private Map<String, String> tradeIds;

    private Map<String, TradeAttributes> tradeIdToTradeAttributes = new HashMap<>();

    private Properties profilerProperties;
    private Properties optionalProperties;

    private boolean readMarketDataFromCache;
    private boolean tradeEventStatusPublishEnabled;

    private RetryParans marketDataReadRetryParams;

    private boolean objectCheckRetry;
    private boolean disabledNotionalScalingValuesTransformation;
    private boolean useMarketDataCacheForLargeObjects;
    private String pathToMarketData;
    private boolean cacheLargeObjectsOnDisk;
    private boolean splitLargeResultMessages;
    private boolean skipJaiUpdate;
    private Integer priority;
    private Long autoKillTimeInMilliseconds;

    private String calcTaskUID;
    private String calcTaskGroupUID;
    private String marketDataVersion;
    private boolean reportCalcStatistic;
    private long nearCacheFreeSpaceLimit;
    private int nearCacheVersionCountLimit;
    private int sensiBucketCount;
    private boolean saveModifiedThirdDictionary;
    private boolean scoreSecurityNameOverride;
    private String gridType;
    private String brokerChannelTask;
    private boolean dbaFieldsToggled;
    private String cpvResultContextMetaDataCols;
    private boolean skipPricing;

    private String transposeKey;
    private String extraReportHeaderCols;
    private boolean transposeResult;
    private boolean extraReportEnabled;
    private boolean extraResultsEnabled;
    private String extraReportKeyExclFilter;

    private boolean calcStatusRfgEnabled;
    private boolean splitRfgBySecurity;
    private boolean extraTradeEnvelopeHash;
    private boolean applyReportUnit;
    private String calcSubType;
    private String runSubType;
    private String runCategory;
    private String runQualifier;
    private boolean applySecurityNameCorrectness;
    private boolean useSolacePriority;

    private Map<String, Boolean> toggleMap = new HashMap<>();

    private String timeseriesName;
    private boolean gatherCalculationMetadata;
    private boolean percentageErrorsEnabled;

    private String optimizedTransposeKey;
    private String optimizedExtraReportHeaderCols;
    private boolean optimizedTransposeResult;
    private String optimizedExtraReportKeyExclFilter;

    private int numberOfTenors;
    private String grain;
    private int securityCount;
    private String taskType;
    private String flowEventId;
    private String flowName;
    private String stepType;

    private String calculationLocation;
    private Map<String, String> functionProperties;
    private String brokerLocation;
    private String brokerType;
    private String secondaryCategory;
    private Long dictionaryCreatedTs;
    private Long estimatedCalcTimeMs;
    private List<CalcTask> calcTaskGroup;
    private boolean relaunch;
    private Long resultBytes;

    // For serialization only
    private CalcTask() {
        this(null, null, null);
    }

    public CalcTask(UID uid, String calcType) {
        this(uid, null, calcType);
    }

    public CalcTask(UID uid, final Datauri dictionaryReference, String calcType) {
        this.uid = uid;
        this.dictionaryReference = dictionaryReference;
        this.messagingParameters = new HashMap<>();
        this.processorParams = new LinkedList<>();
        this.calcType = calcType;
        this.connectorString = new String[0];
        this.parserArguments = new HashMap<>();
        this.customHeaders = new LinkedHashMap<>();
    }

    public List<Datauri> getOriginalDictionaryReferences() {
        return originalDictionaryReferences != null ? originalDictionaryReferences : Collections.emptyList();
    }

    public void setCustomHeaders(Map<String, String> customHeaders) {
        this.customHeaders.putAll(customHeaders);
    }

    public Map<String, Datauri> getIdcReferences() {
        Map<String, Datauri> idcReferences = new HashMap<>();
        if (originalDictionaryReferences != null) {
            int index = 0;
            for (Datauri originalDictionaryReference : originalDictionaryReferences) {
                idcReferences.put(CalcReturn.PV_DICTIONARY_KEY + CalcReturn.SEPARATOR + index++, originalDictionaryReference);
            }
        }
        return idcReferences;
    }

    public void addOriginalDictionaryReference(Datauri originalDictionaryReference) {
        if (originalDictionaryReferences == null) {
            originalDictionaryReferences = new LinkedList<>();
        }
        originalDictionaryReferences.add(originalDictionaryReference);
    }

    public void setMessagingParameter(String key, String value) {
        messagingParameters.put(key, value);
    }

    public void setMessagingParameters(Map<String, String> taskParameters) {
        messagingParameters.putAll(taskParameters);
    }

    public void addCalcProcessorParam(CalcProcessorParams param) {
        processorParams.add(param);
    }

    public List<CalcProcessorParams> getCalcProcessorParams() {
        return processorParams;
    }

    public int getNumDeals() {
        if (tradeIds == null || tradeIds.isEmpty()) {
            return 0;
        }
        return tradeIds.size();
    }

    public List<String> getExtraTables() {
        return getFilterArguments(TABLE_FILTER);
    }

    public List<String> getExtraResultHandles() {
        return getFilterArguments(RESULT_HANDLE_FILTER);
    }

    private List<String> getFilterArguments(String filter) {
        List<String> tables = new ArrayList<>();
        for (Map.Entry<String, String> arg : getParserArguments().entrySet()) {
            if (arg.getKey().startsWith(filter)) {
                tables.addAll(Arrays.asList(arg.getValue().split(TABLE_DELIMITER)));
            }
        }
        return tables;
    }

    public boolean needsCacheOnFileSystemFor(String objectType) {
        if (this.optionalProperties == null || optionalProperties.isEmpty()) {
            return false;
        }
        String locallyCachedObjectTypesAsText = optionalProperties.getProperty(NEEDS_LOCALLY_CACHED_OBJECTS, "");
        if (locallyCachedObjectTypesAsText.isBlank()) {
            return false;
        }
        List<String> locallyCachedObjectTypes = parseLocallyCachedObjectTypes(locallyCachedObjectTypesAsText);
        String fullyQualifiedObjectType = getFullyQualifiedObjectType(objectType);
        return locallyCachedObjectTypes.contains(fullyQualifiedObjectType);
    }

    public String getBusinessProcess() {
        return getTaskContext() != null ? getTaskContext().getBusinessProcess() : "N/A";
    }

    private List<String> parseLocallyCachedObjectTypes(String locallyCachedObjectTypesAsText) {
        return trimValues(Arrays.asList(locallyCachedObjectTypesAsText.split(VALUE_DELIMITER)));
    }

    private List<String> trimValues(List<String> originalValues) {
        return originalValues.stream().map(String::trim).collect(Collectors.toList());
    }

    private String getFullyQualifiedObjectType(String objectType) {
        return String.format("%s:%s", getBusinessProcess(), objectType);
    }

    public String getDbaxVersionStr() {
        return Optional.ofNullable(dbaxVersion).map(DbaxVersionParser::format).orElse(null);
    }

    public boolean isGrouped() {
        return calcTaskGroup != null && !calcTaskGroup.isEmpty();
    }
}
