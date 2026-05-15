
package com.md.poseidon.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.md.poseidon.common.utils.DateUtils;
import com.md.poseidon.common.utils.HttpResult;
import com.md.poseidon.common.utils.HttpUtils;
import com.md.poseidon.entity.CfgRobotAiMerchGczoomEntity;
import com.md.poseidon.entity.InfoServiceErrorEntity;
import com.md.poseidon.entity.SysDictDetailEntity;
import com.md.poseidon.entity.fourteenth.CfgPlatformEntity;
import com.md.poseidon.service.InfoServiceErrorService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.md.poseidon.controller.fourteenth.BaseController;
import com.md.poseidon.service.RedisService;
import com.md.poseidon.service.SysDictService;
import com.md.poseidon.vo.ErrorAggregationDTO;
import com.md.poseidon.vo.ServiceErrorReportReq;
import feign.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.ui.Model;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.md.poseidon.common.utils.DataResult;

import javax.servlet.http.HttpServletRequest;

/**
 * 游戏服务异常信息表
 *
 * @author zwq
 * @date 2025-06-06 14:36:57
 */
@Slf4j
@Controller
@RequestMapping("/")
public class InfoServiceErrorController extends BaseController {
    @Autowired
    private InfoServiceErrorService infoServiceErrorService;
    @Autowired
    private RedisService redisService;
    @Value("${web.env:未知}")
    public String env;
    @Autowired
    private SysDictService sysDictService;

    private static final String LARK_NOTIFY_CONF_KEY = "LARK_NOTIFY_CONF";
    private static final String LARK_NOTIFY_WEBHOOK_KEY = "LARK_NOTIFY_WEBHOOK";
    private static final String LARK_NOTIFY_MINUTES_KEY = "LARK_NOTIFY_MINUTES";
    private static final String LARK_NOTIFY_PROXY_KEY = "LARK_NOTIFY_PROXY";
    private static final String LARK_NOTIFY_HIGHLIGHT_COUNT_KEY = "LARK_NOTIFY_HIGHLIGHT_COUNT";
    private static final String LARK_NOTIFY_LOCK_KEY = "LARK_NOTIFY_LOCK_KEY";


    /**
    * 跳转到页面
    */
    @GetMapping("index/infoServiceError")
    public String infoServiceError(Model model) {
        List<CfgPlatformEntity> platform = getUserPlatformChannelList(true, false);
        model.addAttribute("platform", platform);
        model.addAttribute("TIPS_MAP", getTipsMap());
        return "infoserviceerror/list";
    }

    //@ApiOperation(value = "新增")
    @PostMapping("infoServiceError/add")
    @RequiresPermissions("infoServiceError:add")
    @ResponseBody
    public DataResult add(@RequestBody InfoServiceErrorEntity infoServiceError){
        infoServiceErrorService.save(infoServiceError);
        tableChange("info_service_error");
        return DataResult.success();
    }

    @PostMapping("infoServiceError/report")
    @ResponseBody
    public DataResult report(@RequestBody ServiceErrorReportReq req, HttpServletRequest request){
        // 获取客户端IP
        String clientIp = getClientIp(request);
        if (!isPrivateNetworkIp(clientIp)) {
            log.warn("服务错误上报接口非法调用->ip:{}  req->{}",  clientIp, req);
            return DataResult.fail("API不存在");
        }

        InfoServiceErrorEntity infoServiceError = new InfoServiceErrorEntity();
        infoServiceError.setPlatformid(req.getPlatformid());
        infoServiceError.setChannel(req.getChannel());
        infoServiceError.setUserid(req.getUserid());  
        infoServiceError.setLevel(req.getLevel());
        infoServiceError.setServicename(req.getServicename());
        infoServiceError.setModulename(req.getModulename());
        infoServiceError.setResourceid(req.getResourceid());
        infoServiceError.setErrorcode(req.getErrorcode());
        infoServiceError.setShortdesc(req.getShortdesc());
        infoServiceError.setContent(req.getContent());
        infoServiceError.setReporttime(req.getReporttime());
        infoServiceError.setNotifystatus(0);
        infoServiceError.setServiceip(clientIp);

        infoServiceErrorService.save(infoServiceError);
        return DataResult.success();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }

    private boolean isPrivateNetworkIp(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            if (inetAddress.isSiteLocalAddress() || inetAddress.isLoopbackAddress()) {
                return true;
            }

            // 检查 IPv4 私有地址范围
            if (ip.startsWith("10.")) {
                return true; // 10.0.0.0/8
            }
            if (ip.startsWith("192.168.")) {
                return true; // 192.168.0.0/16
            }
            if (ip.startsWith("172.")) {
                // 提取第二段数字
                String[] octets = ip.split("\\.");
                if (octets.length >= 2) {
                    try {
                        int secondOctet = Integer.parseInt(octets[1]);
                        // 检查 172.16.0.0/12 范围 (16-31)
                        return secondOctet >= 16 && secondOctet <= 31;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            }
            return false;
        } catch (UnknownHostException e) {
            return false;
        }
    }
    //@ApiOperation(value = "删除")
    @DeleteMapping("infoServiceError/delete")
    @RequiresPermissions("infoServiceError:delete")
    @ResponseBody
    public DataResult delete(@RequestBody /*@ApiParam(value = "id集合")*/ List<String> ids){
        infoServiceErrorService.removeByIds(ids);
        tableChange("info_service_error");
        return DataResult.success();
    }

    //@ApiOperation(value = "更新")
    @PutMapping("infoServiceError/update")
    @RequiresPermissions("infoServiceError:update")
    @ResponseBody
    public DataResult update(@RequestBody InfoServiceErrorEntity infoServiceError){
        infoServiceErrorService.updateById(infoServiceError);
        tableChange("info_service_error");
        return DataResult.success();
    }

    //@ApiOperation(value = "查询分页数据")
    @PostMapping("infoServiceError/listByPage")
    @RequiresPermissions("infoServiceError:list")
    @ResponseBody
    public DataResult findListByPage(@RequestBody InfoServiceErrorEntity infoServiceError){
        LambdaQueryWrapper<InfoServiceErrorEntity> queryWrapper = Wrappers.lambdaQuery();
        if (infoServiceError.getPlatformid() != null && infoServiceError.getPlatformid() != 0) {
            queryWrapper.eq(InfoServiceErrorEntity::getPlatformid, infoServiceError.getPlatformid());
        }
        if (infoServiceError.getChannel() != null && !infoServiceError.getChannel().isEmpty()) {
            queryWrapper.eq(InfoServiceErrorEntity::getChannel, infoServiceError.getChannel());
        }
        if (infoServiceError.getUserid() != null ) {
            queryWrapper.eq(InfoServiceErrorEntity::getUserid, infoServiceError.getUserid());
        }
        if (infoServiceError.getLevel() != null) {
            queryWrapper.eq(InfoServiceErrorEntity::getLevel, infoServiceError.getLevel());
        }
        if (infoServiceError.getServicename() != null && !infoServiceError.getServicename().isEmpty()) {
            queryWrapper.eq(InfoServiceErrorEntity::getServicename, infoServiceError.getServicename());
        }
        if (infoServiceError.getModulename() != null && !infoServiceError.getModulename().isEmpty()) {
            queryWrapper.eq(InfoServiceErrorEntity::getModulename, infoServiceError.getModulename());
        }
        if (infoServiceError.getErrorcode() != null && !infoServiceError.getErrorcode().isEmpty()) {
            queryWrapper.eq(InfoServiceErrorEntity::getErrorcode, infoServiceError.getErrorcode());
        }
        if (infoServiceError.getNotifystatus() != null) {
            queryWrapper.eq(InfoServiceErrorEntity::getNotifystatus, infoServiceError.getNotifystatus());
        }
        if (infoServiceError.getStart() != null) {
            queryWrapper.ge(InfoServiceErrorEntity::getReporttime, infoServiceError.getStart());
        }
        if (infoServiceError.getEnd() != null) {
            queryWrapper.le(InfoServiceErrorEntity::getReporttime, infoServiceError.getEnd());
        }
        queryWrapper.orderByDesc(InfoServiceErrorEntity::getId);
        IPage<InfoServiceErrorEntity> iPage = infoServiceErrorService.page(infoServiceError.getQueryPage(), queryWrapper);
        return DataResult.success(iPage);
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void notifyError() throws Exception {
        List<SysDictDetailEntity> dictList = sysDictService.getTypeList(LARK_NOTIFY_CONF_KEY);
        if (dictList == null || dictList.isEmpty()){
            //没有配置
           return;
        }
        String webhook = "";
        String proxy = "";
        int minutes = 1;
        //达到alertcount条错误，加红灯
        int highlightcount = 100;
        for (SysDictDetailEntity dict : dictList){
            if (dict.getLabel().equals(LARK_NOTIFY_WEBHOOK_KEY)){
                webhook = dict.getValue();
            } else if (dict.getLabel().equals(LARK_NOTIFY_MINUTES_KEY)){
                try {
                    minutes = Integer.parseInt(dict.getValue());
                }catch (Exception e){
                    logger.error("LARK_NOTIFY_MINUTES配置错误");
                }
            } else if (dict.getLabel().equals(LARK_NOTIFY_HIGHLIGHT_COUNT_KEY)){
                try {
                    highlightcount = Integer.parseInt(dict.getValue());
                }catch (Exception e){
                    logger.error("LARK_NOTIFY_HIGHLIGHT_COUNT配置错误");
                }
            }else if (dict.getLabel().equals(LARK_NOTIFY_PROXY_KEY)){
                proxy = dict.getValue();
                if (proxy.endsWith("/")){
                    proxy = proxy.substring(0,proxy.length()-1);
                }
                if (proxy.equalsIgnoreCase("none")){
                    proxy = "";
                }
            }
        }
        if (StringUtils.isBlank(webhook)){
            return;
        }
        if (minutes<1){
            return;
        }
        //redis加检测，避免任务同时执行抢占游戏服务器和数据库资源
        boolean locked = redisService.lock(LARK_NOTIFY_LOCK_KEY,minutes * 60L-5);
        if (!locked) {
            return;
        }

        String msgContent = generateNotification(DateUtils.addMinutes(new Date(), -minutes),highlightcount);
        if (StringUtils.isBlank(msgContent)){
            return;
        }
        JSONObject msgBody = new JSONObject();

        msgBody.put("msg_type", "text");
        JSONObject text = new JSONObject();
        text.put("text", msgContent);
        msgBody.put("content", text);
        logger.info("发送LARK消息：{}", msgBody.toJSONString());
        HttpResult resp = HttpUtils.postJsonWithProxy(webhook, msgBody.toJSONString(),null,proxy);
        logger.info("LARK消息发送结果：{}", resp.getBody());
    }

        // 最大保留详情条数（避免通知过长）
        private static final int MAX_DETAIL_COUNT = 3;

        /**
         * 核心聚合方法：按「错误码+短描述+业务key」分组聚合
         * @param errorList 原始错误日志列表
         * @return 聚合后的错误列表
         */
        private List<ErrorAggregationDTO> aggregateErrors(List<InfoServiceErrorEntity> errorList) {
            // 聚合Map：key=错误码+短描述+业务key，value=聚合DTO
            Map<String, ErrorAggregationDTO> aggregationMap = new HashMap<>();

            for (InfoServiceErrorEntity error : errorList) {
                // 1. 提取核心聚合维度
                String errorCode = error.getErrorcode();
                String shortDesc = error.getShortdesc();
                String serviceName = error.getServicename();
                String moduleName = error.getModulename();
                // 2. 提取业务key（核心：根据不同错误类型解析content）
                String businessKey = extractBusinessKey(shortDesc, error.getContent(),error.getResourceid());
                // 3. 生成聚合Map的key（唯一标识一组相同错误）
                String mapKey = String.join("|", errorCode, businessKey);

                // 4. 初始化/更新聚合DTO
                ErrorAggregationDTO dto = aggregationMap.getOrDefault(mapKey, new ErrorAggregationDTO());
                if (dto.getErrorCode() == null) {
                    // 首次初始化
                    dto.setErrorCode(errorCode);
                    dto.setShortDesc(shortDesc);
                    dto.setServiceName(serviceName);
                    dto.setModuleName(moduleName);
                    dto.setBusinessKey(businessKey);
                    dto.setFirstOccurTime(error.getReporttime());
                    dto.setLastOccurTime(error.getReporttime());
                } else {
                    // 更新统计信息
                    dto.setErrorCount(dto.getErrorCount() + 1);
                    // 更新首次/末次时间
                    if (error.getReporttime().getTime()<dto.getFirstOccurTime().getTime()){
                        dto.setFirstOccurTime(error.getReporttime());
                    }
                    if (error.getReporttime().getTime()>dto.getLastOccurTime().getTime()){
                        dto.setLastOccurTime(error.getReporttime());
                    }
                }
                // 添加详情（最多保留3条）
                if (dto.getErrorDetails().size() < MAX_DETAIL_COUNT) {
                    dto.addErrorDetail(error.getContent());
                }

                // 放回Map
                aggregationMap.put(mapKey, dto);
            }

            // 转换为列表并按错误次数降序排序
            return aggregationMap.values().stream()
                    .sorted((d1, d2) -> Integer.compare(d2.getErrorCount(), d1.getErrorCount()))
                    .collect(Collectors.toList());
        }

        /**
         * 提取业务key：根据短描述解析content中的核心业务标识
         * @param shortDesc 错误简短描述
         * @param content 错误详情
         * @return 业务key（唯一标识业务场景）
         */
        private String extractBusinessKey(String shortDesc, String content,String resourceid) {
            if (shortDesc == null || content == null) {
                return "未知业务场景";
            }

            try {
                // 场景1：短信发送失败
                if (shortDesc.contains("send sms fail")) {
                    // content格式：验证类型|用户|手机号|其他 → 提取前3段作为业务key
                    String[] parts = content.split("\\|");
                    if (parts.length >= 3) {
                        return String.join("|", parts[0], parts[1], parts[2]);
                    } else {
                        return "短信发送失败|未知参数";
                    }
                }
                // 场景2：提现响应异常
                else if (shortDesc.contains("withdraw resp status")) {
                    return "提现异常|"+shortDesc.substring(0, Math.min(20, shortDesc.length()));
                }
                // 场景3：设备锁定
                else if (shortDesc.contains("设备被锁定")) {
                    // content格式：设备(xxx)被锁定 → 提取设备ID
                    String deviceId = content.substring(content.indexOf("(") + 1, content.indexOf(")"));
                    return "设备锁定|" + deviceId;
                }
                // 场景4：IP段锁定
                else if (shortDesc.contains("IP段被锁定")) {
                    // content格式：IP段(xxx)被锁定 → 提取IP
                    String ip = content.substring(content.indexOf("(") + 1, content.indexOf(")"));
                    return "IP段锁定|" + ip;
                }
                // 场景5：邮件发送失败
                else if (shortDesc.contains("send email fail")) {
                    // content格式：注册账号|系统邮箱|邮箱地址|其他 → 提取邮箱
                    String[] parts = content.split("\\|");
                    if (parts.length >= 3) {
                        return "邮件发送失败|" + parts[2];
                    } else {
                        return "邮件发送失败|未知邮箱";
                    }
                }
                // 其他场景
                else {
                    return resourceid +"|" + shortDesc.substring(0, Math.min(20, shortDesc.length()));
                }
            } catch (Exception e) {
                // 解析失败时返回默认值，避免程序中断
                return resourceid +"|" + shortDesc.substring(0, Math.min(20, shortDesc.length()));
            }
        }

        private String generateNotification(Date start,int highlightcount) {
            LambdaQueryWrapper<InfoServiceErrorEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.ge(InfoServiceErrorEntity::getReporttime, start);
            List<InfoServiceErrorEntity> errorList = infoServiceErrorService.list(queryWrapper);
            List<ErrorAggregationDTO> aggregationList = aggregateErrors(errorList);
            if (aggregationList.isEmpty()) {
                return "";
            }
            StringBuilder notification = new StringBuilder();
            notification.append("【"+env);
            notification.append(" - 错误告警汇总 - ").append(new Date()).append("】\n");
            notification.append("=========================================\n");

            for (int i = 0; i < aggregationList.size(); i++) {
                ErrorAggregationDTO dto = aggregationList.get(i);
                String highlightStr = "\uD83D\uDEA8\uD83D\uDEA8\uD83D\uDEA8\uD83D\uDEA8\uD83D\uDEA8\uD83D\uDEA8\uD83D\uDEA8\uD83D\uDEA8\uD83D\uDEA8\uD83D\uDEA8";
                if (dto.getErrorCount()+1<highlightcount){
                    highlightStr = "";
                }
                notification.append(String.format(
                        "%d. 【%s(%s)】\n" +
                                "   发生次数：%d次%s\n" +
                                "   涉及服务：%s/%s\n" +
                                "   业务场景：%s\n" +
                                "   时间范围：%s ~ %s\n" +
                                "   典型详情：%s\n",
                        i + 1,
                        dto.getShortDesc(),
                        dto.getErrorCode(),
                        dto.getErrorCount() + 1, // 初始化时count从0开始，+1修正
                        highlightStr,
                        dto.getServiceName(),
                        dto.getModuleName(),
                        dto.getBusinessKey(),
                        dto.getFirstOccurTime(),
                        dto.getLastOccurTime(),
                        String.join("；", dto.getErrorDetails())
                ));
                notification.append("-----------------------------------------\n");
            }

            return notification.toString();
        }
}
