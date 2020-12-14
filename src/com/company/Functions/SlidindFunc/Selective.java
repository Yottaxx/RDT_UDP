package com.company.Functions.SlidindFunc;

import com.company.Functions.Congestion.Congestion;
import com.company.Functions.Reliable.EstablishConnection;
import com.company.Functions.Reliable.FinishConnection;
import com.company.Functions.Transport.Transport;
import com.company.Utils.DataFormat;
import com.company.Utils.PrimitiveType;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;

import static java.lang.Integer.max;

public class Selective {
    private boolean isServer = false;
    private List<Integer> dataWindow=new ArrayList<>();
    private Integer pointerSendBegin=0;
    private Integer pointerSendEnd=0;
    private Integer sendWindowSize=1;
    public static final Integer MAX_SEQUENCE_NUM =10240;
    private String addressCon;
    private Integer connectionPort;
    private  Integer serverWindows =2048;
    private DataFormat dataFormat;
    private boolean newData = false;
    public final DatagramSocket socket;
    public TreeSet<Integer> ackList = new TreeSet<>();
    public final ConcurrentHashMap<Integer,DataFormat> sendSet = Transport.sendSet;
    public final Queue<DataFormat> messageQueue = Transport.messageQueue;
    private final HashSet<Integer> confirmed =new HashSet<>();
    public DataFormat getDataFormat() {
        return dataFormat;
    }
    public final ConcurrentHashMap<Integer,Integer> timeOutReMessage = Transport.timeOutReMessage;

    public void setDataFormat(DataFormat dataFormat) {
        this.dataFormat = dataFormat;
        this.newData = true;
    }

    public static final Integer TIME_WAIT=2000;
    Selective(DatagramSocket socket)
    {

        this.socket = socket;
    }

    public boolean isServer() {
        return isServer;
    }

    public void setServer(boolean server) {
        isServer = server;
    }

    public Selective(boolean isServer, String addressCon, Integer connectionPort, DatagramSocket socket)
    {
        System.out.println("--------Selective------"+addressCon);
        this.isServer = isServer;
        this.addressCon=addressCon;
        this.connectionPort = connectionPort;
        this.socket = socket;
    }


    public Selective(String addressCon, Integer connectionPort, DatagramSocket socket)
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
    public static boolean selectiveCore(DataFormat dataFormat, DatagramSocket socket,
                                      String addressCon, HashMap<Integer, EstablishConnection> connectionHashMap,
                                      HashMap<Integer, Selective> selectiveHashMapNHashMap) throws IOException {
        int sourcePort = dataFormat.getSourcePort();
        if(dataFormat.getPrimitiveType().equals(PrimitiveType.getAckType())) {
            if (connectionHashMap.containsKey(sourcePort)) {
                if (connectionHashMap.get(sourcePort).isConnected()) {
                    if (!selectiveHashMapNHashMap.containsKey(sourcePort)) {
                        selectiveHashMapNHashMap.put(sourcePort, new Selective(true, addressCon, sourcePort, socket));
                    }
                    selectiveHashMapNHashMap.get(sourcePort).getACK(dataFormat, socket);
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
        System.out.println("-----Selective----发送窗口1开始-----");
        selectiveNSend(socket, addressCon, connectionPort, 0, new byte[1]);
        pointerSendBegin=0;
        pointerSendEnd=0;
    }


    public boolean getACKClient(DataFormat dataFormat, DatagramSocket socket) throws IOException {
        System.out.println("-------------Selective Client------------");
        System.out.println(dataFormat);
        System.out.println("-----------------应答点----------------");
        System.out.println(pointerSendBegin+" "+pointerSendEnd);

        System.out.println(Arrays.toString(this.ackList.toArray()));

        if (!dataFormat.getPrimitiveType().equals(PrimitiveType.getAckType())) {
            System.out.println("Go Back N 捕获到非ACK消息");
            return false;
        }
        int ackNum = dataFormat.getAcknowledgementNumber();

        Congestion.calculateAnswerTimes(ackNum);

        if (ackNum == -1) {
            beginSend(dataFormat,socket);
            return true;
        }

        sendWindowSize = dataFormat.getWindow();

        if (ackNum < pointerSendBegin || this.ackList.isEmpty()) {
            if(ackNum!=0) {
                System.out.println("-----------忽略过期应答点" + ackNum + "------------"+pointerSendBegin);
                return false;
            }
        }

        if (this.ackList.contains(ackNum)) {

//            this.ackList.removeIf(o -> (Integer) o <=ackNum)
            System.out.println("-----------删除应答点" + ackNum + this.ackList.remove(ackNum)
            + "------------" + Arrays.toString(this.ackList.toArray()));

            //timeout
//            timeOutReMessage.remove(ackNum);
            Transport.timeOutManageRemove(ackNum);

            this.sendSet.remove(ackNum);
            System.out.println("-----------捕捉到应答点" + ackNum + "------------");


            //选择重传
            if (ackList.lower(ackNum)!=null )
            {
                int selective =Collections.min(ackList);
                System.out.println("----------选择重传"+selective+"------------");
                Transport.send(sendSet.get(selective));
            }

//                pointerSendBegin = ackNum;
            if(ackNum+sendWindowSize>=MAX_SEQUENCE_NUM && !ackList.isEmpty()) {
                System.out.println("empty");
                pointerSendBegin = 0;
            }
            else if(ackList.isEmpty())
            {
                pointerSendBegin =ackNum;
            }
            else if(ackList.higher(ackNum)==null){
                System.out.println("others");
                pointerSendBegin = 0;
            }
            else if(ackList.lower(ackNum)==null)
            {
                System.out.println("<");
                pointerSendBegin = ackNum;
            }


            if(pointerSendBegin + sendWindowSize - 1 - pointerSendEnd > 0) {
                System.out.println("-----------还能发送" + (pointerSendBegin + sendWindowSize - 1 - pointerSendEnd) + "字节数据------------");
                System.out.println("-----------从" + (pointerSendEnd) + "开始发送------------");
                System.out.println("-----------从" + (pointerSendBegin) + "开始发送------------");
                System.out.println("-----------从" + (sendWindowSize) + "开始发送------------");

                System.out.println(Arrays.toString(ackList.toArray()));
                if ((pointerSendBegin + sendWindowSize - 1) >= MAX_SEQUENCE_NUM) {
                    selectiveNSend(socket, addressCon, connectionPort,
                            (pointerSendEnd+1)%MAX_SEQUENCE_NUM,
                            new byte[(int) Math.min(Math.max(MAX_SEQUENCE_NUM - pointerSendEnd, 1),Congestion.CWND)]);
                    pointerSendEnd = 0;
                    pointerSendBegin = 0;

                } else {
                    selectiveNSend(socket, addressCon, connectionPort,
                            (pointerSendEnd+1)%MAX_SEQUENCE_NUM,
                            new byte[(int) Math.min(Math.max(pointerSendBegin + sendWindowSize - 1 - pointerSendEnd, 1), Congestion.CWND)]);
                    pointerSendEnd = pointerSendBegin + sendWindowSize - 1;
                }
            }
            return true;
        }
        else if(ackList.isEmpty())
        {
            if(pointerSendBegin + sendWindowSize - 1 - pointerSendEnd > 0) {
                System.out.println("-----------还能发送" + (pointerSendBegin + sendWindowSize - 1 - pointerSendEnd) + "字节数据------------");
                System.out.println("-----------从" + (pointerSendEnd) + "开始发送------------");
                System.out.println("-----------从" + (pointerSendBegin) + "开始发送------------");
                System.out.println("-----------从" + (sendWindowSize) + "开始发送------------");

                System.out.println(Arrays.toString(ackList.toArray()));
                if ((pointerSendBegin + sendWindowSize - 1) >= MAX_SEQUENCE_NUM) {
                    selectiveNSend(socket, addressCon, connectionPort,
                            (pointerSendEnd+1)%MAX_SEQUENCE_NUM,
                            new byte[(int) Math.min(Math.max(MAX_SEQUENCE_NUM - pointerSendEnd, 1),Congestion.CWND)]);
                    pointerSendEnd = 0;
                    pointerSendBegin = 0;

                } else {
                    selectiveNSend(socket, addressCon, connectionPort,
                            (pointerSendEnd+1)%MAX_SEQUENCE_NUM,
                            new byte[(int) Math.min(Math.max(pointerSendBegin + sendWindowSize - 1 - pointerSendEnd, 1), Congestion.CWND)]);
                    pointerSendEnd = pointerSendBegin + sendWindowSize - 1;
                }
            }
        }
        return false;//不需要回复
    }


    public boolean getACKServer(DataFormat dataFormat, DatagramSocket socket) throws IOException {
        System.out.println("-------------Selective Server------------");
        System.out.println(dataFormat);
        if (!dataFormat.getPrimitiveType().equals(PrimitiveType.getAckType())) {
            System.out.println("Selective 捕获到非ACK消息");
            return false;
        }

        //pointerSendBegin 下一次要收到的位置
        int ackNum = dataFormat.getSequenceNumber() + 1;
        if(confirmed.contains(ackNum)) {
            confirmed.remove(ackNum);
            return true;
        }
        if (ackNum > pointerSendBegin) {
            if ((ackNum - dataFormat.getBuf().length) >= pointerSendBegin) {
                if(ackNum==MAX_SEQUENCE_NUM+1) {
                    System.out.println("------------服务端要求重置序号-----------------");
                    pointerSendBegin = 0;
                    selectiveNSend(socket, addressCon, connectionPort, ackNum, new byte[1], 2, -1);
                    return true;
                }
                else
                {
                    //最小得到了才移动窗口
                    if((ackNum - dataFormat.getBuf().length) == pointerSendBegin)
                        pointerSendBegin = ackNum;
                    else
                        confirmed.add(ackNum);

                    selectiveNSend(socket, addressCon, connectionPort, ackNum, new byte[1], serverWindows-messageQueue.size(), ackNum);
                }

            }
            return true;
        }
        return false;//不需要回复
    }

    public void selectiveNSend(DatagramSocket socket, String addressCon, Integer connectionPort,Integer sequenceNum,byte[] data,Integer window,Integer ackNum) throws IOException {
        int now = 0;
        for(int i=0;i<(data.length/DataFormat.maxBuffer)+1;++i)
        {
            DataFormat dataFormat =new DataFormat();
            dataFormat.setSourcePort(socket.getLocalPort());
            dataFormat.setDestinationPort(connectionPort);
            dataFormat.setPrimitiveType(new PrimitiveType(PrimitiveType.getAckType()));
            dataFormat.setAcknowledgementNumber(ackNum);
            if(i==data.length/DataFormat.maxBuffer)
            {
                if(data.length%DataFormat.maxBuffer==0)
                    break;
                dataFormat.setBuf(Arrays.copyOfRange(data,i*DataFormat.maxBuffer,data.length%DataFormat.maxBuffer));
                dataFormat.setSequenceNumber(sequenceNum+now+dataFormat.getBuf().length-1);

                now = now+data.length%DataFormat.maxBuffer;

                if(dataFormat.getAcknowledgementNumber().equals(-1))
                    dataFormat.setWindow(max(window-data.length,0));
                else
                    dataFormat.setWindow(max(serverWindows-messageQueue.size()*200,0));


            }
            else
            {
                dataFormat.setWindow(max(serverWindows-messageQueue.size()*200,0));
                dataFormat.setBuf(Arrays.copyOfRange(data,i*DataFormat.maxBuffer,DataFormat.maxBuffer));
                dataFormat.setSequenceNumber(sequenceNum+now+dataFormat.getBuf().length-1);
                now = now+DataFormat.maxBuffer;
            }

            if (!isServer()) {
                if(dataFormat.getSequenceNumber().equals(MAX_SEQUENCE_NUM)) {
                    System.out.println("--------达到上限重置0 加入应答点" + 0 + "----------");
//                    this.ackList.offer(1);
                    this.pointerSendEnd=0;
                    this.pointerSendBegin=0;
                    System.out.println(pointerSendBegin+" "+pointerSendEnd);
                    System.out.println("--------达到上限重置0 加入应答点 over" + "----------");
                }
                else {
                    System.out.println("--------加入应答点" + (dataFormat.getSequenceNumber() + 1) + "----------");
                    Transport.timeOutManageInsert(dataFormat.getSequenceNumber() + 1,dataFormat);
                }
                System.out.println(Arrays.toString(ackList.toArray()));
            }
            System.out.println("-------------setwindow"+dataFormat.getWindow());
            Transport.send(dataFormat);
        }


    }


    public void selectiveNSend(DatagramSocket socket, String addressCon, Integer connectionPort,Integer sequenceNum,byte[] data) throws IOException {
        int now = 0;
        for(int i=0;i<data.length/DataFormat.maxBuffer+1;++i)
        {
            DataFormat dataFormat =new DataFormat();
            dataFormat.setSourcePort(socket.getLocalPort());
            dataFormat.setDestinationPort(connectionPort);
            dataFormat.setPrimitiveType(new PrimitiveType(PrimitiveType.getAckType()));

            if(i==data.length/DataFormat.maxBuffer)
            {

                if(data.length%DataFormat.maxBuffer==0)
                    break;
                dataFormat.setBuf(Arrays.copyOfRange(data,i*DataFormat.maxBuffer,data.length));
                dataFormat.setSequenceNumber(sequenceNum+now+dataFormat.getBuf().length-1);
                now = now+data.length%DataFormat.maxBuffer;
            }
            else
            {
                dataFormat.setBuf(Arrays.copyOfRange(data,i*DataFormat.maxBuffer,(i+1)*DataFormat.maxBuffer));
                dataFormat.setSequenceNumber(sequenceNum+now+dataFormat.getBuf().length-1);
                now = now+DataFormat.maxBuffer;
            }
            if (!isServer()) {
                if(dataFormat.getSequenceNumber().equals(MAX_SEQUENCE_NUM)) {
                    System.out.println("--------达到上限重置0 加入应答点" + 0 + "----------");
//                        this.ackList.offer(1);
                    this.pointerSendEnd=0;
                    this.pointerSendBegin=0;
                    System.out.println(pointerSendBegin+" "+pointerSendEnd);
                    System.out.println("--------达到上限重置0 加入应答点 over" + "----------");
                }
                else {
                    System.out.println("--------加入应答点" + (dataFormat.getSequenceNumber() + 1) + "----------");
                    this.ackList.add(dataFormat.getSequenceNumber() + 1);
;

                    Transport.timeOutManageInsert(dataFormat.getSequenceNumber() + 1,dataFormat);


                }
                System.out.println(Arrays.toString(ackList.toArray()));
            }

            Transport.send(dataFormat);
        }
    }
}
