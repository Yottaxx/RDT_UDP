package com.company;
import com.company.Servers.RDTServerListener;

import java.io.IOException;

public class Router22 {
    public static void main(String[] args) throws IOException {
        RDTServerListener rdtServerListener  = new RDTServerListener("wait",5556,5555);
        rdtServerListener.start();

    }

}
