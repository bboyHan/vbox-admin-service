package com.vbox.service.channel.impl;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vbox.common.ResultOfList;
import com.vbox.common.util.CommonUtil;
import com.vbox.common.util.RedisUtil;
import com.vbox.config.exception.NotFoundException;
import com.vbox.config.exception.ServiceException;
import com.vbox.config.local.TokenInfoThreadHolder;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.entity.CChannel;
import com.vbox.persistent.entity.ChannelPre;
import com.vbox.persistent.pojo.dto.CGatewayInfo;
import com.vbox.persistent.pojo.dto.ChannelPreCount;
import com.vbox.persistent.pojo.dto.ChannelPreExcel;
import com.vbox.persistent.pojo.dto.PayInfo;
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
                        remark = money + "|" + money + "," + "null|" + acid + ",test";
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
    public List<CAccount> listCAccount() {
        Integer currentUid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = relationUSMapper.listSidByUid(currentUid);

        sidList.add(currentUid);

        List<CAccount> caList = cAccountMapper.listACInUids(sidList, null, 1, 0, 999);

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
}
