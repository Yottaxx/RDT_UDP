package com.company.Utils;

public class PrimitiveType{
    public static final  Integer MAX_ACK =102400;
    public static final  Integer MAX_SEQ =102400;
    public final static int ACK = 0;
    public static final int SYN =1;
    public static final int FIN= 2;
    public final int type;

    public PrimitiveType(int type)
    {
        this.type= type;
    }

    public static int  getAckType()
    {
        return ACK;
    }

    public static int  getSynType()
    {
        return SYN;
    }

    public  static  int getFinType()
    {
        return FIN;
    }
}