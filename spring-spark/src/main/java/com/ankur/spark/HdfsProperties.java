package com.ankur.spark;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HdfsProperties {

    private String uri = "hdfs://localhost:9000";
    private String user;
}
