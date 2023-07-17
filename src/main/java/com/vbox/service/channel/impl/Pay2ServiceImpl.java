//package com.vbox.service.channel.impl;
//
//import cn.hutool.core.exceptions.ValidateException;
//import com.alibaba.fastjson2.JSONObject;
//import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import com.vbox.common.util.CommonUtil;
//import com.vbox.common.util.ProxyUtil;
//import com.vbox.common.util.RedisUtil;
//import com.vbox.persistent.entity.CChannel;
//import com.vbox.persistent.entity.Channel;
//import com.vbox.persistent.entity.PAccount;
//import com.vbox.persistent.entity.PayOrder;
//import com.vbox.persistent.pojo.dto.CAccountInfo;
//import com.vbox.persistent.pojo.param.OrderCreateParam;
//import com.vbox.persistent.pojo.vo.OrderQueryVO;
//import com.vbox.persistent.repo.*;
//import com.vbox.service.channel.Pay2Service;
//import com.vbox.service.task.Gee4Service;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.dao.DuplicateKeyException;
//import org.springframework.util.StringUtils;
//
//import java.util.SortedMap;
//
//public class Pay2ServiceImpl implements Pay2Service {
//    @Autowired
//    private PAccountMapper pAccountMapper;
//    @Autowired
//    private PAuthMapper pAuthMapper;
//    @Autowired
//    private RelationURMapper urMapper;
//    @Autowired
//    private RoleMapper roleMapper;
//    @Autowired
//    private CAccountMapper cAccountMapper;
//    @Autowired
//    private ChannelMapper channelMapper;
//    @Autowired
//    private CGatewayMapper cGatewayMapper;
//    @Autowired
//    private Gee4Service gee4Service;
//    @Autowired
//    private POrderMapper pOrderMapper;
//    @Autowired
//    private POrderEventMapper pOrderEventMapper;
//    @Autowired
//    private RedisUtil redisUtil;
//    @Autowired
//    private RelationUSMapper relationUSMapper;
//    @Autowired
//    private CAccountWalletMapper cAccountWalletMapper;
//    @Autowired
//    private VboxUserWalletMapper vboxUserWalletMapper;
//
//    @Override
//    public Object createOrder(OrderCreateParam orderCreateParam) throws Exception {
//
//        // 1. param check
//        String pa = orderCreateParam.getP_account();
//        String orderId = orderCreateParam.getP_order_id();
//        CChannel channel = paramCheckCreateOrder(orderCreateParam);
//
//        // 2. proxy setting
//        addProxy(orderCreateParam);
//
//        // 3. 取可用资源账户
////        CAccountInfo randomACInfo = computeCAPool(channel.getId());
//
//        // 4. 建单 for gee4
////        JSONObject orderResp = createJX3order(randomACInfo);
//
//        // 5. 入库
//
//        return null;
//    }
//
//    @Override
//    public Object createTestOrder(Integer num, String acid, String channel, String area) throws Exception {
//        return null;
//    }
//
//    @Override
//    public JSONObject queryOrder(String orderId) throws Exception {
//        return null;
//    }
//
//    @Override
//    public OrderQueryVO queryOrderToP(OrderCreateParam orderCreateParam) throws Exception {
//        return null;
//    }
//
//    public void addProxy(OrderCreateParam orderCreateParam) {
//
//        String payIp = orderCreateParam.getPay_ip();
//        // 如果pay ip为空, 随机地区
//        if (!StringUtils.hasLength(payIp)) {
//            ProxyUtil.addProxy(payIp);
//        }
//
//    }
//
//    /**
//     * 建单参数校验
//     */
//    public CChannel paramCheckCreateOrder(OrderCreateParam orderCreateParam) throws IllegalAccessException {
//        String pa = orderCreateParam.getP_account();
//        String sign = orderCreateParam.getSign();
//        PAccount paDB = pAccountMapper.selectOne((new QueryWrapper<PAccount>()).eq("p_account", pa));
//        String pKey = paDB.getPKey();
//        orderCreateParam.setSign((String) null);
//        SortedMap<String, String> map = CommonUtil.objToTreeMap(orderCreateParam);
//        String signDB = CommonUtil.encodeSign(map, pKey);
//        if (!signDB.equals(sign)) {
//            throw new ValidateException("入参仅限文档包含字段，请核对");
//        } else {
//            String channelId = orderCreateParam.getChannel_id();
//            CChannel channel = channelMapper.getChannelByChannelId(channelId);
//            if (channel == null) {
//                throw new ValidateException("通道id错误，请重新查询确认");
//            } else {
//                String notify = orderCreateParam.getNotify_url();
//                boolean isUrl = CommonUtil.isUrl(notify);
//                if (!isUrl) {
//                    throw new ValidateException("notify_url不合法，请检验入参");
//                } else {
//                    String attach = orderCreateParam.getAttach();
//                    if (attach != null && attach.length() > 128) {
//                        throw new ValidateException("attach不合法，请检验入参");
//                    } else {
//                        String orderId = orderCreateParam.getP_order_id();
//                        if (orderId != null && orderId.length() <= 32 && orderId.length() >= 12) {
//                            PayOrder poDB = pOrderMapper.getPOrderByOid(orderId);
//                            if (poDB != null) {
//                                throw new DuplicateKeyException("该订单已创建，请勿重复操作，order id: " + orderId);
//                            } else {
//                                return channel;
//                            }
//                        } else {
//                            throw new ValidateException("orderId不合法，请检验入参");
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//}
