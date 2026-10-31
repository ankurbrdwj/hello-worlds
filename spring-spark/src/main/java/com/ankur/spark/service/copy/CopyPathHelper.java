package com.ankur.spark.service.copy;

import java.nio.file.Paths;

/** Resolves where a table key's objects are copied from (GCS) and to (HDFS). */
public class CopyPathHelper {

    public String getGcsSourceDirectory(String tableKey, CopyRequest request, CopySurface surface) {
        return surface.getGrainPaths().get(tableKey);
    }

    /** The source directory with the surface swapped from in to out, rooted under the request's HDFS target path. */
    public String getHdfsTargetDirectory(String tableKey, CopyRequest request, CopySurface surface) {
        String source = getGcsSourceDirectory(tableKey, request, surface);
        String swapped = source.replace(surface.getInSurface(), surface.getOutSurface());
        return request.getHdfsTargetPath() == null ? swapped : Paths.get(request.getHdfsTargetPath(), swapped).toString();
    }

    public CopyObject getCopyObject(String gcsObject, String tableKey, CopyRequest request, CopySurface surface) {
        String objectName = Paths.get(gcsObject).getFileName().toString();
        return new CopyObject(objectName, getGcsSourceDirectory(tableKey, request, surface),
                getHdfsTargetDirectory(tableKey, request, surface));
    }
}
