package com.xiaozhi.common.exception;

import com.xiaozhi.common.web.AjaxResult;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

/**
 * @Author: Joey
 * @Date: 2025/2/28 下午2:02
 * @Description:
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ResponseBody
    @ExceptionHandler(MultipartException.class)
    public AjaxResult handleMaxUpload(MaxUploadSizeExceededException e) {
        String msg;
        if (e.getCause().getCause() instanceof FileSizeLimitExceededException) {
            log.info("文件大小超过限制");
            msg = "文件大小超过限制";
        } else if (e.getCause().getCause() instanceof SizeLimitExceededException) {
            log.info("文件大小超过限制");
            msg = "文件大小超过限制";
        } else {
            msg = "文件有误";
        }
        return AjaxResult.error(msg);

    }
}
