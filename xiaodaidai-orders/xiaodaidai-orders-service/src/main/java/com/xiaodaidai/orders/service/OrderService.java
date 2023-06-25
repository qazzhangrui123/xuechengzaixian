package com.xiaodaidai.orders.service;

import com.xiaodaidai.messagesdk.model.po.MqMessage;
import com.xiaodaidai.orders.model.dto.AddOrderDto;
import com.xiaodaidai.orders.model.dto.PayRecordDto;
import com.xiaodaidai.orders.model.dto.PayStatusDto;
import com.xiaodaidai.orders.model.po.XcPayRecord;

public interface OrderService {

    /**
     * @description 创建商品订单
     * @param addOrderDto 订单信息
     * @return PayRecordDto 支付交易记录(包括二维码)
     * @author Mr.M
     * @date 2022/10/4 11:02
     */
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto);

    public XcPayRecord getPayRecordByPayno(String payNo);

    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付记录id
     * @return 支付记录信息
     */
    public PayRecordDto queryPayResult(String payNo);

    public void saveAliPayStatus(PayStatusDto payStatusDto);

    /**
     * 发送通知结果
     * @param message
     */
    public void notifyPayResult(MqMessage message);
}
