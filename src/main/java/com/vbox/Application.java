package com.vbox;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.vbox.persistent.*")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

//    public static void main(String[] args) {
//
//        String fileName = "111.mp4";
//        String s = new String(Base64.encodeBase64URLSafe(fileName.getBytes()));
//        System.out.println(s);
//    }

}
