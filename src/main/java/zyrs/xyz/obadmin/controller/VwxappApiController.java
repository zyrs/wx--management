package zyrs.xyz.obadmin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import com.sun.tools.javac.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;
import zyrs.xyz.obadmin.bean.*;
import zyrs.xyz.obadmin.service.ObService;
import zyrs.xyz.obadmin.service.VwxappService;
import zyrs.xyz.obadmin.service.WeixinService;
import zyrs.xyz.obadmin.service.WxappService;
import zyrs.xyz.obadmin.utils.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Created by Administrator on 2019/3/1.
 */
@RestController
@RequestMapping("/api/v")
public class VwxappApiController {

    @Autowired
    private WxappService wxappService;
    @Autowired
    private VwxappService vwxappService;
    @Autowired
    private WeixinService weixinService;

    /**
     * 用户授权登录,
     * @param wxappMember 基本信息
     * @param code 用户登录凭证
     * @return
     */
    @RequestMapping("login")
    public WxappMember login(WxappMember wxappMember,@RequestParam("code")String code,@RequestParam("id")Integer id){

        Wxapp wxapp = wxappService.getWxappInfoByObId(id);

        WxappResult wxappResult = WxappApiUtil.authLogin(wxapp.getAppid(),wxapp.getSecret(),code);

        wxappMember.setOid(id);
        wxappMember.setOpenid(wxappResult.getOpenid());
        //根据openID 更新或者插入数据 并返回 数据

        WxappMember member = wxappService.insertOrUpdateMemberAndReturnData(wxappMember);


        return member;
    }

    /**
     * 返回用户的微信公众号openid
     * @param id 用户id
     * @return
     */
    @RequestMapping("get_user_wxopenid_by_id")
    public String getUserWxopenidById(@RequestParam("id")Integer id){

        String member = wxappService.getUserWxopenidById(id);

        return member;
    }

    /**
     * 返回用户信息
     * @param id 用户id
     * @return
     */
    @RequestMapping("get_userinfo")
    public WxappMember getUserInfoById(@RequestParam("id")Integer id){

        WxappMember member = vwxappService.getUserInfoById(id);

        return member;
    }

    /**
     * 上传图片
     * @param id  用户id
     * @param file  文件
     * @return  文件地址
     */
    @RequestMapping("upload_picture")
    public String uploadPicture(@RequestParam("id")Integer id,@RequestParam("file") MultipartFile file){

        String res =  AliyunOss.upload_picture("wxapp/v/"+id,file);
        return res;
    }

    /**
     * 更新用户信息
     * 真实姓名 生日 联系方式  +》医院  教育  领域  （医生）
     * 通过用户id
     * @param wxappMember
     * @return
     */
    @RequestMapping("modify_userinfo")
    public int modifyUserInfo(WxappMember wxappMember){
       try{
           vwxappService.modifyUserInfo(wxappMember);
       }catch (Exception e){
           System.out.println(e);
           return 500;
       }

        return 0;
    }

    @RequestMapping("get_user_bank")
    public WxappBank getUserBank(@RequestParam("wxopenid")String wxopenid,@RequestParam("oid")Integer oid){
        return   wxappService.getUserBank(wxopenid,oid);
    }

    @RequestMapping("modify_user_bank")
    public int modifyUserBank(WxappBank wxappBank){
        try{
            //查询此用户绑定 id
            wxappService.modifyUserBank(wxappBank);
        }catch (Exception e){
            e.printStackTrace();
            return 500;
        }
        return 0;
    }

    /**
     * 统一下单接口 _次数用公众号信息支付 所以 appid也是公众号的
     * @param oid 项目id
     * @param request 请求信息 包含  wxopenid  amount 等
     * @return
     */
    @RequestMapping("/create_unified_order")
    public String createUnifiedOrder(@RequestParam("oid")Integer oid,HttpServletRequest request){

        try{
          //1.通过项目id获取项目的商户信息_公众号信息
            Wxapp wxapp = wxappService.getWxappInfoByObId(oid);
            WxappMerchant wxappMerchant = wxappService.getWxappMerchantInfo(oid);
//           //没有开通微信支付_设置一个开通了的_小程序已经没问题了，公众号未测试 暂时先这样
//            wxapp.setGzappid("wx8c45fc78f9bcdb6c");
//            wxappMerchant.setMchid("1494443872");


            String result = WeixinApiUtil.unifiedOrder(wxapp,wxappMerchant,request,"http://localhost/api/v/payCallback",false);
        }catch (Exception e){
            e.printStackTrace();
            return e.getMessage();
        }

        return "";
    }
    /**
     * 统一下单接口 _回调
     * @return
     */
    @RequestMapping("/pay_call_back")
    public void payCallback(HttpServletRequest request){
        System.out.println("下单回调:"+request);
    }


    /**
     * 创建咨询订单
     * @return  结果  success   "错误信息"
     */
    @RequestMapping("/create_consult_order")
    public String createConsultOrder(VmemberConsult vmemberConsult){
        System.out.println(vmemberConsult);
        String res ="";
        try{
           res =  vwxappService.createConsultOrder(vmemberConsult);
        }catch(Exception e){
            e.printStackTrace();
           res =  e.getMessage();
        }

        if(StringUtils.indexOfIgnoreCase(res,"成功") >= 0){
            //有生意了 通知管理者微信
            //购买成功通知 同时通知 管理者 和 购买用户

            //数据库获取模板id_管理者id_url   BY 项目id和模板id(自己制定不可变 为1)
            WeixinTemplate weixinTemplate = weixinService.getBuySuccessTemplateById("quanvjk_buy_success");

            if(weixinTemplate == null){
                return "订单生成成功，但消息通知失败，原因:服务器模板ID不存在，请联系客服检查是否配置错误！";
            }

           //填充数据
            WeixinTemplateValue t1 = new WeixinTemplateValue("全V健康咨询服务，金额:￥"+vmemberConsult.getCost(),"#ff0000");
            WeixinTemplateValue t2 = new WeixinTemplateValue("咨询内容:["+vmemberConsult.getLabel()+"]"+vmemberConsult.getTitle(),"#159bf4");

           //生成data_每个模板的参数都不一样，自动化转换暂时没想到好的方式这里先手动拼接
            StringBuffer str = new StringBuffer();
            str.append("{");
            str.append("\"name\":"+t1.toJson()+",");
            str.append("\"remark\":"+t2.toJson());
            str.append("}");

            weixinTemplate.setData(str.toString());
            //发送消息
            //获取公众号程序的appid srcret
            Wxapp wxapp = wxappService.getWxappInfoByObId(vmemberConsult.getOid());

            //发送给用户
            weixinTemplate.setTouser(vmemberConsult.getPatientWxopenid());
            WeixinResult weixinResult = WeixinApiUtil.sendTemplateMessage(weixinTemplate,wxapp);

            //发送失败 提示...
            if(weixinResult.getErrcode() != 0){
                return "订单生成成功，但消息通知失败，原因:"+weixinResult.getErrmsg()+"，请联系客服检查是否配置错误！";
            }
            /**
             * 发送给管理者
             */
           //获取咨询用户姓名 手机号
            WxappMember wxappMember = wxappService.getMemberBaseInfoByWxopenidAndOid(vmemberConsult.getPatientWxopenid(),vmemberConsult.getOid());

            WeixinTemplate weixinTemplate1 = weixinService.getBuySuccessTemplateById("quanvjk_consult_inform");
            //填充数据
            WeixinTemplateValue t11 = new WeixinTemplateValue("有客户提交咨询订单了,金额 ￥"+vmemberConsult.getCost(),"#f35937");
            WeixinTemplateValue t22 = new WeixinTemplateValue(wxappMember.getRealname()+"   "+wxappMember.getContact(),"#777777");
            WeixinTemplateValue t33 = new WeixinTemplateValue(CalculateUtil.getCurrentDate("yyyy-MM-dd aa hh:mm"),"#777777");
            WeixinTemplateValue t44 = new WeixinTemplateValue("["+vmemberConsult.getLabel()+"]=>["+vmemberConsult.getTitle()+"]","#159bf4");
            WeixinTemplateValue t55 = new WeixinTemplateValue("全V健康","#ff0000");

            //生成data_每个模板的参数都不一样，自动化转换暂时没想到好的方式这里先手动拼接
            StringBuffer str1 = new StringBuffer();
            str1.append("{");
            str1.append("\"first\":"+t11.toJson()+",");
            str1.append("\"keyword1\":"+t22.toJson()+",");
            str1.append("\"keyword2\":"+t33.toJson()+",");
            str1.append("\"keyword3\":"+t44.toJson()+",");
            str1.append("\"remark\":"+t55.toJson());

            str1.append("}");

            weixinTemplate1.setData(str1.toString());
            weixinTemplate1.setTouser(weixinTemplate1.getAdminOpenid());

            WeixinResult weixinResult1 = WeixinApiUtil.sendTemplateMessage(weixinTemplate1,wxapp);

            System.out.println("2购买成功模板消息发送结果:"+weixinResult1);
        }


        return res;
    }

    @RequestMapping("get_consult_list")
    public List<VmemberConsult> getConsultList(@RequestParam("wxopenid")String wxopenid,@RequestParam("identity")Integer identity,@RequestParam("oid")Integer oid){

        List<VmemberConsult> vmemberConsults = null;

        if(identity == 2){
           //患者 查询咨询订单__状态_医生信息_医生_最后一条消息_回复时间 or 接单时间
           vmemberConsults = vwxappService.getPatientConsultList(wxopenid,oid);

        }else{

        }

        return vmemberConsults;
    }
}
