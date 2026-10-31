package com.ankur.spark.service.common;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Processing/run context a {@link CalcTask} belongs to. Used to enrich messaging data.
 */
@Getter
@Setter
public class TaskContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private String businessProcess;
}
