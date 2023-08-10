package com.vbox.service.channel.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vbox.common.ResultOfList;
import com.vbox.common.constant.CommonConstant;
import com.vbox.common.util.CommonUtil;
import com.vbox.common.util.RedisUtil;
import com.vbox.config.exception.NotFoundException;
import com.vbox.config.exception.ServiceException;
import com.vbox.config.local.ProxyInfoThreadHolder;
import com.vbox.config.local.TokenInfoThreadHolder;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.entity.CChannel;
import com.vbox.persistent.entity.ChannelPre;
import com.vbox.persistent.pojo.dto.CGatewayInfo;
import com.vbox.persistent.pojo.dto.ChannelPreCount;
import com.vbox.persistent.pojo.dto.ChannelPreExcel;
import com.vbox.persistent.pojo.dto.PayInfo;
import com.vbox.persistent.pojo.param.ChannelPreBatchAcListParam;
import com.vbox.persistent.pojo.param.ChannelPreBatchParam;
import com.vbox.persistent.pojo.param.ChannelPreParam;
import com.vbox.persistent.repo.*;
import com.vbox.service.channel.ChannelPreService;
import com.vbox.service.channel.PayService;
import com.vbox.service.channel.SdoPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ChannelPreServiceImpl implements ChannelPreService {
    @Resource
    private RedisUtil redisUtil;
    @Autowired
    private ChannelPreMapper channelPreMapper;
    @Autowired
    private ChannelMapper channelMapper;
    @Autowired
    private CAccountMapper cAccountMapper;
    @Autowired
    private RelationUSMapper relationUSMapper;
    @Autowired
    private PayService payService;
    @Autowired
    private SdoPayService sdoPayService;
    @Autowired
    private CGatewayMapper cGatewayMapper;
    @Autowired
    private Gee4Service gee4Service;
    @Autowired
    private VboxProxyMapper vboxProxyMapper;

    @Override
    public int batchChannelPre(MultipartFile multipartFile) {
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        List<ChannelPreExcel> preExcelList;
        try {
            preExcelList = CommonUtil.parseChannelPreExcel(multipartFile);
//            if (preExcelList.size() > 50) {
//                log.error("batchChannelPre. 超出上限，一次最多50个");
//                throw new ServiceException("文件解析异常");
//            }
        } catch (IOException e) {
            log.error("batchChannelPre. 上传文件解析异常");
            throw new ServiceException("文件解析异常");
        }
        log.warn("本次批量导入 start ... uid: {}", uid);

        int count = 0;
        int errCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (ChannelPreExcel preExcel : preExcelList) {
            try {
                ChannelPre channelPre = new ChannelPre();
                BeanUtils.copyProperties(preExcel, channelPre);

                CAccount ca = cAccountMapper.getCAccountByAcRemark(preExcel.getAcRemark());
                CChannel channelDB = channelMapper.getChannelByChannelId(channelPre.getChannel());

                String acid = ca.getAcid();

//                log.warn("{}",channelPre);
                Integer money = channelPre.getMoney();
                String platParam = channelPre.getPlatParam();
                String platOid = preExcel.getPlatOid();
                String remark = "";
                String url = "";
                String address = "";
                if (channelDB.getCChannelId().contains("sdo")) {
                    if (money == 200) {
                        money = 204;
                        remark = "200|204," + "null|" + acid;
                    } else if (money == 1) {
                        money = 1;
                        remark = "1|1," + "null|" + acid + ",test";
                    } else if (money == 100) {
                        money = 102;
                        remark = "100|102," + "null|" + acid;
                    }

                    if (platParam.contains("_input_charset")) {
                        url = "https://mapi.alipay.com/gateway.do?";
                        boolean flag = platParam.startsWith("_input_charset");
                        if (!flag) {
                            throw new ServiceException("核对queryString参数,示例: _input_charset...或者 app_id=");
                        }
                    } else if ((platParam.contains("app_id"))) {
                        url = "https://openapi.alipay.com/gateway.do?";
                        boolean flag1 = platParam.startsWith("app_id");
                        if (!flag1) {
                            throw new ServiceException("核对queryString参数,示例: _input_charset...或者 app_id=");
                        }
                        boolean flag2 = platParam.endsWith("version=1.0");
                        if (!flag2) {
                            throw new ServiceException("核对queryString参数,示例: _input_charset...或者 app_id=");
                        }
                    } else {
                        throw new ServiceException("核对queryString参数,示例: _input_charset...或者 app_id=");
                    }

                    address = url + platParam;
                } else if (channelDB.getCChannelId().contains("cy")) {
                    if (money == 1) {
                        money = 1;
                        remark = "1|1," + "null|" + acid + ",test";
                    } else {
                        remark = money + "|" + money + "," + "null|" + acid;
                    }

                    // 获取form标签中的action属性值
                    Pattern actionPattern = Pattern.compile("<form[^>]*action=\"([^\"]*)\"");
                    Matcher actionMatcher = actionPattern.matcher(platParam);
                    String resource_url = "";
                    String biz = "";
                    if (actionMatcher.find()) {
                        String actionValue = actionMatcher.group(1);
                        log.warn("Action Value: {}", actionValue);

                        resource_url = actionValue;
                    }

                    // 获取input标签中name为biz_content的value值
                    Pattern inputPattern = Pattern.compile("<input[^>]*name=\"biz_content\"[^>]*value=\"([^\"]*)\"");
                    Matcher inputMatcher = inputPattern.matcher(platParam);
                    if (inputMatcher.find()) {
                        String bizContentValue = inputMatcher.group(1);
                        log.warn("biz_content Value: {}", bizContentValue);
                        bizContentValue = bizContentValue.replaceAll("&quot;", "\"");
                        biz = URLEncoder.encode(bizContentValue, "UTF-8");
                        log.warn("biz_content encode Value: {}", biz);

                        String keyword = "\"out_trade_no\":\"";
                        int startIdx = bizContentValue.indexOf(keyword) + keyword.length();
                        int endIdx = bizContentValue.indexOf("\"", startIdx);

                        platOid = bizContentValue.substring(startIdx, endIdx);
                    }

                    address = resource_url + "&biz_content=" + biz;
                } else if (channelDB.getCChannelId().contains("jx3_alipay_pre")) {
                    if (money == 1) {
                        money = 1;
                        remark = "1|1," + "null|" + acid + ",test";
                    } else {
                        remark = money + "|" + money + "," + "null|" + acid + ",test";
                    }

                    PayInfo payInfo = new PayInfo();
                    CAccount c = cAccountMapper.getCAccountByAcid(acid);
                    CGatewayInfo cgi = cGatewayMapper.getGateWayInfoByCIdAndGId(c.getCid(), c.getGid());
                    payInfo.setChannel(cgi.getCChannel());
                    String account = c.getAcAccount();
                    payInfo.setRepeat_passport(account);
                    payInfo.setGame(cgi.getCGame());
                    payInfo.setGateway(cgi.getCGateway());
                    payInfo.setRecharge_unit(money);
                    payInfo.setRecharge_type(6);
                    String acPwd = c.getAcPwd();
                    String cookie = "";

                    payService.addProxy(null, "127.0.0.1", null);

                    cookie = payService.getCK(account, Base64.decodeStr(acPwd));
                    boolean expire = gee4Service.tokenCheck(cookie, account);
                    if (!expire) {
                        redisUtil.del("account:ck:" + account);
                        cookie = payService.getCK(account, Base64.decodeStr(acPwd));
                        expire = gee4Service.tokenCheck(cookie, account);
                        if (!expire) {
                            throw new NotFoundException("ck问题，请联系管理员");
                        }
                    }

                    payInfo.setCk(cookie);

                    JSONObject orderResp = gee4Service.createOrder(payInfo);
//            orderResp = gee4Service.createOrderForQuery(payInfo);
                    for (int i = 0; i < 10; i++) {
                        if (orderResp != null) {
                            String os = orderResp.toString();
                            if (os.contains("验证码")) {
                                log.warn("验证码不正确，重试 {} 次 : ", i + 1);
                                orderResp = gee4Service.createOrder(payInfo);
                            } else {
                                break;
                            }
                        }
                    }

                    if (orderResp != null && orderResp.get("data") != null) {
                        if (orderResp.getInteger("code") != 1) {
                            String os = orderResp.toString();
                            if (os.contains("冻结")) {
                                log.warn("冻结关号: channel_account : {}", c);
                                cAccountMapper.stopByCaId("账号冻结，请及时查看", c.getId());
                            }
                            throw new ServiceException(os);
                        } else {
                            JSONObject data = orderResp.getJSONObject("data");
                            String platform_oid = data.getString("vouch_code");
                            String resource_url = data.getString("resource_url");

                            log.info("alipay url 初始: {}", resource_url);

                            address = resource_url;
                            platOid = platform_oid;
                        }
                    }


                } else {
                    throw new ServiceException("不支持的通道预产批量导入");
                }

                log.warn("创建预产地址: {}", address);
                channelPre.setAddress(address);
                channelPre.setPlatOid(platOid);
                channelPre.setUid(uid);
                channelPre.setAcid(acid);
                channelPre.setMoney(money);
                channelPre.setChannel(preExcel.getChannel());
                channelPre.setCid(channelDB.getId());
                channelPre.setRemark(remark);
                channelPre.setCreateTime(now);
                channelPreMapper.insert(channelPre);
                count++;

            } catch (Exception e) {
                errCount++;
                log.error("第 {} 行记录参数异常，跳过， info ： {}", count, preExcel, e);
            }
        }

        log.warn("共计本次批量导入总计： {} 条, : {} 条成功, : {} 条失败", preExcelList.size(), count, errCount);

        return 1;
    }

    @Override
    public List<ChannelPreCount> countForCAccounts(ChannelPreParam csParam) {
        Integer currentUid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = relationUSMapper.listSidByUid(currentUid);

        sidList.add(currentUid);

        List<ChannelPreCount> countList = channelPreMapper.countForCAccounts(sidList, 2, csParam.getAcAccount());
        return countList;
    }

    @Override
    public List<CAccount> listCAccount(ChannelPreParam param) {
        Integer currentUid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = relationUSMapper.listSidByUid(currentUid);

        sidList.add(currentUid);

        Integer cid = null;
        Integer status = param.getStatus();

        String channel = param.getChannel();
        if (channel != null) {
            CChannel chan = channelMapper.getChannelByChannelId(channel);
            cid = chan.getId();
        }

        List<CAccount> caList = cAccountMapper.listACInUids(sidList, null, cid, status, 0, 999);

        return caList;
    }

    @Override
    public int createChannelPre(ChannelPreParam csParam) throws Exception {
        Integer uid = TokenInfoThreadHolder.getToken().getId();

        CChannel channelDB = channelMapper.getChannelByChannelId(csParam.getChannel());
        ChannelPre channelPre = new ChannelPre();
        String platOid = csParam.getPlatOid();
        String address = "";
        String remark = "";
        String url = "";
        Integer money = csParam.getMoney();

        String ckid = csParam.getCkid();
        String acid = csParam.getAcid();
        String platParam = csParam.getPlatParam();

        if (channelDB.getCChannelId().contains("sdo")) {
            platParam = platParam.replaceAll("\"", "");

            if (money == 200) {
                money = 204;
                remark = "200|204," + ckid + "|" + acid;
            } else if (money == 1) {
                money = 1;
                remark = "1|1," + ckid + "|" + acid + ",test";
            } else if (money == 100) {
                money = 102;
                remark = "100|102," + ckid + "|" + acid;
            } else {
                throw new ServiceException("仅支持100、200的固额设置");
            }

            if (platParam.contains("_input_charset")) {
                url = "https://mapi.alipay.com/gateway.do?";
                boolean flag = platParam.startsWith("_input_charset");
                if (!flag) {
                    throw new ServiceException("核对queryString参数,示例: _input_charset...或者 app_id=");
                }
            } else if ((platParam.contains("app_id"))) {
                url = "https://openapi.alipay.com/gateway.do?";
                boolean flag1 = platParam.startsWith("app_id");
                if (!flag1) {
                    throw new ServiceException("核对queryString参数,示例: _input_charset...或者 app_id=");
                }
                boolean flag2 = platParam.endsWith("version=1.0");
                if (!flag2) {
                    throw new ServiceException("核对queryString参数,示例: _input_charset...或者 app_id=");
                }
            } else {
                throw new ServiceException("核对queryString参数,示例: _input_charset...或者 app_id=");
            }

            address = url + platParam;
        } else if (channelDB.getCChannelId().contains("cy")) {
            if (money == 1) {
                remark = "1|1," + "null|" + acid + ",test";
            } else {
                remark = money + "|" + money + "," + "null|" + acid;
            }

            // 获取form标签中的action属性值
            Pattern actionPattern = Pattern.compile("<form[^>]*action=\"([^\"]*)\"");
            Matcher actionMatcher = actionPattern.matcher(platParam);
            String resource_url = "";
            String biz = "";
            if (actionMatcher.find()) {
                String actionValue = actionMatcher.group(1);
                log.warn("Action Value: {}", actionValue);

                resource_url = actionValue;
            }

            // 获取input标签中name为biz_content的value值
            Pattern inputPattern = Pattern.compile("<input[^>]*name=\"biz_content\"[^>]*value=\"([^\"]*)\"");
            Matcher inputMatcher = inputPattern.matcher(platParam);
            if (inputMatcher.find()) {
                String bizContentValue = inputMatcher.group(1);
                log.warn("biz_content Value: {}", bizContentValue);
                bizContentValue = bizContentValue.replaceAll("&quot;", "\"");
                biz = URLEncoder.encode(bizContentValue, "UTF-8");
                log.warn("biz_content encode Value: {}", biz);

                String keyword = "\"out_trade_no\":\"";
                int startIdx = bizContentValue.indexOf(keyword) + keyword.length();
                int endIdx = bizContentValue.indexOf("\"", startIdx);

                platOid = bizContentValue.substring(startIdx, endIdx);
            }

            address = resource_url + "&biz_content=" + biz;

        } else if (channelDB.getCChannelId().contains("jx3_alipay_pre")) {
            if (money == 1) {
                remark = "1|1," + "null|" + acid + ",test";
            } else {
                remark = money + "|" + money + "," + "null|" + acid;
            }
//            int count = Integer.parseInt(platParam);

            PayInfo payInfo = new PayInfo();
            CAccount c = cAccountMapper.getCAccountByAcid(acid);
            CGatewayInfo cgi = cGatewayMapper.getGateWayInfoByCIdAndGId(c.getCid(), c.getGid());
            payInfo.setChannel(cgi.getCChannel());
            String account = c.getAcAccount();
            payInfo.setRepeat_passport(account);
            payInfo.setGame(cgi.getCGame());
            payInfo.setGateway(cgi.getCGateway());
            payInfo.setRecharge_unit(money);
            payInfo.setRecharge_type(6);
            String acPwd = c.getAcPwd();
            String cookie = "";
//        if ("jx3_weixin".equals(channelId)) {
//            redisUtil.del("account:ck:" + account);
//            log.info("ck new : - del account {}", account);
//        }

            payService.addProxy(null, "127.0.0.1", null);

            cookie = payService.getCK(account, Base64.decodeStr(acPwd));
            boolean expire = gee4Service.tokenCheck(cookie, account);
            if (!expire) {
                redisUtil.del("account:ck:" + account);
                cookie = payService.getCK(account, Base64.decodeStr(acPwd));
                expire = gee4Service.tokenCheck(cookie, account);
                if (!expire) {
                    throw new NotFoundException("ck问题，请联系管理员");
                }
            }

            payInfo.setCk(cookie);

            JSONObject orderResp = gee4Service.createOrder(payInfo);
//            orderResp = gee4Service.createOrderForQuery(payInfo);
            for (int i = 0; i < 10; i++) {
                if (orderResp != null) {
                    String os = orderResp.toString();
                    if (os.contains("验证码")) {
                        log.warn("验证码不正确，重试 {} 次 : ", i + 1);
                        orderResp = gee4Service.createOrder(payInfo);
                    } else {
                        break;
                    }
                }
            }

            if (orderResp != null && orderResp.get("data") != null) {
                if (orderResp.getInteger("code") != 1) {
                    String os = orderResp.toString();
                    if (os.contains("冻结")) {
                        log.warn("冻结关号: channel_account : {}", c);
                        cAccountMapper.stopByCaId("账号冻结，请及时查看", c.getId());
                    }
                    throw new ServiceException(os);
                } else {
                    JSONObject data = orderResp.getJSONObject("data");
                    String platform_oid = data.getString("vouch_code");
                    String resource_url = data.getString("resource_url");

                    log.info("alipay url 初始: {}", resource_url);

                    address = resource_url;
                    platOid = platform_oid;
                }
            }

        } else {
            throw new ServiceException("channel传参不在服务范围内");
        }

        log.warn("创建预产地址: {}", address);

        channelPre.setAddress(address);

        CAccount caDB = cAccountMapper.getCAccountByAcid(acid);
        String account = caDB.getAcAccount();

        LocalDateTime now = LocalDateTime.now();

        channelPre.setAcAccount(account);
        channelPre.setPlatOid(platOid);
        channelPre.setPlatParam(platParam);
        channelPre.setMoney(money);

        channelPre.setUid(uid);
        channelPre.setAcid(acid);
        channelPre.setCkid(ckid);
        channelPre.setChannel(channelDB.getCChannelId());
        channelPre.setCid(channelDB.getId());
        channelPre.setRemark(remark);
        channelPre.setCreateTime(now);
        int row = channelPreMapper.insert(channelPre);

        return row;
    }

    @Override
    public ResultOfList<List<ChannelPre>> listChannelPre(ChannelPreParam queryParam) {

        Integer uid = TokenInfoThreadHolder.getToken().getId();
        QueryWrapper<ChannelPre> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasLength(queryParam.getPlatOid())) {
            queryWrapper.eq("plat_oid", queryParam.getPlatOid());
        }
        if (StringUtils.hasLength(queryParam.getChannel())) {
            queryWrapper.likeRight("channel", queryParam.getChannel());
        }
        List<Integer> subList = relationUSMapper.listSidByUid(TokenInfoThreadHolder.getToken().getId());
        subList.add(uid);

        if (queryParam.getStatus() != null) {
            queryWrapper.eq("status", queryParam.getStatus());
        }

        if (queryParam.getMoney() != null) {
            queryWrapper.eq("money", queryParam.getMoney());
        }

        queryWrapper.in("uid", subList);
        queryWrapper.orderByDesc("id");

        Page<ChannelPre> page = null;
        if (null != queryParam.getPage() && null != queryParam.getPageSize()) {
            page = new Page<>(queryParam.getPage(), queryParam.getPageSize());
        } else {
            page = new Page<>(1, 20);
        }

        Page<ChannelPre> csPage = channelPreMapper.selectPage(page, queryWrapper);
        List<ChannelPre> records = csPage.getRecords();

        ResultOfList rs = new ResultOfList(records, (int) page.getTotal());
        return rs;
    }


    @Override
    public int updateChannelPre(ChannelPreParam param) {
        ChannelPre channelPre = new ChannelPre();
        channelPre.setId(param.getId());
//        Integer money = param.getMoney();
        String platOid = param.getPlatOid();

        channelPre.setPlatOid(platOid);
        channelPre.setAddress(param.getAddress());

        if (!param.getPlatParam().contains("_input_charset")) {
            throw new ServiceException("核对queryString参数,示例: _input_charset...");
        }

//        String ckid = param.getCkid();
//        CAccount ckAccount = cAccountMapper.getCAccountByAcid(ckid);

//        List<SdoWater> sdoWaters = sdoPayService.queryOrderBy2Day(ckAccount.getAcPwd(), ckid);
//        boolean flag = false;
//        for (SdoWater sdoWater : sdoWaters) {
//            if (sdoWater.getOrderId().equals(platOid)) {
//                boolean moneyFlag = NumberUtil.equals(new BigDecimal(money), new BigDecimal(sdoWater.getOrderAmount()));
//                if (moneyFlag) {
//                    flag = true;
//                }
//            }
//        }
//        if (!flag) throw new ServiceException("核对订单金额");

        int row = channelPreMapper.updateById(channelPre);
        return row;
    }

    @Override
    public int deleteChannelPre(Integer id) {
        int row = channelPreMapper.deleteById(id);
        return row;
    }

    @Override
    public List<CChannel> getChannelPreTypes(ChannelPreParam param) {
        List<CChannel> channels = channelMapper.getChannelPreTypes();
        return channels;
    }

    @Override
    public int batchCreateChannelPre(ChannelPreBatchParam param) {
        Integer uid = TokenInfoThreadHolder.getToken().getId();

        Integer countPre = param.getCount();
        String acRemark = param.getAcRemark();
        String acid = param.getAcid();
        CAccount ca = cAccountMapper.getCAccountByAcid(acid);
        CChannel channelDB = channelMapper.getChannelByChannelId(param.getChannel());

        String platOid = "";
        Integer money = param.getMoney();

        log.warn("本次批量创建 start ..remark: {} .. ca: {}", acRemark, ca);

        int count = 0;
        int errCount = 0;
        LocalDateTime now = LocalDateTime.now();
        payService.addProxy(null, "127.0.0.1", null);
        String remark = "";
        String url = "";

        int amount = 0;
        if (channelDB.getCChannelId().contains("jx3_alipay_pre")) {
            if (money == 1) {
                money = 1;
                remark = "1|1," + "null|" + acid + ",test";
            } else {
                remark = money + "|" + money + "," + "null|" + acid + ",test";
            }
        } else if (channelDB.getCChannelId().contains("sdo_alipay")) {
            if (money == 1) {
                money = 1;
                amount = money * 100;
                remark = "1|1|" + amount + ",null|" + acid + ",test";
            } else if (money == 10) {
                money = 10;
                amount = 1020;
                remark = "10|10|" + amount + ",null|" + acid;
            } else if (money == 30) {
                money = 30;
                amount = 3060;
                remark = "30|30|" + amount + ",null|" + acid;
            } else if (money == 100) {
                money = 102;
                amount = RandomUtil.randomInt(10203, 10205);
//                amount = 10204;
                remark = "100|102|" + amount + ",null|" + acid;
            } else if (money == 200) {
                money = 204;
                amount = RandomUtil.randomInt(20403, 20410);
//                amount = 20409;
                remark = "200|204|" + amount + ",null|" + acid;
            } else {
                amount = money * 100;
                remark = money + "|" + money + "|" + amount + "," + "null|" + acid;
            }
        }
        log.warn("当前amount 设置，{}", amount);
        CAccount c = cAccountMapper.getCAccountByAcid(acid);

        for (int i = 0; i < countPre; i++) {
            try {

                String address = "";
                if (channelDB.getCChannelId().contains("jx3_alipay_pre")) {
                    if (money == 1) {
                        money = 1;
                        remark = "1|1," + "null|" + acid + ",test";
                    } else {
                        remark = money + "|" + money + "," + "null|" + acid + ",test";
                    }

                    PayInfo payInfo = new PayInfo();
                    CGatewayInfo cgi = cGatewayMapper.getGateWayInfoByCIdAndGId(c.getCid(), c.getGid());
                    payInfo.setChannel(cgi.getCChannel());
                    String account = c.getAcAccount();
                    payInfo.setRepeat_passport(account);
                    payInfo.setGame(cgi.getCGame());
                    payInfo.setGateway(cgi.getCGateway());
                    payInfo.setRecharge_unit(money);
                    payInfo.setRecharge_type(6);
                    String acPwd = c.getAcPwd();
                    String cookie = "";
//                    payService.addProxy(null, "127.0.0.1", null);

                    cookie = payService.getCK(account, Base64.decodeStr(acPwd));
                    boolean expire = gee4Service.tokenCheck(cookie, account);
                    if (!expire) {
                        redisUtil.del("account:ck:" + account);
                        cookie = payService.getCK(account, Base64.decodeStr(acPwd));
                        expire = gee4Service.tokenCheck(cookie, account);
                        if (!expire) {
                            throw new NotFoundException("ck问题，请联系管理员");
                        }
                    }

                    payInfo.setCk(cookie);

                    JSONObject orderResp = gee4Service.createOrder(payInfo);
//            orderResp = gee4Service.createOrderForQuery(payInfo);
                    for (int it = 0; it < 10; it++) {
                        if (orderResp != null) {
                            String os = orderResp.toString();
                            if (os.contains("验证码")) {
                                log.warn("验证码不正确，重试 {} 次 : ", it + 1);
                                orderResp = gee4Service.createOrder(payInfo);
                            } else {
                                break;
                            }
                        }
                    }

                    if (orderResp != null && orderResp.get("data") != null) {
                        if (orderResp.getInteger("code") != 1) {
                            String os = orderResp.toString();
                            if (os.contains("冻结")) {
                                log.warn("冻结关号: channel_account : {}", c);
                                cAccountMapper.stopByCaId("账号冻结，请及时查看", c.getId());
                            }
                            throw new ServiceException(os);
                        } else {
                            JSONObject data = orderResp.getJSONObject("data");
                            String platform_oid = data.getString("vouch_code");
                            String resource_url = data.getString("resource_url");

                            log.info("alipay url 初始: {}", resource_url);

                            address = resource_url;
                            platOid = platform_oid;
                        }
                    }
                } else if (channelDB.getCChannelId().contains("sdo_alipay")) {
//                    Thread.sleep(2000);

                    String account = c.getAcAccount();
                    String acPwd = Base64.decodeStr(c.getAcPwd());
                    String cookie = c.getCk();

                    // 从库里那的ck 校验一次
                    boolean expire = sdoPayService.tokenCheck(cookie);

                    if (!expire) {
                        cookie = sdoPayService.getCK(account, acPwd); //有可能从缓存拿一次ck
                        expire = sdoPayService.tokenCheck(cookie);
                        if (expire) {
                            cAccountMapper.updateCkByID(cookie, c.getId());
                        } else { //假设缓存ck过期，删掉，重新官方拿
                            redisUtil.del(CommonConstant.ACCOUNT_CK + c.getAcAccount());
                            cookie = sdoPayService.getCK(account, acPwd);
                            expire = sdoPayService.tokenCheck(cookie);
                            if (expire) {
                                cAccountMapper.updateCkByID(cookie, c.getId());
                            } else {
                                log.error("ck 更新失败");
                                throw new ServiceException("ck问题，请联系管理员");
                            }
                        }
                    }

//                    String payReqUrl = "http://101.89.120.162:555/api/pay";
                    String payReqUrl = vboxProxyMapper.getEnvUrl("sdo_pay");

                    String payResp = HttpRequest.get(payReqUrl)
                            .form("username", account)
                            .form("amount", amount)
                            .form("cookie", cookie)
                            .form("proxy", ProxyInfoThreadHolder.getAddress())
                            .execute()
                            .body();
                    JSONObject payJson = JSONObject.parseObject(payResp);
                    if (payJson.getInteger("code") == 200) {
                        platOid = payJson.getString("orderId");
                        address = payJson.getString("url");
                    } else {
                        log.warn("补偿重试 1次, ac: {}, amount: {}", account, account);
                        payResp = HttpRequest.get(payReqUrl)
                                .form("username", account)
                                .form("amount", amount)
                                .form("cookie", cookie)
                                .form("proxy", ProxyInfoThreadHolder.getAddress())
                                .execute()
                                .body();
                        payJson = JSONObject.parseObject(payResp);
                        if (payJson.getInteger("code") == 200) {
                            platOid = payJson.getString("orderId");
                            address = payJson.getString("url");
                        } else {
                            log.warn("补偿重试 2次, ac: {}, amount: {}", account, account);
                            payResp = HttpRequest.get(payReqUrl)
                                    .form("username", account)
                                    .form("amount", amount)
                                    .form("cookie", cookie)
                                    .form("proxy", ProxyInfoThreadHolder.getAddress())
                                    .execute()
                                    .body();
                            payJson = JSONObject.parseObject(payResp);
                            if (payJson.getInteger("code") == 200) {
                                platOid = payJson.getString("orderId");
                                address = payJson.getString("url");
                            } else {
                                cAccountMapper.updateSysLogByACID(payResp, ca.getAcid());
                                throw new ServiceException("当前sdo账号订单创建异常，info : " + account + ", resp: " + payResp);
                            }
                        }
//                        throw new ServiceException("当前sdo账号订单创建异常，info : " + account + ", resp: " + payResp);
                    }
                } else {
                    throw new ServiceException("不支持的通道预产批量导入");
                }

                log.warn("创建预产地址: {}", address);
                ChannelPre channelPre = new ChannelPre();
                channelPre.setAddress(address);
                channelPre.setPlatOid(platOid);
                channelPre.setUid(uid);
                channelPre.setAcid(acid);
                channelPre.setMoney(money);
                channelPre.setChannel(param.getChannel());
                channelPre.setCid(channelDB.getId());
                channelPre.setRemark(remark);
                channelPre.setAcAccount(ca.getAcAccount());
                channelPre.setCreateTime(now);
                channelPreMapper.insert(channelPre);
                count++;

            } catch (Exception e) {
                errCount++;
                log.error("第 {} 行记录参数异常，跳过， err info ： ", count, e);
            }
        }

        log.warn("共计本次批量创建总计： {} 条, : {} 条产码成功, : {} 条产码失败", countPre, count, errCount);

        return 1;
    }

    @Override
    public int batchCreateChannelPreForAcList(ChannelPreBatchAcListParam param) {

        String channel = param.getChannel();
        Integer count = param.getCount();
        List<String> acidList = param.getAcidList();
        Integer money = param.getMoney();

        if (channel.contains("sdo_alipay")) {
            if (count > 5) throw new ServiceException("不允许超5个");
        } else if (channel.contains("jx3_alipay_pre")) {
            if (count > 20) throw new ServiceException("不允许超20个");
        }
        int successCount = 0;
        int errCount = 0;
        for (String acid : acidList) {
            ChannelPreBatchParam channelPreBatchParam = new ChannelPreBatchParam();
            try {
                CAccount ca = cAccountMapper.getCAccountByAcid(acid);
                channelPreBatchParam.setCount(count);
                channelPreBatchParam.setMoney(money);
                channelPreBatchParam.setAcRemark(ca.getAcRemark());
                channelPreBatchParam.setAcAccount(ca.getAcAccount());
                channelPreBatchParam.setChannel(channel);
                channelPreBatchParam.setAcid(acid);

                int row = batchCreateChannelPre(channelPreBatchParam);
                if (row == 1) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("batchCreateChannelPreForAcList.当前账号预产异常. info : {}", channelPreBatchParam);
                errCount++;
            }
        }

        log.warn("batchCreateChannelPreForAcList.共计本次批量创建总计： {} 个账号, : {} 个账号成功, : {} 个账号失败", acidList.size(), successCount, errCount);

        return 1;
    }

    @Override
    public int clearChannelPre(String acid) {
        CAccount ca = cAccountMapper.getCAccountByAcid(acid);
        Integer cid = ca.getCid();

        int row = channelPreMapper.deleteByACID(acid);
        log.warn("账号清码:[{}] info -> {}", row, ca);
        Set<String> keys = redisUtil.keys(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid + ":*");
        for (String key : keys) {
            log.warn("账号清码时，清理通道keys: {}", key);
            redisUtil.del(key);
        }
        return row;
    }
}
