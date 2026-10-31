package com.ankur.spark.service.common;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/** Reference to a dictionary (data URI). */
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class Datauri implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String uri;
}
