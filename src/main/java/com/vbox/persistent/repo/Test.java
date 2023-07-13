package com.vbox.persistent.repo;

import com.vbox.persistent.entity.CAccount;

import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public class Test {

    public static void main(String[] args) {
        CAccount c = new CAccount();
//        int a = 1 / 0;
        try {
            System.out.println(c.getMax().equals("a"));
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());
        }

        int[] aa = {1, 2, 4};
        System.out.println(aa[3]);

    }

    public static final void test() {
        System.out.println();
    }

    // 计算最大的积木总重量
    private static int getMaxWeight(int N, int[] weights) {
        int maxWeight = 0;
        // 尝试每一种分配方式
        for (int i = 0; i < (1 << N); i++) {
            int sum1 = 0; // 第一堆积木的总重量
            int sum2 = 0; // 第二堆积木的总重量
            boolean forgetCarry = true; // 是否忘记进位
            // 遍历每块积木
            for (int j = 0; j < N; j++) {

                if ((i & (1 << j)) != 0) {
                    if (forgetCarry) {
                        // 忘记进位，转为二进制再进行加法运算
                        sum1 = addWithoutCarry(sum1, weights[j]);
                    } else {
                        sum1 += weights[j];
                    }
                } else {
                    sum2 += weights[j];
                }
                // 检查是否发生进位
                if (sum1 >= 2) {
                    forgetCarry = false;
                }
            }
            // 检查总重量是否相等，并更新最大值
            if (sum1 == sum2 && sum1 > maxWeight) {
                maxWeight = sum1;
            }
        }
        return maxWeight;
    }

    // 将两个数转为二进制再进行加法运算（忽略进位）
    private static int addWithoutCarry(int a, int b) {
        int sum = 0;
        int carry = 0;
        while (a != 0 || b != 0) {
            int bitA = a & 1;
            int bitB = b & 1;
            int resultBit = bitA ^ bitB ^ carry;
            sum |= (resultBit << carry);
            carry = (bitA & bitB) | (bitA & carry) | (bitB & carry);
            a >>= 1;
            b >>= 1;
            carry <<= 1;
        }
        sum |= carry;
        return sum;
    }
}
