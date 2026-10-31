package com.ankur.spark.service.copy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.nio.file.Paths;

@Getter
@ToString
@AllArgsConstructor
public class CopyObject {

    private final String objectName;
    private final String fromDirectory;
    private final String toDirectory;

    public String getFromPath() {
        return Paths.get(fromDirectory, objectName).toString();
    }

    public String getToPath() {
        return Paths.get(toDirectory, objectName).toString();
    }
}
