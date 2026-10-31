package com.ankur.spark;

import java.util.*;

public interface Profiler extends ProfilerLogger {

    enum Level {
        ERROR, WARN, INFO, DEBUG, TRACE
    }

    enum Component {
        NONE("NONE"),
        ADF_PROCESSOR("adf_processor_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        ADF_PROCESSOR_GCS("adf_processor_gcs_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        ADF_PROCESSOR_ONPREM("adf_processor_onprem_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        BI_CONSUMER("bic_service_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        CALC("[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        CASH_BALANCE_RESOLVER("cash_balance_resolver_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        CPV_CACHE_REDIS("cpv_cache_redis_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        CPV_FX_RESOLVER("fx_resolver_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        CS("calendar_service_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        CTP("calc-tasks-persister_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        DICTIONARY_DERIVER("dictionary_deriver_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        DICTIONARY_LAUNCHER("dictionary_launcher_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        DLDS("dlds-[a-z0-9]{3,5}-dbax-lib-download-service-[0-9a-z]{10}-[0-9a-z]{5}\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        EIO("eio_service_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        EM("event_manager_service_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        EPS("event_persistence_service_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        ES("execution_service_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        FC("fc_server_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        FRTB_CONSUMER("frtb_consumer_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        GDM("grid_director_manager_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        GDM_GCP("grid_director_manager_gcp_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        GDM_ONPREM("grid_director_manager_onprem_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        GM("jm_gridman_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        GTRSC("grid_task_request_service_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        HDFS_RESOLVER("hdfs_resolver_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        INDEX_LAUNCHER("index_launcher_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        JMC("jm_server_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        MERCURY_ADAPTER("mercury_adapter_service_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        PAPI("jm_papi_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        RFDS_DOWNLOADER("rfds_downloader_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        RESULT_PUBLISHER_SERVICE("result_publisher_service_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        STAGING("staging_manager_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        TIME_SERIES_RESOLVER("time_series_resolver_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        UPSTREAM_EXCHANGE_ADAPTER("upstream_exchange_adapter_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        UPSTREAM_EXCHANGE_SERVICE("upstream-exchange-service_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        VC("valuation_context_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        XVA_AGGREGATOR("xva_aggregator_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        XVA_CONSUMER("xva_consumer_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?"),
        CPVC_MANAGER("cpvc_manager_[0-9]+\\.monitoring\\.log(\\.[0-9]{4}-[0-9]{2}-[0-9]{2})?");

        private final String filenamePattern;

        Component(String filenamePattern) {
            this.filenamePattern = filenamePattern;
        }

        public String getFilenamePattern() {
            return filenamePattern;
        }
    }

    Long stop();

    Long stopAndReport(Level level, MonitorableEvent monitorableEvent);

    void report(Level level, MonitorableEvent monitorableEvent);

    void report(Level level, MonitorableEvent monitorableEvent, Long deltaTime);
}
