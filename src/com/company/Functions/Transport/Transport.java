package com.company.Functions.Transport;

import com.company.Functions.Congestion.Congestion;
import com.company.Utils.DataFormat;
import com.company.Utils.PrimitiveType;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.lang.Integer.max;

public class Transport {
    public static final Queue<DataFormat> sendList = new ConcurrentLinkedQueue<DataFormat>();
    public static final ConcurrentHashMap<Integer,DataFormat> sendSet = new ConcurrentHashMap<Integer,DataFormat>();
    public static final Queue<DataFormat> messageQueue = new ConcurrentLinkedQueue<DataFormat>();
    public static final ConcurrentHashMap<Integer,Integer> timeOutReMessage = new ConcurrentHashMap<Integer,Integer>();//3倍超时 即可重传
    public static final ConcurrentHashMap<Integer,Integer> timeChanceReMessage = new ConcurrentHashMap<Integer,Integer>();//3次重传 即丢弃
    public static final Random random = new Random(1000);
    public static final float lossRate = 0.1f;

    public static void timeOut() throws IOException {
        for (Map.Entry<Integer, Integer> entry : timeOutReMessage.entrySet()) {
            if(entry.getValue().equals(0) && entry.getKey()!=0)
            {
                System.out.println("-------超时重传"+entry.getKey()+"----------");
                System.out.println(sendSet.toString());
                if(timeChanceReMessage.contains(entry.getKey()))
                {
                   if(timeChanceReMessage.get(entry.getKey())==3) {
                       timeOutReMessage.remove(entry.getKey());
                        continue;
                   }
                }

                if(sendSet.get(entry.getKey())!=null) {
                    send(sendSet.get(entry.getKey()));
                    Congestion.timeOut();
                    if(!timeChanceReMessage.contains(entry.getKey()))
                        timeChanceReMessage.put(entry.getKey(),1);
                    else
                        timeChanceReMessage.put(entry.getKey(),timeChanceReMessage.get(entry.getKey())+1);

                }
                continue;
            }
            timeOutReMessage.put(entry.getKey(),entry.getValue()-1);
        }
    }

    public static DataFormat receive(DataFormat receiveData, DatagramSocket socket) throws IOException, ClassNotFoundException {
        byte []buf = new byte[512];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        try {
            socket.receive(packet);
        }
        catch (Exception e) {
//            System.out.println( "---------本时间段内无消息---------");
            receiveData =new DataFormat();
            receiveData.setPrimitiveType(new PrimitiveType(PrimitiveType.getEmptyType()));
            return receiveData;
        }
//                String receive = new String(packet.getData(), 0, packet.getLength());
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf);

        //包装流 ：对象流
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        System.out.println("---------receiveLength"+receiveData.getBuf().length+"----------");
        System.out.println("---------receiveLength"+packet.getData().length+"----------");

        //内存输入流 读取对象信息
        Object object = objectInputStream.readObject();

        receiveData = (DataFormat) object;


        messageQueue.add(receiveData);


        byteArrayInputStream.close();
        objectInputStream.close();

        if(receiveData.getPrimitiveType().equals(PrimitiveType.getSynType()))
            System.out.println("---------receive Syn------");
        else if(receiveData.getPrimitiveType().equals(PrimitiveType.getFinType()))
            System.out.println("---------receive Fin------");
        else  if(receiveData.getPrimitiveType().equals(PrimitiveType.getAckType()))
            System.out.println("---------receive ACK------");
        else
            System.out.println("---------receive Wrong type of Message------"+receiveData.getPrimitiveType());
        System.out.println(receiveData);


        return receiveData;
    }

    public static boolean lossPacket()
    {
        int i = (int) (1/lossRate);
        return random.nextInt() % i == 0;
    }
    public static void sendCore(DataFormat sendData, DatagramSocket socket, String addressCon, Integer connectionPort) throws IOException {

        if(lossPacket() && sendData.getPrimitiveType()==PrimitiveType.getAckType()) {
            System.out.println("------------本包丢失----------");
            return ;
        }
        assert sendData.getBuf().length>1 && sendData.getBuf().length<400;
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(byteOutStream);
        outputStream.writeObject(sendData);

        byte[] buf = byteOutStream.toByteArray();
        byteOutStream.close();
        outputStream.close();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(addressCon),connectionPort);
        socket.send(packet);
        System.out.println("---------sendContent-------- "+sendData.toString());
        System.out.println("---------sendPort------- "+connectionPort);
        System.out.println("---------sendWindow-------- "+sendData.getWindow());

    }

    public static void sendCall(DataFormat sendData, DatagramSocket socket, String addressCon) throws IOException {
        assert sendData.getBuf().length>1 && sendData.getBuf().length<400;
        int size = sendList.size();
        for(int i=0;i<size;i++)
        {
            sendData = sendList.peek();
            assert sendData != null;
            Transport.sendCore(sendData,socket,addressCon,sendData.getDestinationPort());
            sendList.poll();

        }

    }

    public static void send(DataFormat sendData, DatagramSocket socket, String addressCon, Integer connectionPort) throws IOException {
        sendList.add(sendData);
    }

    public static void send(DataFormat sendData) throws IOException {
        sendList.add(sendData);

    }

    public static void send(DataFormat sendData,Integer ackNum) throws IOException {
        sendList.add(sendData);
    }


    public static  void timeOutManageInsert(Integer ack,DataFormat dataFormat)
    {
        timeOutReMessage.put(ack,3);
        sendSet.put(ack,dataFormat);
    }

    public static void  timeOutManageRemove(Integer ack)
    {
        timeOutReMessage.remove(ack);
        Congestion.getACK(sendSet.get(ack).getBuf().length);
        sendSet.remove(ack);
    }


}
