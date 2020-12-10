//package com.company.Servers;
//
//import com.company.Functions.ReliableFunction;
//import com.company.Functions.SlidingWindow;
//import com.company.Utils.DataFormat;
//import com.company.Utils.PrimitiveType;
//
//import java.io.*;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.InetAddress;
//import java.net.SocketException;
//import java.util.Timer;
//import java.util.concurrent.Callable;
//
//public class RDT implements ReliableFunction, SlidingWindow {
//    protected DatagramSocket socket = null;
//    private Integer CWND;  //拥塞窗口
//    private Integer RWND; // 滑动窗口
//    public static final Integer TIME_WAIT=2000;
//    public Integer connectionPort = 5555;
//    public final String addressCon = "localhost";
//
//    public RDT() throws SocketException {
//        this("RDTServer"+ new Timer().toString(),5555,5556);
//    }
//    public RDT(String name, int port, int connectionPort) throws SocketException {
//        socket = new DatagramSocket(port);
//        socket.setSoTimeout(TIME_WAIT);
//        this.connectionPort = connectionPort;
//        System.out.println("port"+port+"connection"+connectionPort);
//
//    }
//
//    public RDT(String name, DatagramSocket socket, int connectionPort) throws SocketException {
//        this.socket = socket;
//        this.connectionPort = connectionPort;
//        System.out.println("port"+socket.getLocalPort()+"connection"+connectionPort);
//
//    }
//
//    public boolean FunTools(Callable<Boolean> func,Integer connectionPort,String funcType)
//    {
//        boolean flagCon = false;
//        for(int i=0;i<2;++i)
//        {
//            try {
//                if (!func.call()) {
//                    System.out.println("-------------"+funcType+"出错---重试中------------");
//                }
//                else {
//                    flagCon=true;
//                    break;
//                }
//            } catch (IOException | ClassNotFoundException e) {
//                e.printStackTrace();
//                System.out.println("-------------"+funcType+"出错---重试中------------");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        if(flagCon) {
//            System.out.println("-------------"+funcType+" "+ this.connectionPort + "成功---------------");
//            return true;
//        }
//        else {
//            System.out.println("-------------连接"+funcType+" "+ this.connectionPort + "失败---------------");
//            return false;
//        }
//    }
//
//
//    @Override
//    public boolean establishConnection(DataFormat dataFormat) throws IOException, ClassNotFoundException {
//        return false;
//    }
//
//    @Override
//    public boolean establishConnection() throws IOException, ClassNotFoundException {
//
//        DataFormat establishData = new DataFormat();
//        establishData.setPrimitiveType(new PrimitiveType(PrimitiveType.getSynType()));
//        send(establishData);
//        DataFormat receiveData = new DataFormat();
//
//        try {
//            //设置超时时间,2秒
//            receive(receiveData);
//        } catch (Exception e) {
//            System.out.println( "----------TIME OUT"+"------------");
//            return false;
//        }
//
//        establishData = new DataFormat();
//        establishData.setPrimitiveType(new PrimitiveType(PrimitiveType.getSynType()));
//        send(establishData);
//        return true;
//
//    }
//
//    @Override
//    public boolean finishConnection() throws IOException, ClassNotFoundException {
//        boolean flag1=false,flag2=false;
//        for(int i=0;i<2;++i)
//        {
//            if(finishConnectionCore()) {
//                flag1=true;
//                break;
//            }
//        }
//        if(!flag1)
//            return false;
//        for(int i=0;i<2;++i)
//        {
//            if(finishConnectionCore()) {
//                flag2=true;
//                break;
//            }
//        }
//        return flag2;
//    }
//
//    public boolean finishConnectionCore() throws IOException, ClassNotFoundException {
//        DataFormat establishData = new DataFormat();
//        establishData.setPrimitiveType(new PrimitiveType(PrimitiveType.getFinType()));
//        send(establishData);
//        DataFormat receiveData = new DataFormat();
//
//        try {
//            //设置超时时间,2秒
//            receive(receiveData);
//        } catch (Exception e) {
//            System.out.println( "----------TIME OUT"+"------------");
//            return false;
//        }
//
//        return true;
//    }
//
//    @Override
//    public boolean transportMessage() {
//        return false;
//    }
//
//    @Override
//    public boolean timeOut() {
//        return false;
//    }
//
//    @Override
//    public void send(DataFormat sendData) throws IOException, ClassNotFoundException {
//        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
//        ObjectOutputStream outputStream = new ObjectOutputStream(byteOutStream);
//        outputStream.writeObject(sendData);
//
//        byte[] buf = byteOutStream.toByteArray();
//        byteOutStream.close();
//        outputStream.close();
//
//        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(this.addressCon),this.connectionPort);
//        socket.send(packet);
//        System.out.println("---------send--------"+sendData.toString());
//    }
//
//    @Override
//    public void receive(DataFormat receiveData) throws IOException, ClassNotFoundException {
//        byte []buf = new byte[512];
//        DatagramPacket packet = new DatagramPacket(buf, buf.length);
//        socket.receive(packet);
////                String receive = new String(packet.getData(), 0, packet.getLength());
//        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf);
//
//        //包装流 ：对象流
//        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
//
//        //内存输入流 读取对象信息
//        Object object = objectInputStream.readObject();
//
//        receiveData = (DataFormat) object;
//
//        byteArrayInputStream.close();
//        objectInputStream.close();
//        System.out.println("---------receive------"+receiveData.toString());
//    }
//
//    @Override
//    public void GoBackN() {
//
//    }
//
//    @Override
//    public void SelectiveRepeat() {
//
//    }
//}
