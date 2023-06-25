package com.xiaodaidai.orders.service.Impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaodaidai.base.exception.XuechengPlusException;
import com.xiaodaidai.base.utils.IdWorkerUtils;
import com.xiaodaidai.base.utils.QRCodeUtil;
import com.xiaodaidai.messagesdk.model.po.MqMessage;
import com.xiaodaidai.messagesdk.service.MqMessageService;
import com.xiaodaidai.orders.config.AlipayConfig;
import com.xiaodaidai.orders.config.PayNotifyConfig;
import com.xiaodaidai.orders.mapper.XcOrdersGoodsMapper;
import com.xiaodaidai.orders.mapper.XcOrdersMapper;
import com.xiaodaidai.orders.mapper.XcPayRecordMapper;
import com.xiaodaidai.orders.model.dto.AddOrderDto;
import com.xiaodaidai.orders.model.dto.PayRecordDto;
import com.xiaodaidai.orders.model.dto.PayStatusDto;
import com.xiaodaidai.orders.model.po.XcOrders;
import com.xiaodaidai.orders.model.po.XcOrdersGoods;
import com.xiaodaidai.orders.model.po.XcPayRecord;
import com.xiaodaidai.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单相关
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    XcOrdersMapper xcOrdersMapper;

    @Autowired
    XcOrdersGoodsMapper xcOrdersGoodsMapper;

    @Autowired
    XcPayRecordMapper xcPayRecordMapper;

    @Value("${pay.qrcodeurl}")
    String qrcodeurl;

    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;

    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;

    @Autowired
    OrderServiceImpl currentProxy;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    MqMessageService mqMessageService;



    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        XcPayRecord xcPayRecord = xcPayRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
        return xcPayRecord;
    }

    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付记录id
     * @return 支付记录信息
     */
    @Override
    public PayRecordDto queryPayResult(String payNo) {
        //调用支付宝，查询支付结果
//        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);

        //测试用的，因为支付宝那边没法通过嘛
        XcPayRecord xcPayRecord = xcPayRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getOrderId, payNo));
        PayStatusDto payStatusDto = new PayStatusDto();
        BeanUtils.copyProperties(xcPayRecord,payStatusDto);


        //拿到支付结果更新订单表状态
        currentProxy.saveAliPayStatus(payStatusDto);
        //要返回最新的支付记录信息
        XcPayRecord payRecordByPayno = getPayRecordByPayno(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecordByPayno,payRecordDto);
        return payRecordDto;
    }

    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付交易号
     * @return 支付结果
     */
    public PayStatusDto queryPayResultFromAlipay(String payNo) {
//        XuechengPlusException.cast("没有支付宝商户帐号，支付不了啊哥哥～～");
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, "json", AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE); //获得初始化的AlipayClient
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        //bizContent.put("trade_no", "2014112611001004680073956707");
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        String body = null;
        try {
            response = alipayClient.execute(request);
            if (!response.isSuccess()){
                //交易不成功
                XuechengPlusException.cast("请求支付宝查询支付结果失败");
            }
            body = response.getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
            XuechengPlusException.cast("请求支付查询支付结果异常");
        }
        Map<String,String> bodymap = JSON.parseObject(body, Map.class);
        //解析支付结果
        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_no(bodymap.get("trade_no"));    //支付宝的交易号
        payStatusDto.setTrade_status(bodymap.get("trade_status"));
        payStatusDto.setApp_id(APP_ID);
        payStatusDto.setTotal_amount(bodymap.get("total_amount"));//总金额

        return payStatusDto;
    }

    /**
     * @description 保存支付宝支付结果
     * @param payStatusDto  支付结果信息
     * @return void
     * @author Mr.M
     * @date 2022/10/4 16:52
     */
    @Transactional
    public void saveAliPayStatus(PayStatusDto payStatusDto) {
        //支付记录号
        String out_trade_no = payStatusDto.getOut_trade_no();
        XcPayRecord payRecordByPayno = getPayRecordByPayno(out_trade_no);
        if (payRecordByPayno==null){
            XuechengPlusException.cast("找不到相关记录");
        }
        //拿到相关联的订单id
        Long orderId = payRecordByPayno.getOrderId();
        XcOrders xcOrders = xcOrdersMapper.selectById(orderId);
        if (xcOrders==null){
            XuechengPlusException.cast("找不到相关联的订单");
        }
        String statusFromDb = payRecordByPayno.getStatus();
        if (statusFromDb.equals("601002")){
            //如果数据库保存的状态已经成功
            return;
        }
        //如果支付成功
        String trade_status = payStatusDto.getTrade_status();
        if (trade_status.equals("TRADE_SUCCESS")){
            //更新支付记录表的状态
            payRecordByPayno.setStatus("601002");
            //支付宝的订单号
            payRecordByPayno.setOutPayNo(payStatusDto.getOut_trade_no());
            //第三方支付渠道编号
            payRecordByPayno.setOutPayChannel("Alipay");
            //支付成功时间
            payRecordByPayno.setPaySuccessTime(LocalDateTime.now());
            xcPayRecordMapper.updateById(payRecordByPayno);


            //更新订单表状态为成功
            //订单交易状态为交易成功
            xcOrders.setStatus("600002");
            xcOrdersMapper.updateById(xcOrders);


            //=====================================
            //将消息写道数据库
            MqMessage mqMessage = mqMessageService.addMessage("payresult_notify", xcOrders.getOutBusinessId(), xcOrders.getOrderType(), null);
            //发送消息
            notifyPayResult(mqMessage);
        }

    }

    @Override
    public void notifyPayResult(MqMessage message) {
        //消息内容
        String jsonString = JSON.toJSONString(message);
        //创建一个持久化消息
        Message messageObj = MessageBuilder.withBody(jsonString.getBytes(StandardCharsets.UTF_8)).setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();
        //消息id
        Long id = message.getId();
        //全局消息id
        CorrelationData correlationData = new CorrelationData();
        //使用CorrelationData指定回调方法
        correlationData.getFuture().addCallback(result->{
            if (result.isAck()){
                //消息成功发送到了交换机
                log.debug("发送消息成功：{}",jsonString);
                //将消息从数据库表删除
                mqMessageService.completed(id);
            }else {
                //消息发送失败
                log.debug("发送消息失败：{}",jsonString);
            }
        },ex->{
            //发生异常
            log.debug("发生异常：{}",jsonString);
        });
        //发送消息
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT,"",messageObj,correlationData);
    }

    @Override
    @Transactional
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {
        //插入订单表,订单表、订单明细表
        XcOrders xcOrders = saveXcOrders(userId, addOrderDto);

        //插入支付记录
        XcPayRecord payRecord = createPayRecord(xcOrders);
        Long payNo = payRecord.getPayNo();
        //生成二维码
        QRCodeUtil qrCodeUtil = new QRCodeUtil();
        //支付二维码的url
        String url = String.format(qrcodeurl, payNo);
        String qrCode = null;
        try {
            qrCode = qrCodeUtil.createQRCode(url, 200, 200);
        } catch (IOException e) {
            XuechengPlusException.cast("生成二维码出错");
        }
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord,payRecordDto);
        payRecordDto.setQrcode(qrCode);
        return payRecordDto;
    }

    /**
     * 保存支付记录
     * @param orders
     * @return
     */
    public XcPayRecord createPayRecord(XcOrders orders){
        if(orders==null){
            XuechengPlusException.cast("订单不存在");
        }
        if(orders.getStatus().equals("600002")){
            XuechengPlusException.cast("订单已支付");
        }
        XcPayRecord payRecord = new XcPayRecord();
        //生成支付交易流水号
        long payNo = IdWorkerUtils.getInstance().nextId();
        payRecord.setPayNo(payNo);
        payRecord.setOrderId(orders.getId());//商品订单号
        payRecord.setOrderName(orders.getOrderName());
        payRecord.setTotalPrice(orders.getTotalPrice());
        payRecord.setCurrency("CNY");
        payRecord.setCreateDate(LocalDateTime.now());
        payRecord.setStatus("601001");//未支付
        payRecord.setUserId(orders.getUserId());
        xcPayRecordMapper.insert(payRecord);
        return payRecord;
    }

    @Transactional
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto){
        //插入订单表,订单表、订单明细表
        XcOrders xcOrders = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if (xcOrders!=null){
            return xcOrders;
        }
        //插入订单主表
        xcOrders = new XcOrders();
        //使用雪花算法生成订单号
        xcOrders.setId(IdWorkerUtils.getInstance().nextId());
        xcOrders.setTotalPrice(addOrderDto.getTotalPrice());
        xcOrders.setCreateDate(LocalDateTime.now());
        xcOrders.setStatus("600001");  //未支付
        xcOrders.setUserId(userId);
        xcOrders.setOrderType("60201");//订单类型
        xcOrders.setOrderName(addOrderDto.getOrderName());
        xcOrders.setOrderDescrip(addOrderDto.getOrderDescrip());
        xcOrders.setOrderDetail(addOrderDto.getOrderDetail());
        xcOrders.setOutBusinessId(addOrderDto.getOutBusinessId());  //如果是选课这里记录选课表的id

        int insert = xcOrdersMapper.insert(xcOrders);
        if (insert<=0){
            XuechengPlusException.cast("添加订单失败");
        }
        Long orderId = xcOrders.getId();
        //插入订单明细表
        //将前端传入的明细json串转成list
        String orderDetailJson = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> xcOrdersGoods = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        //遍历xcOrdersGoods
        xcOrdersGoods.forEach(goods->{
            goods.setOrderId(orderId);
            int insert1 = xcOrdersGoodsMapper.insert(goods);
        });

        //进行幂等性判断，同一个选课记录只能由一个订单
        return xcOrders;
    }

    //根据业务id查询订单,业务id就是选课记录表中的主键
    public XcOrders getOrderByBusinessId(String businessId){
        XcOrders xcOrders = xcOrdersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId));
        return xcOrders;
    }
}
