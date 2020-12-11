package com.company;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        TreeSet<Integer> tr=new TreeSet<Integer>();
        tr.add(1);
        tr.add(2);
        tr.add(3);
        tr.add(2);
        tr.add(5);
        tr.add(2);
        //遍历
        Object[] obje=tr.toArray();
        tr.removeIf(o -> (Integer) o < 5);
        System.out.println("=======================2");

    }
}
