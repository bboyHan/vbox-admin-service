package com.vbox.common.util;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.vbox.persistent.entity.ChannelPre;
import com.vbox.persistent.pojo.dto.ChannelAccountExcel;
import com.vbox.persistent.pojo.dto.ChannelPreExcel;
import jodd.util.StringUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtil {

    public static List<ChannelAccountExcel> parseChannelAccountExcel(MultipartFile file) throws IOException {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        List<ChannelAccountExcel> data = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0) {
                continue; // 跳过表头行
            }
            ChannelAccountExcel channelAccountExcel = new ChannelAccountExcel();
            mapRowToClassAccount(row, channelAccountExcel);
            data.add(channelAccountExcel);
        }

        workbook.close();

        return data;
    }

    public static List<ChannelPreExcel> parseChannelPreExcel(MultipartFile file) throws IOException {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        List<ChannelPreExcel> data = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0) {
                continue; // 跳过表头行
            }
            ChannelPreExcel channelPre = new ChannelPreExcel();
            mapRowToClassPre(row, channelPre);
            data.add(channelPre);
        }

        workbook.close();

        return data;
    }

    public static  void mapRowToClassAccount(Row row, ChannelAccountExcel channelAccountExcel) {
        Field[] fields = ChannelAccountExcel.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);
            Cell cell = row.getCell(i);
            Class<?> fieldType = field.getType();

            try {
                if (fieldType.equals(Integer.class)) {
                    if (cell == null || cell.getCellType() == CellType.BLANK) {
                        field.set(channelAccountExcel, null);
                    } else if (cell.getCellType() == CellType.NUMERIC) {
                        field.set(channelAccountExcel, (int) cell.getNumericCellValue());
                    } else if (cell.getCellType() == CellType.STRING) {
                        field.set(channelAccountExcel, Integer.parseInt(cell.getStringCellValue()));
                    }
                } else if (fieldType.equals(String.class)) {
                    if (cell == null || cell.getCellType() == CellType.BLANK) {
                        field.set(channelAccountExcel, null);
                    } else if (cell.getCellType() == CellType.NUMERIC) {
                        field.set(channelAccountExcel, String.valueOf((long) cell.getNumericCellValue()));
                    } else if (cell.getCellType() == CellType.STRING) {
                        field.set(channelAccountExcel, cell.getStringCellValue());
                    }
                } else if (fieldType.equals(LocalDateTime.class)) {
                    // 处理 LocalDateTime 类型
                    // ...
                }
                // 其他属性类型的处理...
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static  void mapRowToClassPre(Row row, ChannelPreExcel channelPre) {
        Field[] fields = ChannelPreExcel.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);
            Cell cell = row.getCell(i);
            Class<?> fieldType = field.getType();
            try {
                if (fieldType.equals(Integer.class)) {
                    field.set(channelPre, (int) cell.getNumericCellValue());
                } else if (fieldType.equals(String.class)) {
                    field.set(channelPre, cell.getStringCellValue());
                } else if (fieldType.equals(LocalDateTime.class)) {
                    // 处理 LocalDateTime 类型，你可以根据实际需求进行相应的转换
                    // 例如，从字符串转换为 LocalDateTime 对象：
                    // String localDateTimeStr = cell.getStringCellValue();
                    // LocalDateTime localDateTime = LocalDateTime.parse(localDateTimeStr);
                    // field.set(channelPre, localDateTime);
                    // 这里仅作示例，具体转换逻辑需根据Excel中日期时间类型的格式来决定
                }
                // 其他属性类型的处理...
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 判断html中的qrCode - sdo
     */
    public static String getQrCodeValue(String html) {
        String regex = "<input[^>]*name=\"qrCode\"[^>]*value=\"([^\"]+)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * 判断设备情况
     */
    public static boolean isMobileDevice(String userAgent) {
        // 判断逻辑可以根据实际需求进行扩展和改进
        return userAgent != null && (userAgent.toLowerCase().contains("android") || userAgent.toLowerCase().contains("iphone"));
    }

    /**
     * cookie  key
     */
    public static String getCookieValue(String cookie, String key) {
        String[] pairs = cookie.split("[;]");

        for (String pair : pairs) {
            String[] keyValue = pair.trim().split("[:=]");

            if (keyValue.length == 2) {
                String k = keyValue[0].toLowerCase(); // 转换为小写，不区分大小写
                String v = keyValue[1];

                // 使用正则表达式进行模糊匹配
                Pattern pattern = Pattern.compile(key, Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(k);

                if (matcher.find()) {
                    return v;
                }
            }
        }

        return null; // 转换为小写，不区分大小写
    }

    /**
     * ip查询
     */
    public static String ip2region(String ip) {
        if (!StringUtils.hasLength(ip)) {
            System.out.println("未传入ip，跳过");
            return null;
        }
        String property = System.getProperty("user.dir");
        String dbPath = (property + File.separator + "ip2region.xdb");
//        System.out.println(dbPath);
//        String dbPath = "ip2region.xdb";

        // 1、从 dbPath 加载整个 xdb 到内存。
        byte[] cBuff = new byte[8096];
        try {
            cBuff = Searcher.loadContentFromFile(dbPath);
        } catch (Exception e) {
            System.out.printf("failed to load content from `%s`: %s\n", dbPath, e);
        }

        // 2、使用上述的 cBuff 创建一个完全基于内存的查询对象。
        Searcher searcher = null;
        try {
            searcher = Searcher.newWithBuffer(cBuff);
        } catch (Exception e) {
            System.out.printf("failed to create content cached searcher: %s\n", e);
        }

        // 3、查询
        try {
            long sTime = System.nanoTime();
            String region = searcher.search(ip);
            long cost = TimeUnit.NANOSECONDS.toMicros((long) (System.nanoTime() - sTime));
            System.out.printf("{region: %s, ioCount: %d, took: %d μs}\n", region, searcher.getIOCount(), cost);
            return region;
        } catch (Exception e) {
            System.out.printf("failed to search(%s): %s\n", ip, e);
        }
        return null;
    }

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
                + "-9\\-]+\\.)*[a-zA-Z0-9\\-]+\\.[a-zA-Z]{2,6})(\\:[0-9]+)?(/"
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
