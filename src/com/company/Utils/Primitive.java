package com.company.Utils;

public enum Primitive {
    ACK("A"),SYN("S"), FIN("F");

    Primitive( String primitive) {
        this.primitive = primitive;
    }

    private final String primitive;

    public static Primitive getInstance(int i){

        switch(i){
            case 0: return ACK;
            case 1: return SYN;
            case 2: return FIN;
            default: return null;
        }

    }


}

