//package com.company.Servers;
//
//import com.company.Utils.DataFormat;
//
//import java.io.IOException;
//import java.net.DatagramSocket;
//import java.net.SocketException;
//
//public class RDTFunListener extends RDT implements Runnable {
//
//    public boolean running=false;
//    public RDTFunListener() throws SocketException {
//    }
//
//    public RDTFunListener(String name, int port, int connectionPort) throws SocketException {
//        super(name, port, connectionPort);
//    }
//    public RDTFunListener(String name, DatagramSocket socket, int connectionPort) throws SocketException {
//        super(name, socket,connectionPort);
//    }
//
//    public boolean isRunning() {
//        return running;
//    }
//
//    public void setRunning(boolean running) {
//        this.running = running;
//    }
//
//    @Override
//    public void run() {
//        DataFormat dataFormat = new DataFormat();
//        while (running) {
//            try {
//                receive(dataFormat);
////                send(new DataFormat());
//
//            } catch (IOException | ClassNotFoundException ignored) {
//            }
//        }
//    }
//
//    @Override
//    public boolean establishConnection(DataFormat dataFormat) throws IOException, ClassNotFoundException {
//        return false;
//    }
//}
