package com.vbox.api;

import com.vbox.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class FileController {

//    @Autowired
//    private FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<Result<Integer>> upload(HttpServletRequest request) {
        int rs = 0;
        try {
            List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");
            MultipartFile multipartFile = null;
//            rs = fileService.upload(role);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.ok(rs);
    }

}
