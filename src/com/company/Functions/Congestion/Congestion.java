package com.company.Functions.Congestion;

import com.company.Functions.Transport.Transport;
import com.company.Utils.DataFormat;

import java.util.concurrent.ConcurrentHashMap;

public class Congestion {
    public static int SS_THRESH = 10000;
    public static float  CWND = 512f;
    public static float MIN_CWND=512f;
    public static final ConcurrentHashMap<Integer, DataFormat> sendSet = Transport.sendSet;
    public static final ConcurrentHashMap<Integer, Integer> answerTime = new ConcurrentHashMap<Integer, Integer>();

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
        SS_THRESH=Math.max(SS_THRESH/2,512);
        CWND=MIN_CWND;
        System.out.println("--------出现超时 阈值减半 窗口重置--------");
    }

    public static void duplicateACK() {
        SS_THRESH=Math.max(SS_THRESH/2,512);
        CWND=SS_THRESH;
        System.out.println("--------出现三个重复ACK 阈值减半 窗口复位--------");
    }

    public static void calculateAnswerTimes(int ack)
    {
        if(ack==-1)
            answerTime.clear();

        if(answerTime.contains(ack))
        {
            answerTime.put(ack,answerTime.get(ack)+1);
            if(answerTime.get(ack).equals(3))
            {
                answerTime.remove(ack);
                duplicateACK();
            }
        }
        else
        {
            answerTime.put(ack,1);
        }
    }
}
