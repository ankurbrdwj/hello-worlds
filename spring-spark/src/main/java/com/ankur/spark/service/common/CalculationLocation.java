package com.ankur.spark.service.common;

import java.util.*;

public enum CalculationLocation {
    GCP,
    OP;

    public boolean isOnPrem(){return OP==this;}
    public boolean isGCP(){return GCP==this;}

}
