package com.ankur.spark.service.copy;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Getter
@Builder
@ToString
public class CopySurface {

    private String inSurface;
    private String outSurface;
    // table key -> path
    private Map<String, String> grainPaths;
}
