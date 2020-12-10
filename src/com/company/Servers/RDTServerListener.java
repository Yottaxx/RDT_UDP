
package com.company.Servers;

import com.company.Functions.Reliable.EstablishConnection;
import com.company.Functions.Reliable.FinishConnection;
import com.company.Functions.ReliableFunction;
import com.company.Functions.SlidindFunc.GoBackN;
import com.company.Functions.SlidingWindow;
import com.company.Functions.Transport.Transport;
import com.company.Utils.DataFormat;
import com.company.Utils.PrimitiveType;

import java.io.*;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

// TODO: 做消息队列，单独线程receive
public class RDTServerListener extends Thread implements ReliableFunction, SlidingWindow {
    protected DatagramSocket socket = null;
    private Integer CWND=512;  //拥塞窗口
    private Integer RWND=512; // 滑动窗口
    public static final Integer TIME_WAIT=2000;
    public Integer connectionPort = 5555;
    public Thread listener;
    public final String addressCon = "localhost";
    public boolean isServer= true;
    Queue<DataFormat> dataList=new LinkedList<DataFormat>();
    HashMap<Integer, EstablishConnection> connectionHashMap = new HashMap<Integer, EstablishConnection>(); //port connection management
    HashMap<Integer,Queue<Integer>> dataWindows = new HashMap<Integer,Queue<Integer>>();  // port receive datas
    HashMap<Integer, FinishConnection> finishConnectionHashMap = new HashMap<Integer, FinishConnection>();
    HashMap<Integer, GoBackN> goBackNHashMap =new HashMap<Integer, GoBackN>();
    private final Object notifyObj = new Object();
    private final Object notifyFunc = new Object();

    public static final Queue<DataFormat> messageQueue = new ConcurrentLinkedQueue<DataFormat>();

    public RDTServerListener() throws SocketException {
        this("RDTServer"+ new Timer().toString(),5555,5556);
    }
    public RDTServerListener(String name,int port,int connectionPort) throws SocketException {
        super(name);
        socket = new DatagramSocket(port);
        socket.setSoTimeout(TIME_WAIT);
        this.connectionPort = connectionPort;
        System.out.println("port"+port+"connection"+connectionPort);

    }
    public RDTServerListener(String name,int port,int connectionPort,boolean isServer) throws SocketException {
        super(name);
        socket = new DatagramSocket(port);
        socket.setSoTimeout(TIME_WAIT);
        this.connectionPort = connectionPort;
        System.out.println("port"+port+"connection"+connectionPort);
        this.isServer = isServer;

    }
    public void scannerThread() {
        Runnable sannerThread = new Runnable() {
            @Override
            public void run() {
                Scanner scan = new Scanner(System.in);
                // 从键盘接收数据
                // next方式接收字符串
                System.out.println("next方式接收：");
                // 判断是否还有输入
                String input = "begin";
                while (!input.equals("exit")) {
                    input = scan.nextLine();
                    if(input.equals("connect"))
                    {
                        System.out.println("请输入destination Port：");
                        input = scan.nextLine();
                        Integer destinationPort = Integer.parseInt(input);
                        DataFormat dataFormat = new DataFormat();
                        dataFormat.setPrimitiveType(new PrimitiveType(PrimitiveType.getSynType()));
                        dataFormat.setSourcePort(socket.getLocalPort());
                        dataFormat.setDestinationPort(destinationPort);
                        System.out.println(dataFormat.getPrimitiveType());
                        connectionHashMap.put(destinationPort,new EstablishConnection(false));
                        System.out.println("------加入连接哈希map port"+destinationPort+"-----");
                        try {
                            send(dataFormat,destinationPort);
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    if(input.equals("fin"))
                    {
                        System.out.println("请输入destination Port：");
                        input = scan.nextLine();
                        Integer destinationPort = Integer.parseInt(input);
                        DataFormat dataFormat = new DataFormat();
                        dataFormat.setPrimitiveType(new PrimitiveType(PrimitiveType.getFinType()));
                        dataFormat.setSourcePort(socket.getLocalPort());
                        dataFormat.setDestinationPort(destinationPort);
                        System.out.println(dataFormat.getPrimitiveType());
                        finishConnectionHashMap.put(destinationPort,new FinishConnection(false));
                        System.out.println("------加入释放哈希map port"+destinationPort+"-----");
                        try {
                            send(dataFormat,destinationPort);
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    if(input.equals("send"))
                    {
                        System.out.println("请输入destination Port：");
                        input = scan.nextLine();
                        Integer destinationPort = Integer.parseInt(input);
                        if(connectionHashMap.containsKey(destinationPort))
                        {
                            if(connectionHashMap.get(destinationPort).isConnected())
                            {
                                goBackNHashMap.put(destinationPort,new GoBackN(false,addressCon,destinationPort, socket));
                                try {
                                    goBackNHashMap.get(destinationPort).beginSend(new DataFormat(),socket);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                }
                scan.close();

            }
        };
        new Thread(sannerThread).start();
    }

    public void receiveThread()
    {
        Runnable runnable =new Runnable() {
            @Override
            public void run() {
                while(true) {
                    DataFormat receiveDataFormat = new DataFormat();
                    synchronized (notifyObj) {
                        // 主线程等待唤醒。
                        try {
                            receiveDataFormat = receive(receiveDataFormat);
                            if (receiveDataFormat.isEmpty())
                                continue;
                            synchronized (notifyFunc) {
                                messageQueue.add(receiveDataFormat);
                                notifyFunc.notifyAll();
                            }
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        System.out.println("---------notifyAll------------");

                        notifyObj.notifyAll();

                    }
//                    try {
//                        System.out.println(Thread.currentThread().getName() + " sleep");
//                        sleep(TIME_WAIT);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }

                }

            }
        };
        new Thread(runnable,"receiveThread").start();
    }




    public void run() {
        try {
            scannerThread();
            establishConnection();
            finishConnection();
            goBackN();
            receiveThread();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void establishConnection() throws IOException, ClassNotFoundException {
        Runnable runnable =new Runnable() {
            @Override
            public void run() {
                while (true) {
                    synchronized (notifyFunc) {
                        try {
                            System.out.println(Thread.currentThread().getName() + " wait");
                            notifyFunc.wait();
                            System.out.println("--------establishConnection----------");
                            if (messageQueue.peek() != null)
                                if (EstablishConnection.establishConnectCore(messageQueue.peek(), socket, addressCon, connectionHashMap))
                                    messageQueue.poll();
                            // 打印输出结果
                            System.out.println("--------establishConnected----------");
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        new Thread(runnable,"establishConnection").start();
    }

    @Override
    public void finishConnection() throws IOException, ClassNotFoundException {
        Runnable runnable =new Runnable() {
            @Override
            public void run() {
                while (true) {
                    synchronized (notifyFunc) {
                        try {
                            // 打印输出结果
                            // 唤醒当前的wait线程
                            System.out.println(Thread.currentThread().getName() + " wait");
                            notifyFunc.wait();
                            System.out.println("--------finishConnection----------");
                            if (messageQueue.peek() != null)
                                if (FinishConnection.finishConnection(messageQueue.peek(), socket, addressCon, connectionHashMap, finishConnectionHashMap))
                                    messageQueue.poll();
                            // 打印输出结果
                            System.out.println("--------finishConnected----------");
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        new Thread(runnable,"finishConnection").start();
    }



    @Override
    public boolean timeOut() {
        return false;
    }

    @Override
    public void send(DataFormat sendData, Integer connectionPort) throws IOException, ClassNotFoundException {
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
//        System.out.println("---------send--------  sPort:"+sendData.getSourcePort()+"dPort:"+sendData.getDestinationPort());
        Transport.send(sendData,socket,this.addressCon,connectionPort);
    }

    @Override
    public DataFormat receive(DataFormat receiveData) throws IOException, ClassNotFoundException {
        return Transport.receive(receiveData,socket);
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
//        if(receiveData.getPrimitiveType().equals(PrimitiveType.getSynType()))
//            System.out.println("---------receive Syn------");
//        else if(receiveData.getPrimitiveType().equals(PrimitiveType.getSynType()))
//            System.out.println("---------receive Fin------");
//        else  if(receiveData.getPrimitiveType().equals(PrimitiveType.getAckType()))
//            System.out.println("---------receive ACK------");
//        else
//            System.out.println("---------receive Wrong type of Message------");
//
//
//        return receiveData;
    }

    @Override
    public void goBackN() {
        Runnable runnable = () -> {
            while (true) {
                synchronized (notifyFunc) {
                    try {
                        // 打印输出结果
                        // 唤醒当前的wait线程
                        System.out.println(Thread.currentThread().getName() + " wait");
                        notifyFunc.wait();
                        System.out.println("--------goBackNConnection----------");
                        if (messageQueue.peek() != null)
                            if (GoBackN.goBackNCore(messageQueue.peek(), socket, addressCon, connectionHashMap, goBackNHashMap))
                                messageQueue.poll();
                        // 打印输出结果
                        System.out.println("--------goBackNConnected----------");
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        new Thread(runnable,"goBackN").start();
//        int sourcePort = dataFormat.getSourcePort();
//        if(dataFormat.getPrimitiveType().equals(PrimitiveType.getAckType())) {
//            if (connectionHashMap.containsKey(sourcePort)) {
//                if (connectionHashMap.get(sourcePort).isConnected()) {
//                    if (!goBackNHashMap.containsKey(sourcePort)) {
//                        goBackNHashMap.put(sourcePort, new GoBackN(true, this.addressCon, this.connectionPort, socket));
//                    }
//                    goBackNHashMap.get(sourcePort).getACK(dataFormat, socket);
//                }
//            } else {
//                System.out.println("-----非连接端口请求-------");
//            }
//        }
    }

    @Override
    public void selectiveRepeat() {

    }
}
