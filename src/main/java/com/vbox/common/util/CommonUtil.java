package com.vbox.common.util;

import com.alibaba.fastjson2.JSON;
import jodd.util.StringUtil;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtil {
    public static boolean isUrl(String url) {
        if (url == null) {
            return false;
        }
        String regEx = "^(http|https|ftp)\\://([a-zA-Z0-9\\.\\-]+(\\:[a-zA-"
                + "Z0-9\\.&%\\$\\-]+)*@)?((25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{"
                + "2}|[1-9]{1}[0-9]{1}|[1-9])\\.(25[0-5]|2[0-4][0-9]|[0-1]{1}"
                + "[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|"
                + "[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\.(25[0-5]|2[0-"
                + "4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[0-9])|([a-zA-Z0"
                + "-9\\-]+\\.)*[a-zA-Z0-9\\-]+\\.[a-zA-Z]{2,4})(\\:[0-9]+)?(/"
                + "[^/][a-zA-Z0-9\\.\\,\\?\\'\\\\/\\+&%\\$\\=~_\\-@]*)*$";
        Pattern p = Pattern.compile(regEx);
        Matcher matcher = p.matcher(url);
        return matcher.matches();
    }

    /**
     * 输入流转文件
     */
    public static void inputStreamToFile(InputStream ins, File file) {
        BufferedOutputStream bos = null;
        BufferedInputStream bis = new BufferedInputStream(ins);
        try {
            bos = new BufferedOutputStream(Files.newOutputStream(file.toPath()));
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = bis.read(buffer, 0, 8192)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            throw new RuntimeException("上传文件压缩出错", e);
        } finally {
            try {
                ins.close();
            } catch (IOException ignored) {
            }
            ins = null;
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException ignored) {
                }
                bos = null;
            }
            try {
                bis.close();
            } catch (IOException ignored) {
            }
            bis = null;
        }
    }

    /**
     * sign 签名 （参数名按ASCII码从小到大排序（字典序）+key+MD5+转大写签名）
     * @param map
     * @return
     */
    public static String encodeSign(SortedMap<String,String> map, String key){
        if(StringUtil.isEmpty(key)){
            throw new RuntimeException("签名key不能为空");
        }
        Set<Map.Entry<String, String>> entries = map.entrySet();
        Iterator<Map.Entry<String, String>> iterator = entries.iterator();
        List<String> values = new ArrayList<>();

        while(iterator.hasNext()){
            Map.Entry<String, String> entry = iterator.next();
            String k = String.valueOf(entry.getKey());
            String v = String.valueOf(entry.getValue());
            if ((v!=null) && entry.getValue() !=null && !"sign".equals(k) && !"key".equals(k)) {
                values.add(k + "=" + v);
            }
        }
        values.add("key="+ key);
        String sign = StringUtil.join(values, "&");
        return encodeByMD5(sign).toLowerCase();
    }
    /**
     * 通过MD5加密
     *
     * @param algorithmStr
     * @return String
     */
    public static String encodeByMD5(String algorithmStr) {
        if (algorithmStr==null) {
            return null;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(algorithmStr.getBytes(StandardCharsets.UTF_8));
            return getFormattedText(messageDigest.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static String getFormattedText(byte[] b) {
        int i;

        StringBuilder buf = new StringBuilder();
        for (byte value : b) {
            i = value;
            if (i < 0)
                i += 256;
            if (i < 16)
                buf.append("0");
            buf.append(Integer.toHexString(i));
        }
        // 将计算结果s转换为字符串
        return buf.toString();
    }


    /**
     * 将对象转成TreeMap,属性名为key,属性值为value
     *
     * @param object 对象
     */
    public static SortedMap<String, String> objToTreeMap(Object object) throws IllegalAccessException {

        Class<?> clazz = object.getClass();
        TreeMap<String, String> treeMap = new TreeMap<>();

        while ( null != clazz.getSuperclass() ) {
            Field[] declaredFields1 = clazz.getDeclaredFields();

            for (Field field : declaredFields1) {
                String name = field.getName();

                // 获取原来的访问控制权限
                boolean accessFlag = field.isAccessible();
                // 修改访问控制权限
                field.setAccessible(true);
                Object value = field.get(object);
                // 恢复访问控制权限
                field.setAccessible(accessFlag);

                if (null != value && StringUtil.isNotBlank(value.toString())) {
                    //如果是List,将List转换为json字符串
                    if (value instanceof List) {
                        value = JSON.toJSONString(value);
                    }
                    treeMap.put(name, value.toString());
                }
            }

            clazz = clazz.getSuperclass();
        }
        return treeMap;
    }
}
