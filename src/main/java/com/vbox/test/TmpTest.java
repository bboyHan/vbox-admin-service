package com.vbox.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TmpTest {

    public static void main(String[] args) {

        Map<Integer, Integer> map = new HashMap<>();

        map.put(1, 11);
        map.put(2, 22);
        map.put(3, 33);
        map.put(4, 44);
        map.put(5, 55);

        HashSet<Integer> rs = new HashSet<>(map.keySet());

        System.out.println(rs);

//        Set<Integer> set = new HashSet<>();
//
//        set.add(1);
//        set.add(2);
//        set.add(3);
//        set.add(4);
//        set.add(5);
//        set.add(6);
//        set.add(7);
//        set.add(8);
//        set.add(8);
//        set.add(8);
//        set.add(8);
//
//        System.out.println(set);

        int[] list = new int[]{1, 33, 14, 76, 28, 2, 4, 6};
//
//        for (int i = 0; i < list.length; i++) {
//            map.put(list[i], i);
//        }

//        Integer v = map.get(3);

//        boolean b = map.containsKey(1);


//        System.out.println(b);
    }
}
