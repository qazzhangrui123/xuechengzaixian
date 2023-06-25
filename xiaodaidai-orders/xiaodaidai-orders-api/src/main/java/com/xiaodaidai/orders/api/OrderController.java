package com.xiaodaidai.orders.api;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.xiaodaidai.base.exception.XuechengPlusException;
import com.xiaodaidai.orders.config.AlipayConfig;
import com.xiaodaidai.orders.model.dto.AddOrderDto;
import com.xiaodaidai.orders.model.dto.PayRecordDto;
import com.xiaodaidai.orders.model.dto.PayStatusDto;
import com.xiaodaidai.orders.model.po.XcPayRecord;
import com.xiaodaidai.orders.service.OrderService;
import com.xiaodaidai.orders.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Api(value = "订单支付接口", tags = "订单支付接口")
@Slf4j
@Controller
public class OrderController {

    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;

    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;
    @Autowired
    OrderService orderService;

    @ApiOperation("生成支付二维码")
    @PostMapping("/generatepaycode")
    @ResponseBody
    public PayRecordDto generatePayCode(@RequestBody AddOrderDto addOrderDto) {
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        String userId = user.getId();
        //调用service，完成插入订单信息、插入支付记录、生成支付二维码
        PayRecordDto order = orderService.createOrder(userId, addOrderDto);
        return order;
    }

    @ApiOperation("扫码下单接口")
    @GetMapping("/requestpay")
    public void requestpay(String payNo, HttpServletResponse httpResponse) throws Exception {
        //传入支付记录号，判断支付记录号是否存在
        XcPayRecord xcPayRecord = orderService.getPayRecordByPayno(payNo);
        if (xcPayRecord==null){
            XuechengPlusException.cast("支付记录不存在");
        }
        //支付结果
        String status = xcPayRecord.getStatus();
        if (status.equals("601002")){
            XuechengPlusException.cast("已支付，无需重复支付");
        }
        //请求支付宝下单
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, AlipayConfig.FORMAT, AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY,AlipayConfig.SIGNTYPE);
        //获得初始化的AlipayClient
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();//创建API对应的request
//        alipayRequest.setReturnUrl("http://domain.com/CallBack/return_url.jsp");
        alipayRequest.setNotifyUrl("http://tjxt-user-t.itheima.net/xuecheng/orders/paynotify");//在公共参数中设置回跳和通知地址
        alipayRequest.setBizContent("{" +
                "    \"out_trade_no\":\""+payNo+"\"," +
                "    \"total_amount\":" +xcPayRecord.getTotalPrice()+ ","+
                "    \"subject\":\""+xcPayRecord.getOrderName()+"\"," +
                "    \"product_code\":\"QUICK_WAP_WAY\"" +
                "  }");//填充业务参数
        String form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        httpResponse.setContentType("text/html;charset=" + AlipayConfig.CHARSET);
        httpResponse.getWriter().write(form);//直接将完整的表单html输出到页面
        httpResponse.getWriter().flush();

    }

    @ApiOperation("查询支付结果")
    @GetMapping("/payresult")
    @ResponseBody
    public PayRecordDto payresult(String payNo) throws IOException {
        //查询支付结果
        PayRecordDto payRecordDto = orderService.queryPayResult(payNo);
        return payRecordDto;

    }

    @ApiOperation("接收支付结果通知")
    @PostMapping("/receivenotify")
    public void receivenotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        //获取支付宝POST过来反馈信息
        Map<String, String> params = new HashMap<String, String>();
        Map requestParams = request.getParameterMap();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
            //计算得出通知验证结果
            //boolean AlipaySignature.rsaCheckV1(Map<String, String> params, String publicKey, String charset, String sign_type)
            boolean verify_result = AlipaySignature.rsaCheckV1(params, ALIPAY_PUBLIC_KEY, AlipayConfig.CHARSET, "RSA2");

            if (verify_result) {//验证成功
                String out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"), "UTF-8");
                //支付宝交易号
                String trade_no = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"), "UTF-8");
                //交易状态
                String trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"), "UTF-8");

                //交易金额
                String total_amount = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"), "UTF-8");

                if (trade_status.equals("TRADE_SUCCESS")) {
                    //更新支付记录表的状态为成功，订单表的状态为成功
                    PayStatusDto payStatusDto = new PayStatusDto();
                    payStatusDto.setTrade_status(trade_status);
                    payStatusDto.setOut_trade_no(out_trade_no);
                    payStatusDto.setTrade_no(trade_no);
                    payStatusDto.setTotal_amount(total_amount);
                    payStatusDto.setApp_id(APP_ID);
                    orderService.saveAliPayStatus(payStatusDto);

                }

                //——请根据您的业务逻辑来编写程序（以上代码仅作参考）——
                response.getWriter().write("success");//请不要修改或删除

            } else {//验证失败
                response.getWriter().write("fail");
            }
        }
    }


}
