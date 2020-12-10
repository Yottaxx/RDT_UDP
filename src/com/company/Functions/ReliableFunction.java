package com.company.Functions;

import com.company.Utils.DataFormat;

import java.io.IOException;

public interface ReliableFunction {
    public boolean establishConnection(DataFormat dataFormat) throws IOException, ClassNotFoundException;
    public boolean finishConnection(DataFormat receiveDataFormat) throws IOException, ClassNotFoundException;
    public boolean timeOut();
    public void send(DataFormat dataFormat,Integer port) throws IOException, ClassNotFoundException;
    public DataFormat receive(DataFormat receiveData) throws IOException, ClassNotFoundException;
}
