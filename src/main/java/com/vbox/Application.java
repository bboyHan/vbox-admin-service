package com.vbox;

import cn.hutool.core.codec.Base64;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@MapperScan("com.vbox.persistent.*")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

//    public static void main(String[] args) throws IOException {
//
//        URL url = new URL("https://static.geetest.com/captcha_v4/policy/fdd2aaa4a429487381bd673b104f152d/word/25298/2023-01-12T10/8fb7a29f75b14519b95f1f7d124ef1e6.jpg");
//        URLConnection connection = url.openConnection();
//        InputStream is = connection.getInputStream();
//        File file = new File("8fb7a29f75b14519b95f1f7d124ef1e61.jpg");
//        FileOutputStream fo = new FileOutputStream(file);
//
//        BufferedInputStream br = new BufferedInputStream(is);
//        BufferedOutputStream bo = new BufferedOutputStream(fo);
//        byte[] buffer = new byte[br.available()];
//        int temp = 0;
//        while ((temp = br.read(buffer)) != -1) {
//            bo.write(buffer, 0, temp);
//        }
//        bo.flush();
//
//        fo.write(buffer);
//        fo.close();
//        bo.close();
//        br.close();
//        is.close();
//
//
////        String file64 = Base64.encode(buffer);
//
//
//        String rs = HttpRequest.post("http://127.0.0.1:9898/ocr/file").form("image", file).execute().body();
//        String rs2 = HttpRequest.post("http://127.0.0.1:9898/ocr/file/json").form("image", file).execute().body();
//
//        System.out.println(new String(rs.getBytes(StandardCharsets.UTF_8)));
//        System.out.println(new String(rs2.getBytes(StandardCharsets.UTF_8)));
//
//    }

}
