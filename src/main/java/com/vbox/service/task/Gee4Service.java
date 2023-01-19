package com.vbox.service.task;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class Gee4Service {

    public static String imgUrl = "https://static.geetest.com/";

    public JSONObject cap() throws IOException {

        String jsonp = "1ed6f2d396f3230";
        String captchaId = pre_auth(jsonp);
        System.out.println(captchaId);

        JSONObject data = loadCaptcha(captchaId);
        JSONObject rs = downloadAndAnalysis(data);

        return rs;
//        String resp = HttpRequest.get("https://gcaptcha4.geetest.com/verify")
//                .form("captcha_id", captchaId)//
//                .form("client_type", "web")
//                .form("lot_number", "lotNum")//
//                .form("payload", "payload")//
//                .form("process_token", "processToken")//
//                .form("payload_protocol", "1")
//                .form("pt", "1")
//                .form("w", "1")//
//                .form("callback", "callback")//
//                .execute().body();
    }


    // 1. pre auth
    public String pre_auth(String jsonp) {
        //https://pf-api.xoyo.com/passport/user_api/get_info?callback=jsonp_e99e7f8206ba80

        String resp = HttpRequest.get("https://pf-api.xoyo.com/passport/common_api/pre_auth?version=v4&api=pay%2Frecharge_api%2Fcreate_order&data%5Brecharge_source%5D=3&data%5Bchannel%5D=alipay_mobile&callback=jsonp_" + jsonp)
                .execute().body();

        String json = parseGeeJson(resp);

        JSONObject obj = JSONObject.parseObject(json);
        JSONObject data = obj.getJSONObject("data");
        JSONObject config = data.getJSONObject("config");
        String captchaId = config.getString("captchaId");
        return captchaId;
    }


    //2. load captcha
    public JSONObject loadCaptcha(String captchaId) throws IOException {
        String time = System.currentTimeMillis() + "";

        //https://gcaptcha4.geetest.com/load?captcha_id=a7c9ab026dc4366066e4aaad573dce02&challenge=a9464f15-30ac-44a2-8fe6-07e456ebfbb8&client_type=web&lang=zh-cn&callback=geetest_1674011720194

        String challenge = IdUtil.randomUUID();

        String resp = HttpRequest.get("https://gcaptcha4.geetest.com/load")
                .form("captcha_id", captchaId)
                .form("challenge", challenge)
                .form("client_type", "web")
                .form("lang", "zh-cn")
                .form("callback", "geetest_" + time)
                .execute().body();

        log.info("callback :{}", "geetest_" + time);
        String s = parseGeeJson(resp);

        JSONObject obj = JSONObject.parseObject(s);
        JSONObject data = obj.getJSONObject("data");

        return data;
    }

    // download and analysis
    public JSONObject downloadAndAnalysis(JSONObject data) {
        String lot_number = data.getString("lot_number");
        String captcha_type = data.getString("captcha_type");
        String payload = data.getString("payload");
        String process_token = data.getString("process_token");

        String sourceImg = data.getString("imgs");//https://static.geetest.com/captcha_v4/policy/3d0936b11a2c4a65bbb53635e656c780/nine/25897/2023-01-18T10/7f501f264c1a4c81a4754cdc0e593154.jpg

        File sourceFile = downloadFile(sourceImg, "D:/image/" + captcha_type + "/source/");
        JSONArray ques = data.getJSONArray("ques");//https://static.geetest.com/nerualpic/v4_pic/nine_prompt/bd5f4b0419caa97dd2f9b4d3238ff92f.png

        if (captcha_type.equalsIgnoreCase("word")) {
            List<String> target = new ArrayList<>();
            for (Object que : ques) {
                String targetImg = que.toString();
                File file = downloadFile(targetImg, "D:/image/" + captcha_type + "/target/");
                target.add(Base64.encode(file));
            }
            log.info(" -- captcha_type:{}\n lot_number:{}\n sourceImg:{}\n" +
                            " ques: {}\n payload: {}\n process_token: {}\n "
                    , captcha_type, lot_number, sourceImg, ques, payload, process_token);

            JSONObject wordResp = analysisImgWord(Base64.encode(sourceFile), target);
            return wordResp;
        }

        if (captcha_type.equalsIgnoreCase("nine")) {
            List<byte[]> target = new ArrayList<>();
            for (Object que : ques) {
                String targetImg = que.toString();
                File targetFile = downloadFile(targetImg, "D:/image/" + captcha_type + "/target/");
                target.add(toByteArray(targetFile));
            }
            log.info(" -- captcha_type:{}\n lot_number:{}\n sourceImg:{}\n" +
                            " ques: {}\n payload: {}\n process_token: {}\n "
                    , captcha_type, lot_number, sourceImg, ques, payload, process_token);

            JSONObject nineResp = analysisImgNine(toByteArray(sourceFile), target.get(0));
            return nineResp;
        }

        return null;
    }

    //3. download img
    public File downloadFile(String fileUrl, String filePath) {
        FileOutputStream fo = null;
        BufferedInputStream br = null;
        BufferedOutputStream bo = null;
        InputStream is = null;
        try {
            URL url = new URL(imgUrl + fileUrl);
//        URL url = new URL("https://static.geetest.com/captcha_v4/policy/fdd2aaa4a429487381bd673b104f152d/word/25298/2023-01-12T10/8fb7a29f75b14519b95f1f7d124ef1e6.jpg");
            URLConnection connection = url.openConnection();
            is = connection.getInputStream();

            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            File file = new File(filePath, fileName);
            File p = file.getParentFile();
            if (!p.exists()) {
                p.mkdirs();
            }

            fo = new FileOutputStream(file);
            br = new BufferedInputStream(is);
            bo = new BufferedOutputStream(fo);
            byte[] buffer = new byte[br.available()];
            int temp = 0;
            while ((temp = br.read(buffer)) != -1) {
                bo.write(buffer, 0, temp);
            }
            bo.flush();
            fo.write(buffer);

            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fo != null) fo.close();
                if (bo != null) bo.close();
                if (br != null) br.close();
                if (is != null) is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//        Map s = new HashMap();
//        if (capType.equalsIgnoreCase("word")) {
//            String file64 = Base64.encode(file);
//            s.put("b", file64);
//            return s;
//        } else {
//            s.put("f", toByteArray(file));
//            return s;
//        }
    }

    //4. analysis img
    public JSONObject analysisImgWord(String sourceImg, List<String> targetImgs) {
        JSONObject data = new JSONObject();

        data.put("image", sourceImg);
        data.put("project_name", "geetest4_zh_sim");
        data.put("title", targetImgs);

        String body = data.toString();
//        System.out.println("api json: -----------\n" + body);
        String resp = HttpRequest.post("http://kerlomz-ax88u.asuscomm.com:19196/runtime/text/invoke")
                .body(body)
                .execute().body();

        System.out.println("api resp: -----------\n" + resp);

        JSONObject obj = JSONObject.parseObject(resp);
        String[] locationList = obj.getString("data").split("\\|");
        String[] ll = new String[locationList.length];

        for (int i = 0; i < locationList.length; i++) {
            ll[i] = locationList[i];
        }

        long time = System.currentTimeMillis();
        log.info("time: {}, location: {}", time, ll);

        JSONObject res = new JSONObject();
        res.put("ll", locationList);
        res.put("time", time);
        return res;
    }

    //4. analysis img
    public JSONObject analysisImgNine(byte[] sourceImg, byte[] targetImg) {
        JSONObject data = new JSONObject();

        data.put("project_name", "geetest4_zh_sim");
        data.put("image", Base64.encode(sourceImg));
        data.put("title", Base64.encode(targetImg));

        String body = data.toString();
//        System.out.println("api json: -----------\n" + body);
        String resp = HttpRequest.post("http://kerlomz-ax88u.asuscomm.com:19196/runtime/text/invoke")
                .body(body)
                .execute().body();

        System.out.println("api resp: -----------\n" + resp);

        JSONObject obj = JSONObject.parseObject(resp);
        String[] locationList = obj.getString("data").split("\\|");
        String[] ll = new String[locationList.length];

        for (int i = 0; i < locationList.length; i++) {
            ll[i] = locationList[i];
        }

        long time = System.currentTimeMillis();
        log.info("time: {}, location: {}", time, ll);

        JSONObject res = new JSONObject();
        res.put("ll", locationList);
        res.put("time", time);
        return res;

    }

    //5. verify
    public String verify(String capId, String lotNum, String payload, String processToken, String callback) {
        String resp = HttpRequest.get("https://gcaptcha4.geetest.com/verify")
                .form("captcha_id", capId)
                .form("client_type", "web")
                .form("lot_number", lotNum)
                .form("payload", payload)
                .form("process_token", processToken)
                .form("payload_protocol", "1")
                .form("pt", "1")
                .form("w", "1")
                .form("callback", callback)
                .execute().body();
        return resp;
    }

    private static String parseGeeJson(String resp) {
        int startIndex = resp.indexOf("(");
        int endIndex = resp.lastIndexOf(")");
        String json = resp.substring(startIndex + 1, endIndex);
        return json;
    }

    public static byte[] toByteArray(File f) {

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
        }
        return null;
    }
}
