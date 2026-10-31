package com.ankur.spark;

import org.apache.hadoop.conf.Configuration;

public class HadoopConfigurationFactory {

    public Configuration create(HdfsProperties hdfsProperties) {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsProperties.getUri());
        if (hdfsProperties.getUser() != null) {
            conf.set("hadoop.job.ugi", hdfsProperties.getUser());
        }
        return conf;
    }

    public void cleanup() {
    }
}
