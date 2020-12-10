package com.company;

import com.company.Servers.RDTServerListener;

import java.io.IOException;
import java.net.DatagramSocket;

public class Router {
    public static void main(String[] args) throws IOException {
        RDTServerListener rdtServerListener  = new RDTServerListener("hello",5555,5556);
        rdtServerListener.start();

    }

}
