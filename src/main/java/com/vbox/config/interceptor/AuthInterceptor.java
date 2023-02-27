package com.vbox.config.interceptor;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.vbox.config.local.TokenInfoThreadHolder;
import com.vbox.persistent.pojo.dto.TokenInfo;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.PublicKey;
import java.util.Enumeration;
import java.util.List;

public class AuthInterceptor implements HandlerInterceptor {

    @SuppressWarnings("unchecked")
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

//        String method = request.getMethod();
//        Enumeration<String> headerNames = request.getHeaderNames();
//        System.out.println(method);
//        System.out.println(headerNames.toString());

//        if ("OPTIONS".equalsIgnoreCase(method)) {
//            return true;
//        }

        // check expire time
        String token = request.getHeader("authorization");
        JWTValidator.of(token).validateDate();

        JWT jwt = JWT.of(token);
        String account = jwt.getPayload("username").toString();
        String pub = jwt.getPayload("pub").toString();

        // check user token
        PublicKey pubKey = SecureUtil.rsa(null, pub).getPublicKey();

        boolean verify = JWTUtil.verify(token, JWTSignerUtil.rs256(pubKey));
        if (!verify) throw new ValidateException();

        List<String> mIds = (List<String>) jwt.getPayload("mIds");
        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setId(Integer.parseInt(jwt.getPayload("uid").toString()));
        tokenInfo.setUsername(account);
        tokenInfo.setMIds(mIds);

//        System.out.println("mIds: " + mIds);
        TokenInfoThreadHolder.addToken(tokenInfo);
        return true;
    }

    /**
     * 避免内存泄露
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        TokenInfoThreadHolder.remove();
    }
}
