package com.company.Functions;

import com.company.Utils.DataFormat;

import java.io.IOException;

public interface ReliableFunction {
    public void establishConnection() throws IOException, ClassNotFoundException;
    public void finishConnection() throws IOException, ClassNotFoundException;
    public void timeOut();
    public void send(DataFormat dataFormat,Integer port) throws IOException, ClassNotFoundException;
    public DataFormat receive(DataFormat receiveData) throws IOException, ClassNotFoundException;
}
