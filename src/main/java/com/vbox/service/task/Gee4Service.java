package com.vbox.service.task;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.vbox.common.constant.CommonConstant;
import com.vbox.common.util.CommonUtil;
import com.vbox.config.exception.ServiceException;
import com.vbox.config.exception.UnSupportException;
import com.vbox.persistent.pojo.dto.PayInfo;
import com.vbox.persistent.pojo.dto.SecCode;
import com.vbox.persistent.pojo.param.GeeProdCodeParam;
import com.vbox.persistent.pojo.param.GeeVerifyParam;
import com.vbox.persistent.pojo.param.VOrderQueryParam;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class Gee4Service {

    public static String imgUrl = "https://static.geetest.com/";

    //Demo
    /*public Object createOrder() throws Exception {
        JSONObject cap = cap();
        JSONObject pow_detail = cap.getJSONObject("pow_detail");
        String datetime = pow_detail.getString("datetime");
        String lot_number = cap.getString("lot_number");

        JSONObject analysis = analysis(cap);
        JSONArray cptList = analysis.getJSONArray("cptList");
        System.out.println(cptList);
        String w = getW(lot_number, cptList, datetime);
        GeeVerifyParam verifyParam = new GeeVerifyParam();
        verifyParam.setW(w);
        verifyParam.setPayload(cap.getString("payload"));
        verifyParam.setProcess_token(cap.getString("process_token"));
        verifyParam.setCallback("geetest_1673359728134");
        verifyParam.setLot_number(lot_number);
        verifyParam.setCaptcha_id("a7c9ab026dc4366066e4aaad573dce02");
        JSONObject verify = verify(verifyParam);

        JSONObject verifyData = verify.getJSONObject("data");
        SecCode secCode = verifyData.getObject("seccode", SecCode.class);
        secCode.setCaptcha_id("a7c9ab026dc4366066e4aaad573dce02");

        PayInfo payInfo = new PayInfo();
        payInfo.setChannel("weixin");
        payInfo.setGateway("z01");
        payInfo.setRecharge_type(6);
        payInfo.setRecharge_unit(15);
        payInfo.setRepeat_passport("chenzhj11");
        payInfo.setGame("jx3");

        String payload = getPayload(secCode, payInfo);

        GeeProdCodeParam prodCodeParam = new GeeProdCodeParam();
        prodCodeParam.setToken("xoyokey=hG705ysG%26sc%3DphiThaKK%26GnDzzsmy%3DK676K76%26%26GCgsiin%3DphiK%26zseK0x.K8%26Dp0K0.%265K80q7mmy65n%26%3DK6p.i8K%3DDhnx%26i%3Dy6Kya68G5TE%266; expires=Sat, 11-Feb-2023 09:39:37 GMT; path=/; domain=.xoyo.com; httponly");
        prodCodeParam.setPayload(payload);
        prodCodeParam.setEncrypt_fields("payload");
        prodCodeParam.setEncrypt_version("v1");
        prodCodeParam.setEncrypt_method("xoyo_combine");
        JSONObject resp = prodCode(prodCodeParam);
        return resp;
    }*/

    public SecCode capSecCode() throws Exception {
        SecCode secCode = verifyGeeCap();
        return secCode;
    }

    public JSONObject createOrder(PayInfo payInfo) throws Exception {
        SecCode secCode = verifyGeeCap();

//        payInfo.setChannel("weixin");
//        payInfo.setGateway("z01");
//        payInfo.setRecharge_type(6); //通宝type
//        payInfo.setRecharge_unit(15);
//        payInfo.setRepeat_passport("chenzhj11");
//        payInfo.setGame("jx3");

        String payload = getPayload(secCode, payInfo);

        GeeProdCodeParam prodCodeParam = new GeeProdCodeParam();
        prodCodeParam.setToken(payInfo.getCk());
        prodCodeParam.setPayload(payload);
        prodCodeParam.setEncrypt_fields("payload");
        prodCodeParam.setEncrypt_version("v1");
        prodCodeParam.setEncrypt_method("xoyo_combine");
        JSONObject resp = prodCode(prodCodeParam);
        return resp;
    }

    public SecCode verifyGeeCap() throws Exception {
        String captchaId = null;
        JSONObject pow_detail;
        String datetime = null;
        String lot_number = null;
        JSONObject analysis;
        JSONObject cap = null;
        JSONArray cptList = null;

        int capRetry = 0;
        for (int i = 0; i < 10; i++) {
            try {
                capRetry++;
                cap = cap();
                captchaId = cap.getString("captcha_id");
                pow_detail = cap.getJSONObject("pow_detail");
                datetime = pow_detail.getString("datetime");
                lot_number = cap.getString("lot_number");
                String captcha_type = cap.getString("captcha_type");
                if (!captcha_type.equalsIgnoreCase("word")) {
                    continue;
                }
                analysis = analysis(cap);
                cptList = analysis.getJSONArray("cptList");
                log.info("尝试次数 - capRetry : {}", capRetry);
                if (cptList.size() != 3) {
                    log.warn("word analysis error: {}", cptList);
                    continue;
                }
                if (capRetry > 10) {
                    throw new ServiceException("平台验证未通过（请尝试重试请求）");
                }
                break;
            } catch (UnSupportException e) {
                if (capRetry > 10) {
                    throw new ServiceException("平台验证未通过（请尝试重试请求）");
                }
            }
        }
        if (lot_number == null || cptList == null || datetime == null)
            throw new ServiceException("平台验证未通过（请尝试重试请求）");

        String w = getW(lot_number, cptList, datetime);
        GeeVerifyParam verifyParam = new GeeVerifyParam();
        verifyParam.setW(w);
        verifyParam.setPayload(cap.getString("payload"));
        verifyParam.setProcess_token(cap.getString("process_token"));
        verifyParam.setCallback("geetest_" + System.currentTimeMillis());
        verifyParam.setLot_number(lot_number);
        verifyParam.setCaptcha_id(captchaId);
        JSONObject verify = verify(verifyParam);

        JSONObject verifyData = verify.getJSONObject("data");
        SecCode secCode = verifyData.getObject("seccode", SecCode.class);
        secCode.setCaptcha_id(captchaId);
        return secCode;
    }

    public String getW(String lot_num, JSONArray location, String dTime) throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
//        File file = ResourceUtils.getFile("classpath:d2.js");
        ClassPathResource classPathResource = new ClassPathResource("d2.js");
        InputStream is = classPathResource.getInputStream();
        File file = new File("tmp");
        CommonUtil.inputStreamToFile(is, file);
        FileReader reader = new FileReader(file);   // 执行指定脚本
        String w = null;

        try {
            engine.eval(reader);
            if (engine instanceof Invocable) {
                Invocable invoke = (Invocable) engine;    // 调用merge方法，并传入两个参数

                String captchaId = "a7c9ab026dc4366066e4aaad573dce02";

                ScriptObjectMirror c = (ScriptObjectMirror) invoke.invokeFunction("get_param",
                        captchaId,
                        lot_num,
                        location.toString(),
                        dTime
                );
                log.debug("{}", c.get("user_resp"));
                log.debug("{}", c.get("lot_number"));
                log.debug("{}", c.get("d_time"));
                w = (String) invoke.invokeFunction("get_w", c);
                log.debug("{}", "w: ---------\n" + w);
            }
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return w;
    }

    /*{
       captcha_id: 'a7c9ab026dc4366066e4aaad573dce02',
       lot_number: 'fbca77865453438c8c27529fb7d90169',
       pass_token: '624cc20b7597aa4833b3a6de71c28d18d8fce591b18a4fea5b084524d432872c',
       gen_time: '1675369535',
       captcha_output:
         '662a-teKeUpl5FxeT6YiiSbVmvtYPCxaHv_f8xvivNeN2YIW8cuqmG5mytfjn52TZukbefwsbfIc3LxtUmkkTT4MvnZDxAUkXJbDgmFNqqtIgUUSwKDykCkkYYivySzZD5V9gN3e6txd868zifxJLa_320sb8B26qElZLa4HkEKhV9vlSQxn48B3tNAiMGG9-u-raPwFY6idN5gKz3hOS1ofhr-CFSeQSsmSLlhxi3QvCPZTkLivuB4ZEc-Pjqpa0HzEXzvEY6_Vjhhm1KXMkg==',
     }*/
    public String getPayload(SecCode secCode, PayInfo payInfo) throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
//        File file = ResourceUtils.getFile("classpath:d3.js");
        ClassPathResource classPathResource = new ClassPathResource("d3.js");
        InputStream is = classPathResource.getInputStream();
        File file = new File("tmp");
        CommonUtil.inputStreamToFile(is, file);
        FileReader reader = new FileReader(file);
        String payload = null;

        try {
            engine.eval(reader);
            if (engine instanceof Invocable) {
                Invocable invoke = (Invocable) engine;

                ScriptObjectMirror sec = (ScriptObjectMirror) invoke.invokeFunction("get_pay_info",
                        secCode.getCaptcha_id(),
                        secCode.getLot_number(),
                        secCode.getPass_token(),
                        secCode.getGen_time(),
                        secCode.getCaptcha_output(),
                        payInfo.getRepeat_passport(),
                        payInfo.getGateway(),
                        payInfo.getRecharge_type(),
                        payInfo.getRecharge_unit(),
                        payInfo.getGame(),
                        payInfo.getChannel()
                );

                payload = (String) invoke.invokeFunction("get_payload", sec);
                log.debug("{}", "payload = " + payload);
            }
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return payload;
    }

    public JSONObject cap() throws IOException {

        String jsonp = "1ed6f2d396f3230";
        String captchaId = pre_auth(jsonp);
        log.debug("{}", captchaId);

        JSONObject data = loadCaptcha(captchaId);
        data.put("captcha_id", captchaId);

        return data;
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
//        log.info(resp);
        String json = parseGeeJson(resp);

        JSONObject obj = JSONObject.parseObject(json);
        JSONObject data = obj.getJSONObject("data");
        JSONObject config = data.getJSONObject("config");
        String captchaId = config.getString("captchaId");
        return captchaId;
    }

    public boolean tokenCheck(String ck, String account) throws ServiceException {

        String resp = HttpRequest.get("https://pf-api.xoyo.com/passport/user_api/get_info")
                .cookie(ck)
                .execute().body();

        log.info("get_info :{}", resp);

        JSONObject obj = JSONObject.parseObject(resp);
        Integer code = obj.getInteger("code");
        String msg = obj.getString("msg");
        if (code == -10402 || "请先登录".equals(msg)) {
            log.warn("ck is expire, resp: {}", resp);
            return false;
        }
        String uid = obj.getJSONObject("data").getString("uid");
        String pre3 = uid.substring(0, 3);
        String last3 = uid.substring(uid.length() - 3);

        if (account.contains(pre3) || account.contains(last3)) {
            log.info("ck is ok, account: {}, uid: {}, resp: {}", account, uid, resp);
            return true;
        }

        return false;
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
        log.info(resp);
        log.debug("callback :{}", "geetest_" + time);
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

        //windows
//        File sourceFile = downloadFile(sourceImg, "D:/image/" + captcha_type + "/source/");
        //linux
        File sourceFile = downloadFile(sourceImg, "/root/image/" + captcha_type + "/source/");
        JSONArray ques = data.getJSONArray("ques");//https://static.geetest.com/nerualpic/v4_pic/nine_prompt/bd5f4b0419caa97dd2f9b4d3238ff92f.png

        if (captcha_type.equalsIgnoreCase("word")) {
            List<String> target = new ArrayList<>();
            for (Object que : ques) {
                String targetImg = que.toString();
//                File file = downloadFile(targetImg, "D:/image/" + captcha_type + "/target/");
//                target.add(Base64.encode(file)); //analysisImgWord
                target.add(targetImg.substring(targetImg.lastIndexOf("/") + 1)); //analysisImgWordPy
            }
            log.debug(" -- captcha_type:{}\n lot_number:{}\n sourceImg:{}\n" +
                            " ques: {}\n payload: {}\n process_token: {}\n "
                    , captcha_type, lot_number, sourceImg, ques, payload, process_token);

            JSONObject wordResp = analysisImgWordPy(Base64.encode(sourceFile), target);

            return wordResp;
        }

        if (captcha_type.equalsIgnoreCase("nine")) {
            throw new UnSupportException("nine type is not support");
            /*List<byte[]> target = new ArrayList<>();
            for (Object que : ques) {
                String targetImg = que.toString();
                File targetFile = downloadFile(targetImg, "D:/image/" + captcha_type + "/target/");
                target.add(toByteArray(targetFile));
            }
            log.info(" -- captcha_type:{}\n lot_number:{}\n sourceImg:{}\n" +
                            " ques: {}\n payload: {}\n process_token: {}\n "
                    , captcha_type, lot_number, sourceImg, ques, payload, process_token);

            JSONObject nineResp = analysisImgNine(toByteArray(sourceFile), target.get(0));
            return nineResp;*/
        }

        return null;
    }

    //3. download img
//    public File downloadFile(String fileUrl, String filePath) {
//        FileOutputStream fo = null;
//        BufferedInputStream br = null;
//        BufferedOutputStream bo = null;
//        InputStream is = null;
//        try {
//            URL url = new URL(imgUrl + fileUrl);
////        URL url = new URL("https://static.geetest.com/captcha_v4/policy/fdd2aaa4a429487381bd673b104f152d/word/25298/2023-01-12T10/8fb7a29f75b14519b95f1f7d124ef1e6.jpg");
//            URLConnection connection = url.openConnection();
//            connection.setConnectTimeout(3000);//设置连接超时:500ms
//            connection.setReadTimeout(3000);
//            is = connection.getInputStream();
//
//            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
//
//            File file = new File(filePath, fileName);
//            File p = file.getParentFile();
//            if (!p.exists()) {
//                p.mkdirs();
//            }
//
//            fo = new FileOutputStream(file);
//            br = new BufferedInputStream(is);
//            bo = new BufferedOutputStream(fo);
//            byte[] buffer = new byte[br.available()];
//            int temp = 0;
//            while ((temp = br.read(buffer)) != -1) {
//                bo.write(buffer, 0, temp);
//            }
//            bo.flush();
//            fo.write(buffer);
//
//            return file;
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        } finally {
//            try {
//                if (fo != null) fo.close();
//                if (bo != null) bo.close();
//                if (br != null) br.close();
//                if (is != null) is.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
////        Map s = new HashMap();
////        if (capType.equalsIgnoreCase("word")) {
////            String file64 = Base64.encode(file);
////            s.put("b", file64);
////            return s;
////        } else {
////            s.put("f", toByteArray(file));
////            return s;
////        }
//    }
    public File downloadFile(String fileUrl, String filePath) {
        InputStream is = null;
        FileOutputStream os = null;
        try {
            URL url = new URL(imgUrl + fileUrl);
//        URL url = new URL("https://static.geetest.com/captcha_v4/policy/fdd2aaa4a429487381bd673b104f152d/word/25298/2023-01-12T10/8fb7a29f75b14519b95f1f7d124ef1e6.jpg");
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(3000);//设置连接超时:500ms
            connection.setReadTimeout(3000);
            is = connection.getInputStream();
            // 1K的数据缓冲
            byte[] bs = new byte[102400];
            // 读取到的数据长度
            int len;
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            File file = new File(filePath, fileName);
            File p = file.getParentFile();
            if (!p.exists()) {
                p.mkdirs();
            }
            os = new FileOutputStream(filePath + fileName, false);

            while ((len = is.read(bs)) != -1) {
                os.write(bs, 0, len);
            }

            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (null != os){
                try {
                    os.flush();
                    os.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            if (null != is){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
    public JSONObject analysisImgWordPy(String sourceImg, List<String> targetImgs) {
        JSONObject data = new JSONObject();

        data.put("imgs", sourceImg);
        data.put("ques", targetImgs);

        String body = data.toString();
//        System.out.println("api json: -----------\n" + body);
        String resp = HttpRequest.post("http://localhost:9898/captcha/b64")
                .body(body)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36")
                .execute().body();

//        log.info("F-\n" + resp);

        JSONObject obj = JSONObject.parseObject(resp);
        List<Object> cptList = obj.getList("comp", Object.class);
        List<Object> ll = obj.getList("ll", Object.class);


        long time = System.currentTimeMillis();
        log.info("time: {}, location: {}", time, ll);

        JSONObject res = new JSONObject();
        res.put("ll", cptList.size());
        res.put("time", time);
        res.put("cptList", cptList);
        return res;
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
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36")
                .execute().body();

        log.debug("api resp: -----------\n" + resp);

        JSONObject obj = JSONObject.parseObject(resp);
        String[] locationList = obj.getString("data").split("\\|");
        String[] ll = new String[locationList.length];
        int[][] cptList = new int[locationList.length][2];

        //[[1,1],[1,1]]
        for (int i = 0; i < locationList.length; i++) {
            String xy = locationList[i];
            ll[i] = xy;

            String[] split = xy.split(",");
            String x = split[0];
            String y = split[1];
            int xi = Integer.parseInt(x) * 10000 / 300;
            int yi = Integer.parseInt(y) * 10000 / 200;
            cptList[i] = new int[]{xi, yi};
        }

        long time = System.currentTimeMillis();
        log.info("time: {}, location: {}", time, ll);

        JSONObject res = new JSONObject();
        res.put("ll", locationList);
        res.put("time", time);
        res.put("cptList", cptList);
        return res;
    }

    //4. analysis img
    public JSONObject analysisImgNine(byte[] sourceImg, byte[] targetImg) {
        JSONObject data = new JSONObject();

        data.put("project_name", "geetest4_grid");
        data.put("image", Base64.encode(sourceImg));
        data.put("title", Base64.encode(targetImg));

        String body = data.toString();
//        System.out.println("api json: -----------\n" + body);
        String resp = HttpRequest.post("http://kerlomz-ax88u.asuscomm.com:19196/runtime/text/invoke")
                .body(body)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36")
                .execute().body();

        log.debug("api resp: -----------\n" + resp);

        JSONObject obj = JSONObject.parseObject(resp);
        String[] locationList = obj.getString("data").split("\\|");
        String[] ll = new String[locationList.length];
        int[][] cptList = new int[3][2];

        //[[1,1,1]]
        String x = locationList[0];
        ll[0] = x;

        JSONArray array = JSONArray.parseArray(x);
        for (int i = 0; i < array.size(); i++) {
            Integer xy = array.getInteger(i);
            if (xy < 3) {
                int xi = 1;
                int yi = xy + 1;
                cptList[i] = new int[]{xi, yi};
            } else if (xy < 6) {
                int xi = 2;
                int yi = xy - 2;
                cptList[i] = new int[]{xi, yi};
            } else {
                int xi = 3;
                int yi = xy - 5;
                cptList[i] = new int[]{xi, yi};
            }
        }

        long time = System.currentTimeMillis();
        log.info("time: {}, location: {}", time, ll);

        JSONObject res = new JSONObject();
        res.put("ll", locationList);
        res.put("cptList", cptList);
        res.put("time", time);
        return res;

    }

    //5. verify
    public JSONObject verify(GeeVerifyParam geeVerifyParam) {
        String resp = HttpRequest.get("https://gcaptcha4.geetest.com/verify")
                .form("captcha_id", geeVerifyParam.getCaptcha_id())
                .form("client_type", "web")
                .form("lot_number", geeVerifyParam.getLot_number())
                .form("payload", geeVerifyParam.getPayload())
                .form("process_token", geeVerifyParam.getProcess_token())
                .form("payload_protocol", "1")
                .form("pt", "1")
                .form("w", geeVerifyParam.getW())
                .form("callback", "geetest_1674967322656")
                .execute().body();
        log.info(resp);
        String json = parseGeeJson(resp);

        JSONObject obj = JSONObject.parseObject(json);
        JSONObject data = obj.getJSONObject("data");
        return obj;
    }

    public static String parseGeeJson(String resp) {
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

    public JSONObject analysis(JSONObject data) {

        return downloadAndAnalysis(data);
    }

    public JSONObject prodCode(GeeProdCodeParam param) {

        param.setEncrypt_fields("payload");
        param.setEncrypt_version("v1");
        param.setEncrypt_method("xoyo_combine");
        String jp = JSON.toJSONString(param);

        String resp = HttpRequest.get("https://pay-pf-api.xoyo.com/pay/recharge_api/create_order")
                .body(jp)
                .cookie(param.getToken())
                .execute().body();
        log.info(resp);
        JSONObject obj = JSONObject.parseObject(resp);

        return obj;
    }

    public JSONObject queryOrder(VOrderQueryParam param) {
        String resp = HttpRequest.get("https://pay-pf-api.xoyo.com/pay/query_api/query_recharge_order")
                .form("vouch_code", param.getVouch_code())
                .form("type", 0)
                .form("geetest_ctype", "web")
                .form("captcha_id", param.getCaptcha_id())
                .form("lot_number", param.getLot_number())
                .form("pass_token", param.getPass_token())
                .form("gen_time", param.getGen_time())
                .form("captcha_output", param.getCaptcha_output())
                .form("callback", "jsonp_11bc1f40f8918c0")
                .cookie(param.getToken())
                .execute().body();
        log.info(resp);
        String json = parseGeeJson(resp);
        JSONObject jsonResp = JSONObject.parseObject(json);
        Integer code = jsonResp.getInteger("code");

        if (code != 1) {
            log.error("gct可能过期，resp: {}", jsonResp);
            throw new ServiceException(String.format("gct可能过期，请联系管理员, msg: %s", jsonResp));
        }

        return jsonResp;
//        return data == null ? obj : data;
    }
}
