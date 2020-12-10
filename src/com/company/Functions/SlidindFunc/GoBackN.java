package com.company.Functions.SlidindFunc;

import com.company.Functions.Reliable.EstablishConnection;
import com.company.Functions.Reliable.FinishConnection;
import com.company.Functions.Transport.Transport;
import com.company.Utils.DataFormat;
import com.company.Utils.PrimitiveType;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GoBackN {
    private boolean isServer = false;
    private List<Integer> dataWindow=new ArrayList<>();
    private Integer pointerSendBegin=0;
    private Integer pointerSendEnd=0;
    private Integer sendWindowSize=1;
    public static final Integer MAX_SEQUENCE_NUM =102400;
    private String addressCon;
    private Integer connectionPort;
    private  Integer serverWindows =200;
    private DataFormat dataFormat;
    private boolean newData = false;
    public final DatagramSocket socket;
    public DataFormat getDataFormat() {
        return dataFormat;
    }

    public void setDataFormat(DataFormat dataFormat) {
        this.dataFormat = dataFormat;
        this.newData = true;
    }

    public static final Integer TIME_WAIT=2000;
    GoBackN(DatagramSocket socket)
    {

        this.socket = socket;
    }

    public boolean isServer() {
        return isServer;
    }

    public void setServer(boolean server) {
        isServer = server;
    }

    public GoBackN(boolean isServer, String addressCon, Integer connectionPort, DatagramSocket socket)
    {
        System.out.println("--------GoBackN------"+addressCon);
        this.isServer = isServer;
        this.addressCon=addressCon;
        this.connectionPort = connectionPort;
        this.socket = socket;
    }


    public GoBackN(String addressCon, Integer connectionPort, DatagramSocket socket)
    {
        this.addressCon=addressCon;
        this.connectionPort = connectionPort;
        this.socket = socket;
    }
    public String getAddressCon() {
        return addressCon;
    }

    public void setAddressCon(String addressCon) {
        this.addressCon = addressCon;
    }

    public Integer getConnectionPort() {
        return connectionPort;
    }

    public void setConnectionPort(Integer connectionPort) {
        this.connectionPort = connectionPort;
    }
    public static boolean goBackNCore(DataFormat dataFormat, DatagramSocket socket,
                                           String addressCon, HashMap<Integer, EstablishConnection> connectionHashMap,
                                           HashMap<Integer, GoBackN> goBackNHashMap) throws IOException {
        int sourcePort = dataFormat.getSourcePort();
        if(dataFormat.getPrimitiveType().equals(PrimitiveType.getAckType())) {
            if (connectionHashMap.containsKey(sourcePort)) {
                if (connectionHashMap.get(sourcePort).isConnected()) {
                    if (!goBackNHashMap.containsKey(sourcePort)) {
                        goBackNHashMap.put(sourcePort, new GoBackN(true, addressCon, sourcePort, socket));
                    }
                    goBackNHashMap.get(sourcePort).getACK(dataFormat, socket);
                }
            } else {
                System.out.println("-----非连接端口请求-------");
            }
            return true;
        }
        else
            return  false;

    }


    //TODO:TIME
//    public void send(DatagramSocket socket, String addressCon, Integer connectionPort,Integer sequenceNum,byte[] data)
//    {
//
//    }
    public boolean getACK(DataFormat dataFormat, DatagramSocket socket) throws IOException {
        if(isServer)
            return getACKServer(dataFormat,socket);
        else
            return getACKClient(dataFormat,socket);
    }
    public void beginSend(DataFormat dataFormat, DatagramSocket socket) throws IOException {
        System.out.println("-----goBackN----发送窗口1开始-----");
        Transport.send(socket, addressCon, connectionPort, 0, new byte[1]);
    }

    public boolean getACKClient(DataFormat dataFormat, DatagramSocket socket) throws IOException {
        System.out.println("-------------Go Back N Client------------");
        if(!dataFormat.getPrimitiveType().equals(PrimitiveType.getAckType())) {
            System.out.println("Go Back N 捕获到非ACK消息");
            return false;
        }
        int ackNum = dataFormat.getAcknowledgementNumber();
        if(ackNum>pointerSendBegin) {
            ackNum = ackNum%MAX_SEQUENCE_NUM-1;
            pointerSendBegin = ackNum;
            pointerSendEnd =-1;
        }
        sendWindowSize = dataFormat.getWindow();
        if((pointerSendBegin+sendWindowSize-1-pointerSendEnd)>0)
        {
            if((pointerSendBegin+sendWindowSize-1)>=MAX_SEQUENCE_NUM) {
                Transport.send(socket, addressCon, connectionPort, ackNum, new byte[MAX_SEQUENCE_NUM- pointerSendEnd]);
                pointerSendEnd =MAX_SEQUENCE_NUM;
            }
            else
            {
                Transport.send(socket, addressCon, connectionPort, ackNum, new byte[Math.min(pointerSendBegin + sendWindowSize - 1 - pointerSendEnd,1)]);
                pointerSendEnd = pointerSendBegin+sendWindowSize-1;
            }
            return true;
        }
        else if(ackNum<pointerSendEnd)
        {
            System.out.println("--------回退N到 "+pointerSendBegin+ " byte "+(pointerSendEnd-pointerSendBegin+1));
            Transport.send(socket, addressCon, connectionPort, ackNum, new byte[pointerSendEnd-pointerSendBegin+1]);

        }

        return false;//不需要回复

    }


    public boolean getACKServer(DataFormat dataFormat, DatagramSocket socket) throws IOException {
        System.out.println("-------------Go Back N Server------------");
        System.out.println(dataFormat);
        if(!dataFormat.getPrimitiveType().equals(PrimitiveType.getAckType())) {
            System.out.println("Go Back N 捕获到非ACK消息");
            return false;
        }
        int ackNum = dataFormat.getSequenceNumber()+1;
        if(ackNum>pointerSendBegin) {
           if((ackNum-dataFormat.getBuf().length) == pointerSendBegin)
           {
               Transport.send(socket,addressCon,connectionPort,ackNum,new byte[1],serverWindows,ackNum);

           }
           else
           {
               System.out.println(ackNum-dataFormat.getBuf().length);
               System.out.println("序列号分别为"+dataFormat.getSequenceNumber()+" "+pointerSendBegin);
               Transport.send(socket,addressCon,connectionPort,ackNum,new byte[1],serverWindows,pointerSendBegin);

           }
           return true;
        }
        return false;//不需要回复

    }
}
