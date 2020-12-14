package com.company.Functions.Reliable;

import com.company.Functions.Transport.Transport;
import com.company.Utils.DataFormat;
import com.company.Utils.PrimitiveType;

import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class FinishConnection {
    protected Integer sourcePort;

    protected boolean isServer=false;
    protected boolean firstFin=false;
    protected boolean secondFin=false;
    protected boolean replyFirstFin=false;
    protected boolean replySecondFin=false;
    protected boolean isFin = false;
    protected Integer lastACK;
    protected Integer clientACK=0;

    public boolean isFin() {
        return isFin;
    }

    public FinishConnection(boolean isServer)
    {
        this.isServer = isServer;
        System.out.println("建立释放检测管理isServer "+isServer);
    }


    public static boolean finishConnection(DataFormat receiveDataFormat, DatagramSocket socket,
                                           String addressCon, ConcurrentHashMap<Integer, EstablishConnection> connectionHashMap,
                                           ConcurrentHashMap<Integer, FinishConnection> finishConnectionHashMap) throws IOException {

        int sourcePort = receiveDataFormat.getSourcePort();
        System.out.println("判断是否为建立释放过程"+receiveDataFormat.getSourcePort()+" "+receiveDataFormat.getDestinationPort());
        if(receiveDataFormat.getPrimitiveType().equals(PrimitiveType.getFinType())) {
            System.out.println("释放信号捕获");
            EstablishConnection establishConnection = connectionHashMap.get(sourcePort);
            if (establishConnection.isConnected) {
                if (finishConnectionHashMap.containsKey(sourcePort)) {
                    FinishConnection finishConnection = finishConnectionHashMap.get(sourcePort);
                    if(finishConnection.isFin())
                        return true; //错误释放请求不予回复

                    if (finishConnection.getData(receiveDataFormat)) {

                        if (!finishConnection.isFin) {
                            Transport.send(finishConnection.getApply(receiveDataFormat), socket, addressCon, receiveDataFormat.getSourcePort());
                            return true;
                        } else {
                            return false;//让滑动窗口来
                        }

                    }
                    else
                        return false; //流氓释放请求

                } else {
                    finishConnectionHashMap.put(sourcePort, new FinishConnection(true));
                    FinishConnection finishConnection = finishConnectionHashMap.get(sourcePort);
                    if (finishConnection.getData(receiveDataFormat)) {
                        Transport.send(finishConnection.getApply(receiveDataFormat), socket, addressCon, receiveDataFormat.getSourcePort());
                    }
                    return true; //建立新的释放连接
                }
            } else {
                return true; //未连接的释放信号 丢弃
            }
        }
        {
            System.out.println("非释放信号");
            return false;
        }

    }


    public boolean isServer() {
        return isServer;
    }
    public boolean getData(DataFormat dataFormat)  //boolean mean need reply for connect or release
    {
        if(isServer)
            return getDataServer(dataFormat);
        else
            return getDataClient(dataFormat);
    }

    public boolean getDataServer(DataFormat dataFormat)  //return boolean for apply fin
    {
        if(!firstFin)
        {
            if(dataFormat.getPrimitiveType().equals(PrimitiveType.getFinType()))
            {
                lastACK = dataFormat.getAcknowledgementNumber();
                firstFin = true;
                System.out.println("server 收到释放第一次"+dataFormat.getAcknowledgementNumber()+"请求 "+dataFormat.getSourcePort());
                return true;
            }
            else
                return false;
        }
        else{
            if(dataFormat.getPrimitiveType().equals(PrimitiveType.getFinType()))
            {
                if(dataFormat.getAcknowledgementNumber().equals(lastACK+1))
                {
                    secondFin = true;
                    System.out.println("server 收到释放第二次"+dataFormat.getAcknowledgementNumber()+"请求 "+dataFormat.getSourcePort());
                    System.out.println("--------------释放"+dataFormat.getSourcePort()+"成功---------------");

                    return true;
                }
                else {
                    System.out.println("server 收到释放的第二次"+dataFormat.getAcknowledgementNumber()+"请求 "+dataFormat.getSourcePort());
                    return false;
                }
            }
            return false;
        }
    }

    public boolean getDataClient(DataFormat dataFormat)
    {
        if(dataFormat.getPrimitiveType().equals(PrimitiveType.getFinType()) && dataFormat.getAcknowledgementNumber().equals(clientACK+1) && !replyFirstFin)
        {
            replyFirstFin = true;
            clientACK = dataFormat.getAcknowledgementNumber();
            System.out.println("client 收到释放第一次"+dataFormat.getAcknowledgementNumber()+"请求回复 "+dataFormat.getSourcePort());
            return true;
        }
        else if (dataFormat.getPrimitiveType().equals(PrimitiveType.getFinType())
                && dataFormat.getAcknowledgementNumber().equals(clientACK+1)
                && !replySecondFin && replyFirstFin)
        {
            {
                replySecondFin = true;
                System.out.println("client 收到释放第二次"+dataFormat.getAcknowledgementNumber()+"请求回复 "+dataFormat.getSourcePort());
                isFin = true;
                System.out.println("--------------释放"+dataFormat.getSourcePort()+"成功---------------");

                return true;
            }
        }
        else
            return false;
    }



    public DataFormat getApply(DataFormat dataFormat)
    {
        System.out.println("释放信息-----isServer "+isServer+"---"+dataFormat.getSourcePort()+" "+dataFormat.getDestinationPort());
        if(isServer)
            return getApplyServer(dataFormat);
        else
            return getApplyClient(dataFormat);
    }

    public DataFormat getApplyClient(DataFormat dataFormat)
    {
        int des = dataFormat.getSourcePort();
        int source = dataFormat.getDestinationPort();
//        System.out.println("-----before apply "+isServer+"---"+dataFormat.getSourcePort()+" "+dataFormat.getDestinationPort());
        DataFormat dataFormatReply =new DataFormat();
        dataFormatReply.setPrimitiveType(new PrimitiveType(PrimitiveType.getFinType()));
        dataFormatReply.setDestinationPort(des);
        dataFormatReply.setSourcePort(source);
        dataFormatReply.setAcknowledgementNumber(dataFormat.getAcknowledgementNumber());

//        System.out.println("-----Client after apply "+isServer+"---"+dataFormatReply.getSourcePort()+" "+dataFormatReply.getDestinationPort());

        return dataFormatReply;
    }

    public DataFormat getApplyServer(DataFormat dataFormat)
    {
        if(secondFin)
            isFin=true;
        int des = dataFormat.getSourcePort();
        int source = dataFormat.getDestinationPort();
//        System.out.println("----- Server before apply "+isServer+"---"+dataFormat.getSourcePort()+" "+dataFormat.getDestinationPort());

        DataFormat dataFormatReply =new DataFormat();
        dataFormatReply.setPrimitiveType(new PrimitiveType(PrimitiveType.getFinType()));
        dataFormatReply.setDestinationPort(des);
        dataFormatReply.setSourcePort(source);
        dataFormatReply.setAcknowledgementNumber(dataFormat.getAcknowledgementNumber()+1);

//        System.out.println("-----Server after apply "+isServer+"---"+dataFormatReply.getSourcePort()+" "+dataFormatReply.getDestinationPort());

        return dataFormatReply;
    }
}
