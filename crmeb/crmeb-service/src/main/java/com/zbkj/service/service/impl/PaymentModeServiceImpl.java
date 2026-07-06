package com.zbkj.service.service.impl;

import cn.hutool.core.util.StrUtil;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.constants.OfflinePayConstants;
import com.zbkj.common.constants.PaymentModeConstants;
import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.request.PaymentModeSwitchRequest;
import com.zbkj.common.response.PaymentModeResponse;
import com.zbkj.common.utils.PaymentModeUtil;
import com.zbkj.service.service.PaymentModeService;
import com.zbkj.service.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 支付模式服务实现
 */
@Service
public class PaymentModeServiceImpl implements PaymentModeService {

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public PaymentModeResponse getMode() {
        String mode = resolveCurrentMode();
        return buildResponse(mode);
    }

    @Override
    public PaymentModeResponse switchMode(PaymentModeSwitchRequest request) {
        String mode;
        try {
            mode = PaymentModeUtil.requireValidMode(request.getMode());
        } catch (IllegalArgumentException e) {
            throw new CrmebException(e.getMessage());
        }
        validateTargetMode(mode);

        Boolean execute = transactionTemplate.execute(e -> {
            systemConfigService.updateOrSaveValueByName(PaymentModeConstants.CONFIG_PAY_MODE, mode);
            systemConfigService.updateOrSaveValueByName(
                    OfflinePayConstants.CONFIG_OFFLINE_PAY_STATUS,
                    PaymentModeUtil.switchValue(PaymentModeUtil.isOfflineQrMode(mode))
            );
            systemConfigService.updateOrSaveValueByName(
                    SysConfigConstants.CONFIG_PAY_WEIXIN_OPEN,
                    PaymentModeUtil.switchValue(PaymentModeUtil.isWechatOnlineMode(mode))
            );
            return Boolean.TRUE;
        });
        if (!Boolean.TRUE.equals(execute)) {
            throw new CrmebException("支付模式切换失败");
        }
        return buildResponse(mode);
    }

    private String resolveCurrentMode() {
        String mode = systemConfigService.getValueByKey(PaymentModeConstants.CONFIG_PAY_MODE);
        String offlineStatus = systemConfigService.getValueByKey(OfflinePayConstants.CONFIG_OFFLINE_PAY_STATUS);
        String wechatStatus = systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_WEIXIN_OPEN);
        return PaymentModeUtil.resolveMode(mode, offlineStatus, wechatStatus);
    }

    private void validateTargetMode(String mode) {
        if (PaymentModeUtil.isOfflineQrMode(mode) && !hasOfflinePayConfig()) {
            throw new CrmebException("请先在支付配置中上传扫码转账收款码");
        }
        if (PaymentModeUtil.isWechatOnlineMode(mode) && !hasWechatPayConfig()) {
            throw new CrmebException("请先配置公众号或小程序微信支付参数");
        }
    }

    private PaymentModeResponse buildResponse(String mode) {
        PaymentModeResponse response = new PaymentModeResponse();
        response.setMode(mode);
        response.setModeName(PaymentModeUtil.modeName(mode));
        response.setOfflinePayOpen(PaymentModeUtil.isOfflineQrMode(mode));
        response.setWechatPayOpen(PaymentModeUtil.isWechatOnlineMode(mode));
        response.setOfflinePayReady(hasOfflinePayConfig());
        response.setWechatPayReady(hasWechatPayConfig());
        response.setWarnings(buildWarnings(response));
        return response;
    }

    private List<String> buildWarnings(PaymentModeResponse response) {
        List<String> warnings = new ArrayList<>();
        if (PaymentModeUtil.isOfflineQrMode(response.getMode()) && !response.getOfflinePayReady()) {
            warnings.add("扫码转账收款码未配置，用户无法看到可扫码的收款码。");
        }
        if (PaymentModeUtil.isWechatOnlineMode(response.getMode()) && !response.getWechatPayReady()) {
            warnings.add("微信支付参数未配置完整，用户无法发起微信在线支付。");
        }
        return warnings;
    }

    private Boolean hasOfflinePayConfig() {
        return StrUtil.isNotBlank(systemConfigService.getValueByKey(OfflinePayConstants.CONFIG_OFFLINE_PAY_QRCODE));
    }

    private Boolean hasWechatPayConfig() {
        return hasPublicWechatPayConfig() || hasRoutineWechatPayConfig() || hasAppWechatPayConfig();
    }

    private Boolean hasPublicWechatPayConfig() {
        return hasAllConfig(
                Constants.CONFIG_KEY_PAY_WE_CHAT_APP_ID,
                Constants.CONFIG_KEY_PAY_WE_CHAT_MCH_ID,
                Constants.CONFIG_KEY_PAY_WE_CHAT_APP_KEY
        );
    }

    private Boolean hasRoutineWechatPayConfig() {
        return hasAllConfig(
                Constants.CONFIG_KEY_PAY_ROUTINE_APP_ID,
                Constants.CONFIG_KEY_PAY_ROUTINE_MCH_ID,
                Constants.CONFIG_KEY_PAY_ROUTINE_APP_KEY
        );
    }

    private Boolean hasAppWechatPayConfig() {
        return hasAllConfig(
                Constants.CONFIG_KEY_PAY_WE_CHAT_APP_APP_ID,
                Constants.CONFIG_KEY_PAY_WE_CHAT_APP_MCH_ID,
                Constants.CONFIG_KEY_PAY_WE_CHAT_APP_APP_KEY
        );
    }

    private Boolean hasAllConfig(String... keys) {
        for (String key : keys) {
            if (StrUtil.isBlank(systemConfigService.getValueByKey(key))) {
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }
}
