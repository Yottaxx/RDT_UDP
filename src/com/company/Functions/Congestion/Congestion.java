package com.company.Functions.Congestion;

import com.company.Functions.Transport.Transport;
import com.company.Utils.DataFormat;

import java.util.concurrent.ConcurrentHashMap;

public class Congestion {
    public static int SS_THRESH = 10000;
    public static float  CWND = 512f;
    public static final ConcurrentHashMap<Integer, DataFormat> sendSet = Transport.sendSet;


    public static void getACK(int add){
        assert add!=0;
        if(CWND<SS_THRESH)
        {
            CWND+=add;
        }
        else
        {
            CWND+=(float)add/sendSet.size();
        }
        System.out.println("-------当前拥塞窗口为"+CWND+"------");
    }

   public static void getACK(int add,int ackLen){
        assert add!=0;
        if(CWND<SS_THRESH)
        {
            CWND+=add;
        }
        else
        {
            CWND+=1f/ackLen;
        }
    }

    public static void timeOut()
    {
        SS_THRESH=SS_THRESH/2;
        CWND=SS_THRESH;
        System.out.println("--------出现超时 阈值减半 窗口复位--------");
    }
}
