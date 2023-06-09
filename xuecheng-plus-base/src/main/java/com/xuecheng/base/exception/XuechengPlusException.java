package com.xuecheng.base.exception;

public class XuechengPlusException extends RuntimeException{
    private String errMessage;

    public XuechengPlusException() {
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }

    public XuechengPlusException(String message) {
        super(message);
        this.errMessage = message;
    }
    public static void cast(String message){
        throw new XuechengPlusException(message);
    }
    public static void cast(CommonError error){
        throw new XuechengPlusException(error.getErrMessage());
    }
}
