package com.hmdp.utils;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author GuoShuo
 * @Time 2022/10/11 19:28
 * @Version 1.0
 * @Description
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "sms")
@Data
public class SendCodeUtils {
    private String SecretId;
    private String SecretKey;
    private String SdkAppId;
    private String TemplateId;
    private String SignName;
    public void SendCode(String phone , String code){
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
            // 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
            Credential cred = new Credential(SecretId , SecretKey);
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("sms.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            SmsClient client = new SmsClient(cred, "ap-nanjing", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            SendSmsRequest req = new SendSmsRequest();
            req.setSmsSdkAppId(SdkAppId);
            req.setTemplateId(TemplateId);
            req.setSignName(SignName);

            String[] phoneArr = {phone};
            req.setPhoneNumberSet(phoneArr);
            String[] codeArr = {code};
            req.setTemplateParamSet(codeArr);

            // 返回的resp是一个SendSmsResponse的实例，与请求对象对应
            SendSmsResponse resp = client.SendSms(req);
            // 输出json格式的字符串回包
            for (SendStatus sendStatus : resp.getSendStatusSet()) {
                if(sendStatus.getCode().equalsIgnoreCase("OK") &&
                sendStatus.getMessage().equalsIgnoreCase("send success")){
                    log.info("验证码发送成功 -> :{}", phone);
                }else{
                    throw new RuntimeException(sendStatus.getMessage());
                }
            }
        } catch (TencentCloudSDKException e) {
            throw new RuntimeException("验证码发送失败,原因:" + e.getMessage());
        }
    }
}
