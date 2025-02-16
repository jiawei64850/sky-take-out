package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RequestMapping("/admin/common")
@RestController
@Api(tags = "公共接口")
public class CommonController {
    @Autowired
    private AliOssUtil aliOssUtil;

    /**
     * 文件上传
     * @param file
     * @return
     */
    // use aliOss for file upload
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result upload(MultipartFile file) {

        String originalFilename = file.getOriginalFilename(); // get original name of file
        log.info("文件上传，原始文件名： {} ", originalFilename);
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")); // get the suffix of file

        // call the file upload method of tool class of AliOssUtil
        String url;
        try {
            String objectName = UUID.randomUUID().toString() + suffix;
            url = aliOssUtil.upload(file.getBytes(), objectName);
        } catch (IOException e) {
            log.info("文件上传失败！！！", e.getMessage());
            return Result.error(MessageConstant.UPLOAD_FAILED);

        }
        return Result.success(url);
    }
}
