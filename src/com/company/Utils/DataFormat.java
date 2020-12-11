package com.company.Utils;

import java.io.Serializable;
import java.util.Arrays;

public class DataFormat implements Serializable {
    public static final Integer maxBuffer =100; //head 12
    public byte[] sourcePort=new byte[2];
    public byte[] destinationPort=new byte[2];
    public byte[] sequenceNumber=new byte[4];
    public byte[] acknowledgementNumber=new byte[4];
    public byte[] window=new byte[2];
    public byte[] connectionID=new byte[2];
    public byte[] primitiveType =new byte[2];
    public byte[] buf=new byte[1];

    public boolean isEmpty()
    {
        return this.getPrimitiveType().equals(PrimitiveType.getEmptyType());
    }

    public void setWindow(Integer window) {

        this.window = IntegerT16bits(window);
    }

    public void setSourcePort(Integer sourcePort) {

        this.sourcePort = IntegerT16bits(sourcePort);
    }

    public void setDestinationPort(Integer destinationPort) {

        this.destinationPort = IntegerT16bits(destinationPort);
    }

    public Integer getSourcePort() {
        return byte2ArrayToInt(sourcePort);
    }

    public Integer getDestinationPort() {
        return byte2ArrayToInt(destinationPort);
    }

    public Integer getWindow() {
        return byte2ArrayToInt(window);
    }

    public byte[] getBuf() {
        return buf;
    }

    public void setBuf(byte[] buf) {
        this.buf = buf;
    }

    public static Integer getMaxBuffer() {
        return maxBuffer;
    }

    public Integer getSequenceNumber() {
        return byte4ArrayToInt(sequenceNumber);
    }

    public void setSequenceNumber(Integer sequenceNumber) {

        this.sequenceNumber = IntegerT32bits(sequenceNumber);
    }

    public Integer getAcknowledgementNumber() {
        return byte4ArrayToInt(acknowledgementNumber);
    }

    public void setAcknowledgementNumber(Integer acknowledgementNumber) {
        this.acknowledgementNumber = IntegerT32bits(acknowledgementNumber);
    }

    public Integer getConnectionID() {
        return byte2ArrayToInt(connectionID);
    }

    public void setConnectionID(Integer connectionID) {
        this.connectionID = IntegerT16bits(connectionID);
    }

    public Integer getPrimitiveType() {
        return byte2ArrayToInt(primitiveType);
    }

    public void setPrimitiveType(PrimitiveType primitiveType) {
        this.primitiveType = IntegerT16bits(primitiveType.type);
    }


    public static byte[] IntegerT32bits(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    public static byte[] IntegerT16bits(int a) {
        return new byte[]{
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }


    public static int byte4ArrayToInt(byte[] b) {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static int byte2ArrayToInt(byte[] b) {
        return   b[1] & 0xFF |
                (b[0] & 0xFF) << 8;
    }

    @Override
    public String toString() {
        return "DataFormat{" +
                "sourcePort=" + getSourcePort() +
                ", destinationPort=" + getDestinationPort() +
                ", sequenceNumber=" + getSequenceNumber() +
                ", acknowledgementNumber=" +getAcknowledgementNumber() +
                ", window=" + getWindow() +
                ", connectionID=" + getConnectionID() +
                ", primitiveType=" + getPrimitiveType() +
                ", buf=" + buf.length +
                '}';
    }
}
