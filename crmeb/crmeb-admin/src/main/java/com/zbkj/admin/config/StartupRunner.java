package com.zbkj.admin.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.zbkj.common.config.CrmebConfig;
import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.service.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class StartupRunner implements CommandLineRunner {

    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private CrmebConfig crmebConfig;

    @Value("${template-store.startup-report.enabled:false}")
    private Boolean startupReportEnabled;

    @Value("${template-store.startup-report.url:}")
    private String startupReportUrl;


    @Override
    public void run(String... args) throws Exception {
        // 项目启动后立即执行的代码
        System.out.println("项目启动完成，开始执行初始化任务...");
        if (Boolean.TRUE.equals(startupReportEnabled)) {
            // 异步执行，不阻塞启动
            CompletableFuture.runAsync(this::installStatistics);
        }
        System.out.println("初始化任务执行结束...");
    }

    public void installStatistics() {
        try {
            String version = crmebConfig.getVersion();
            if (StrUtil.isBlank(version) ) {
                version = "Template-Store-v1.0";
            }
            String apiUrl = systemConfigService.getValueByKey(SysConfigConstants.CONFIG_KEY_API_URL);
            if (StrUtil.isBlank(apiUrl) || !(StrUtil.startWithIgnoreCase(apiUrl, "http"))) {
                return;
            }
            if (StrUtil.isBlank(startupReportUrl) || !(StrUtil.startWithIgnoreCase(startupReportUrl, "http"))) {
                return;
            }
            Map<String, String> map = new HashMap<>();
            map.put("host", apiUrl);
            map.put("version", version);
            map.put("https", "https");
            String result = HttpUtil.post(startupReportUrl, JSONObject.toJSONString(map));

        } catch (Exception e) {
            // 异步调用不应影响主流程
            e.printStackTrace();
        }
    }
}
