package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@Slf4j
@ControllerAdvice
//@RestControllerAdvice
public class GlobalExceptionHandler {

    //对项目的自定义异常进行处理
    @ResponseBody
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse customException(Exception e){
        //记录异常
        log.error("系统异常{}",e.getMessage(),e);
        if (e.getMessage().equals("不允许访问")){
            return new RestErrorResponse("没有操作此功能的权限");
        }
        //解析出异常信息
        String errMessage = e.getMessage();
        RestErrorResponse restErrorResponse = new RestErrorResponse(errMessage);
        return restErrorResponse;
    }

    //MethodArgumentNotValidException
    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse methodArgumentNotValidException(MethodArgumentNotValidException e){
        BindingResult bindingResult = e.getBindingResult();
        ArrayList<String> errors = new ArrayList<>();
        bindingResult.getFieldErrors().stream().forEach(item->{
            errors.add(item.getDefaultMessage());
        });
        //将list中的错误信息拼接起来
        String errMessage = StringUtils.join(errors, ",");
        //记录异常
        log.error("系统异常{}",e.getMessage(),errMessage);
        //解析出异常信息
        RestErrorResponse restErrorResponse = new RestErrorResponse(errMessage);
        return restErrorResponse;
    }

}
