package com.ankur.spark.service.copy;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
public class CopyRequest {

    private String vcsExecutionId;
    private String grain;
    private String hdfsTargetPath;
    private List<CopySurface> surfaces;
}
