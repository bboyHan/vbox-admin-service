package com.vbox;

import cn.hutool.core.codec.Base64;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

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
@EnableScheduling
@EnableWebSocket
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    private static void test() {
        int[][] cptList = new int[3][2];
        JSONArray array = new JSONArray();
        array.add(1);
        array.add(6);
        array.add(8);
        for (int i = 0; i < array.size(); i++) {
            Integer xy = array.getInteger(i);
            if (xy < 3){
                int xi = 1;
                int yi = xy + 1;
                cptList[i] = new int[]{xi, yi};
            }else if (xy < 6){
                int xi = 2;
                int yi = xy - 2;
                cptList[i] = new int[]{xi, yi};
            }else {
                int xi = 3;
                int yi = xy - 5;
                cptList[i] = new int[]{xi, yi};
            }
        }

        System.out.println(JSONArray.toJSONString(cptList));
    }

    /*public static void main(String[] args) throws IOException {

        byte[] sFile = getFile2ByteArray(
                "http://static.geetest.com/captcha_v4/policy/3d0936b11a2c4a65bbb53635e656c780/nine/26973/2023-01-29T08/ef79173b10cb4afcbd5ff7d97810d9e5.jpg",
                "ef79173b10cb4afcbd5ff7d97810d9e5.jpg"
        );

        byte[] tFile = getFile2ByteArray(
                "http://static.geetest.com/nerualpic/v4_pic/nine_prompt/fa414a814eed08800010cc3d942aff02.png",
                "fa414a814eed08800010cc3d942aff02.png"
        );

        String s = Base64.encode(sFile);
        String t = Base64.encode(tFile);

//        String rs = HttpRequest.post("http://127.0.0.1:8090/det/file").form("image", file).execute().body();
//        String rs2 = HttpRequest.post("http://127.0.0.1:8090/det/file/json").form("image", file).execute().body();
//        System.out.println(new String(rs.getBytes(StandardCharsets.UTF_8)));
//        System.out.println(new String(rs2.getBytes(StandardCharsets.UTF_8)));
        JSONObject data = new JSONObject();
        data.put("image", s);
        data.put("title", t);
        data.put("project_name", "geetest4_grid");
        String resp = HttpRequest.post("http://kerlomz-ax88u.asuscomm.com:19196/runtime/text/invoke")
                .body(JSON.toJSONString(data)).
                execute().body();

        System.out.println(resp);

    }

    private static byte[] getFile2ByteArray(String url, String filename) throws IOException {
        URL u = new URL(url);
        URLConnection connection = u.openConnection();
        InputStream is = connection.getInputStream();
        File file = new File(filename);
        FileOutputStream fo = new FileOutputStream(file);

        BufferedInputStream br = new BufferedInputStream(is);
        BufferedOutputStream bo = new BufferedOutputStream(fo);
        byte[] buffer = new byte[br.available()];
        int temp = 0;
        while ((temp = br.read(buffer)) != -1) {
            bo.write(buffer, 0, temp);
        }
        bo.flush();

        fo.write(buffer);
        fo.close();
        bo.close();
        br.close();
        is.close();

        return toByteArray(file);
    }

    public static byte[] toByteArray(File f) throws IOException {

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length())) {
            BufferedInputStream in = null;
            in = new BufferedInputStream(new FileInputStream(f));
            int buf_size = 1024;
            byte[] buffer = new byte[buf_size];
            int len = 0;
            while (-1 != (len = in.read(buffer, 0, buf_size))) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }*/

}
