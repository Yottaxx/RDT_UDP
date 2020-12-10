package com.company.Functions.Transport;

import com.company.Utils.DataFormat;
import com.company.Utils.PrimitiveType;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

import static java.lang.Integer.max;

public class Transport {

    public static DataFormat receive(DataFormat receiveData, DatagramSocket socket) throws IOException, ClassNotFoundException {
        byte []buf = new byte[512];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
//                String receive = new String(packet.getData(), 0, packet.getLength());
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf);

        //包装流 ：对象流
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

        //内存输入流 读取对象信息
        Object object = objectInputStream.readObject();

        receiveData = (DataFormat) object;

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
    public static void send(DataFormat sendData, DatagramSocket socket, String addressCon, Integer connectionPort) throws IOException {
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(byteOutStream);
        outputStream.writeObject(sendData);

        byte[] buf = byteOutStream.toByteArray();
        byteOutStream.close();
        outputStream.close();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(addressCon),connectionPort);
        socket.send(packet);
        System.out.println("---------send-------- "+sendData.toString());
        System.out.println("---------send-------- "+connectionPort);
    }

    public static void send(DatagramSocket socket, String addressCon, Integer connectionPort,Integer sequenceNum,byte[] data) throws IOException {
            int now = 0;
            for(int i=0;i<data.length/DataFormat.maxBuffer+1;++i)
            {
                DataFormat dataFormat =new DataFormat();
                dataFormat.setSourcePort(socket.getLocalPort());
                dataFormat.setDestinationPort(connectionPort);
                dataFormat.setPrimitiveType(new PrimitiveType(PrimitiveType.getAckType()));

                if(i==data.length/DataFormat.maxBuffer)
                {
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
                send(dataFormat,socket,addressCon,connectionPort);
            }
    }

    public static void send(DatagramSocket socket, String addressCon, Integer connectionPort,Integer sequenceNum,byte[] data,Integer window,Integer ackNum) throws IOException {
        int now = 0;
        for(int i=0;i<data.length/DataFormat.maxBuffer+1;++i)
        {
            DataFormat dataFormat =new DataFormat();
            dataFormat.setSourcePort(socket.getLocalPort());
            dataFormat.setDestinationPort(connectionPort);
            dataFormat.setPrimitiveType(new PrimitiveType(PrimitiveType.getAckType()));
            dataFormat.setAcknowledgementNumber(ackNum);
            if(i==data.length/DataFormat.maxBuffer)
            {
                dataFormat.setBuf(Arrays.copyOfRange(data,i*DataFormat.maxBuffer,data.length%DataFormat.maxBuffer));
                dataFormat.setSequenceNumber(sequenceNum+now+dataFormat.getBuf().length-1);

                now = now+data.length%DataFormat.maxBuffer;
                dataFormat.setWindow(max(window-data.length,0));
            }
            else
            {
                dataFormat.setWindow(DataFormat.maxBuffer);
                dataFormat.setBuf(Arrays.copyOfRange(data,i*DataFormat.maxBuffer,DataFormat.maxBuffer));
                dataFormat.setSequenceNumber(sequenceNum+now+dataFormat.getBuf().length-1);
                now = now+DataFormat.maxBuffer;
            }
            send(dataFormat,socket,addressCon,connectionPort);
        }
    }

}
