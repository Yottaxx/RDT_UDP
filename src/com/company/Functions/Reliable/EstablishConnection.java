package com.company.Functions.Reliable;

import com.company.Functions.Transport.Transport;
import com.company.Utils.DataFormat;
import com.company.Utils.PrimitiveType;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EstablishConnection {
    protected Integer sourcePort;
    protected boolean isConnected=false;
    protected boolean isServer=false;
    protected boolean firstFin=false;
    protected boolean secondFin=false;
    protected boolean replyFirstFin=false;
    protected Integer lastACK;
    protected Integer clientACK=0;

    public boolean isServer() {
        return isServer;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public EstablishConnection(boolean isServer)
    {
        this.isServer = isServer;
        System.out.println("建立连接检测管理isServer "+isServer);
    }

    public boolean getData(DataFormat dataFormat)
    {
        if(isServer)
            return getDataServer(dataFormat);
        else
            return getDataClient(dataFormat);
    }

    public static boolean establishConnectCore(DataFormat receiveDataFormat, DatagramSocket socket, String addressCon, ConcurrentHashMap<Integer, EstablishConnection> connectionHashMap) throws IOException {
        System.out.println("判断是否为建立连接过程"+receiveDataFormat.getSourcePort()+" "+receiveDataFormat.getDestinationPort());

        if(receiveDataFormat.getPrimitiveType().equals(PrimitiveType.getSynType())) {
            System.out.println("同步信号捕获");

            if (connectionHashMap.containsKey(receiveDataFormat.getSourcePort())) {
                EstablishConnection establishConnectionObj = connectionHashMap.get(receiveDataFormat.getSourcePort());
                System.out.println("--------进入连接池判断连接进度-------");
                if(establishConnectionObj.isConnected())
                    return true; //错误连接请求不予回复

                if (establishConnectionObj.getData(receiveDataFormat)) {
                    if (!establishConnectionObj.isConnected()) {
                        Transport.send(establishConnectionObj.getApply(receiveDataFormat),socket,addressCon,receiveDataFormat.getSourcePort());
                        return true;
                    }
                    else {
                        return false;
                    }
                }
                else
                    return true;  //流氓连接请求 不予回复
            } else {
                System.out.println("------加入连接哈希map port"+receiveDataFormat.getSourcePort()+"-----");
                connectionHashMap.put(receiveDataFormat.getSourcePort(), new EstablishConnection(true));
                {
                    EstablishConnection establishConnectionObj = connectionHashMap.get(receiveDataFormat.getSourcePort());
                    if (establishConnectionObj.getData(receiveDataFormat)) {
                        Transport.send(establishConnectionObj.getApply(receiveDataFormat),socket,addressCon,receiveDataFormat.getSourcePort());                    }
                    return true;

                }
            }
        }
        else {
            System.out.println("非同步信号");
            return false;
        }
    }

    public boolean getDataClient(DataFormat dataFormat)
    {
        if(dataFormat.getPrimitiveType().equals(PrimitiveType.getSynType()) && dataFormat.getAcknowledgementNumber().equals(clientACK+1))
        {
            replyFirstFin = true;
            System.out.println("连接成功 client 收到连接第一次"+dataFormat.getAcknowledgementNumber()+"请求回复 "+dataFormat.getSourcePort());
            return true;
        }
        else
            return false;
    }
    public DataFormat getApply(DataFormat dataFormat)
    {
        System.out.println("-----isServer "+isServer+"---"+dataFormat.getSourcePort()+" "+dataFormat.getDestinationPort());
        if(isServer)
           return getApplyServer(dataFormat);
        else
            return getApplyClient(dataFormat);
    }
    public DataFormat getApplyClient(DataFormat dataFormat)
    {
        if(replyFirstFin)
            isConnected = true;
        int des = dataFormat.getSourcePort();
        int source = dataFormat.getDestinationPort();
//        System.out.println("-----before apply "+isServer+"---"+dataFormat.getSourcePort()+" "+dataFormat.getDestinationPort());
        DataFormat dataFormatReply =new DataFormat();
        dataFormatReply.setPrimitiveType(new PrimitiveType(PrimitiveType.getSynType()));
        dataFormatReply.setDestinationPort(des);
        dataFormatReply.setSourcePort(source);
        dataFormatReply.setAcknowledgementNumber(dataFormat.getAcknowledgementNumber());

//        System.out.println("-----Client after apply "+isServer+"---"+dataFormatReply.getSourcePort()+" "+dataFormatReply.getDestinationPort());

        return dataFormatReply;
    }

    public DataFormat getApplyServer(DataFormat dataFormat)
    {
        int des = dataFormat.getSourcePort();
        int source = dataFormat.getDestinationPort();
//        System.out.println("----- Server before apply "+isServer+"---"+dataFormat.getSourcePort()+" "+dataFormat.getDestinationPort());

        DataFormat dataFormatReply =new DataFormat();
        dataFormatReply.setPrimitiveType(new PrimitiveType(PrimitiveType.getSynType()));
        dataFormatReply.setDestinationPort(des);
        dataFormatReply.setSourcePort(source);
        dataFormatReply.setAcknowledgementNumber(dataFormat.getAcknowledgementNumber()+1);

//        System.out.println("-----Server after apply "+isServer+"---"+dataFormatReply.getSourcePort()+" "+dataFormatReply.getDestinationPort());

        return dataFormatReply;
    }

   public boolean getDataServer(DataFormat dataFormat)  //return boolean for apply fin
    {
        if(!firstFin)
        {
            if(dataFormat.getPrimitiveType().equals(PrimitiveType.getSynType()))
            {
                lastACK = dataFormat.getAcknowledgementNumber();
                firstFin = true;
                System.out.println("server 收到连接第一次"+dataFormat.getAcknowledgementNumber()+"请求 "+dataFormat.getSourcePort());
                return true;
            }
            else
                return false;
        }
        else{
            if(dataFormat.getPrimitiveType().equals(PrimitiveType.getSynType()))
            {
                if(dataFormat.getAcknowledgementNumber().equals(lastACK+1))
                {
                    secondFin = true;
                    isConnected =true;
                    System.out.println("server 收到连接第二次"+dataFormat.getAcknowledgementNumber()+"请求 "+dataFormat.getSourcePort());
                    System.out.println("--------------连接"+dataFormat.getSourcePort()+"成功---------------");

                    return true;
                }
                else {
                    System.out.println("server 收到过时的第二次"+dataFormat.getAcknowledgementNumber()+"请求 "+dataFormat.getSourcePort());
                    return false;
                }
            }
            return false;
        }
    }
}
