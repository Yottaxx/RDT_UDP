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
//public class RDTFun extends RDT implements Runnable {
//
//    public RDTFun() throws SocketException {
//    }
//
//    public RDTFun(String name, int port, int connectionPort) throws SocketException {
//        super(name, port, connectionPort);
//    }
//    public RDTFun(String name,DatagramSocket socket, int connectionPort) throws SocketException {
//        super(name, socket,connectionPort);
//    }
//
//    @Override
//    public void run() {
//        if(!FunTools(this::establishConnection,connectionPort,"连接"))
//            return;
//        FunTools(this::finishConnection,connectionPort,"释放");
//    }
//
//    @Override
//    public boolean establishConnection(DataFormat dataFormat) throws IOException, ClassNotFoundException {
//        return false;
//    }
//}
