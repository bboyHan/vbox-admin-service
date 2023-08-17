package com.vbox.test;

import java.util.*;
import java.util.concurrent.Callable;

public class TmpTest {

    public static void main(String[] args) {

        RunnableDemo R0 = new RunnableDemo("test-1111");
        R0.start();

        RunnableDemo R1 = new RunnableDemo( "test-2222");
        R1.start();

        RunnableDemo R2 = new RunnableDemo( "test-3333");
        R2.start();
    }

    class A extends Thread{

    }
    class ChaoShiThread implements Runnable {

        @Override
        public void run() {
            System.out.println("正在收银");
        }
    }

    class DDThread implements Callable {

        @Override
        public Object call() throws Exception {
            int a = 111;
            // ...
            return a;
        }
    }

    private static void test() {
        Map<Integer, Object> map = new HashMap<>();
        ArrayList<Integer> value = new ArrayList<>();

        map.put(1, 11);
        map.put(2, value);  // 2 -> [22,44]
        map.put(3, 33);
        map.put(4, 44);
        map.put(5, 55);
        map.put(null, 55);
        map.put(null, 44);
        map.put(44, null);
        map.put(55, null);
        value.add(22);
        value.add(44);
        System.out.println(map);

        HashSet<Integer> rs = new HashSet<>(map.keySet());

        System.out.println(rs);

        Set<Integer> set = new HashSet<>();
//
        set.add(null);
        set.add(null);
        set.add(2);
        set.add(2);
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
        System.out.println(set);
        System.out.println(set.size());

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
