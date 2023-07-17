package com.vbox.config.aspect;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.vbox.common.enums.ResultEnum;
import com.vbox.common.util.RedisUtil;
import com.vbox.config.annotation.AccessLimit;
import com.vbox.config.exception.AccessLimitException;
import com.vbox.config.exception.UnSupportException;
import com.vbox.config.local.PayerInfoThreadHolder;
import com.vbox.persistent.pojo.dto.PayerInfo;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.security.PublicKey;

@Aspect
@Component
@Slf4j
public class AccessLimitAspect {

    @Resource
    private RedisUtil redisUtil;

    @Pointcut("@annotation(com.vbox.config.annotation.AccessLimit)")
    public void pointcut() {

    }

    @Around("pointcut() && @annotation(accessLimit)")
    public Object doAround(ProceedingJoinPoint joinPoint, AccessLimit accessLimit) throws Exception {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        int limitCount = accessLimit.maxCount();
        int limitSeconds = accessLimit.seconds();
        //获取request
        HttpServletRequest request = attributes.getRequest();
        String contentPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (!StringUtils.hasLength(contentPath) && !contentPath.equals("/")) {
            uri = uri.substring(uri.indexOf(contentPath) + contentPath.length());
        }

        String token = request.getHeader("authorization");
        JWTValidator.of(token).validateDate();

        JWT jwt = JWT.of(token);
        String account = jwt.getPayload("account").toString();
        String pub = jwt.getPayload("pub").toString();

        // check user token
        PublicKey pubKey = SecureUtil.rsa(null, pub).getPublicKey();

        boolean verify = JWTUtil.verify(token, JWTSignerUtil.rs256(pubKey));
        if (!verify) throw new UnSupportException("访问频次过于频繁");

        // 访问限制
        String key = "accessLimit:" + account + ":" + uri;

        long count = redisUtil.incr(key, 1);
        if (count == 1) { // 第一次
            redisUtil.expire(key, limitSeconds);
        }

        if (count > limitCount) {
            log.info("uri={}, limit second={}, limit count={}", uri, limitSeconds, limitCount);
            throw new AccessLimitException("access limit");
        }

        log.info("account={}, uri={}, limit second={}, limit count={}", account, uri, limitSeconds, limitCount);

        PayerInfoThreadHolder.addPayer(new PayerInfo(pub, account));
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 最终通知
     */
    @After("pointcut()")
    public void after(JoinPoint joinpoint){
        PayerInfoThreadHolder.remove();
    }

}
