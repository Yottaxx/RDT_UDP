package com.company.Functions;

public interface CongestionFunction {
    public static Integer SS_THRESH=10000;
    public static final Integer MIN_CON_WINDOW=512;
    public static final Integer MAX_CON_WINDOW=51200;
    public void slowStartThreshold();
    public void RTO();
    public void ackReceiveRepeat();
}
