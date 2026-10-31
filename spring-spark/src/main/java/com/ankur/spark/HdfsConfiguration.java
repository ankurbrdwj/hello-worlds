package com.ankur.spark;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FileSystem;

@Slf4j
@Getter
public class HdfsConfiguration {

    private final HadoopConfigurationFactory hadoopConfigurationFactory;

    private final FileSystem hdfsFileSystem;

    @Setter
    private String hdfsTargetPath;

    public HdfsConfiguration(HadoopConfigurationFactory hadoopConfigurationFactory, HdfsProperties hdfsProperties, String hdfsTargetPath) {
        this.hadoopConfigurationFactory = hadoopConfigurationFactory;
        this.hdfsFileSystem = hadoopFileSystem(hdfsProperties);
        this.hdfsTargetPath = hdfsTargetPath;
    }

    @SneakyThrows
    private FileSystem hadoopFileSystem(HdfsProperties hdfsProperties) {
        log.info("Creating connection to HDFS file system");
        org.apache.hadoop.conf.Configuration conf = hadoopConfigurationFactory.create(hdfsProperties);
        return FileSystem.get(conf);
    }

    public void cleanup() {
        try {
            hdfsFileSystem.close();
        } catch (Exception e) {
            log.warn("Error closing HDFS file system.", e);
        }
        hadoopConfigurationFactory.cleanup();
    }
}
