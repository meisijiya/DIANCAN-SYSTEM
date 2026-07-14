package com.scaffold.modules.payment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.coupon.service.CouponService;
import com.scaffold.modules.order.entity.Order;
import com.scaffold.modules.order.entity.OrderItem;
import com.scaffold.modules.order.entity.OrderOperationLog;
import com.scaffold.modules.order.mapper.OrderItemMapper;
import com.scaffold.modules.order.mapper.OrderOperationLogMapper;
import com.scaffold.modules.order.service.OrderService;
import com.scaffold.modules.member.service.MemberSettlementService;
import com.scaffold.modules.payment.dto.AAPayDTO;
import com.scaffold.modules.payment.dto.AdminPaymentQueryDTO;
import com.scaffold.modules.payment.dto.AlipayDTO;
import com.scaffold.modules.payment.dto.CashPayDTO;
import com.scaffold.modules.payment.dto.SplitBillDTO;
import com.scaffold.modules.payment.dto.SplitBillItemDTO;
import com.scaffold.modules.payment.dto.WechatPayDTO;
import com.scaffold.modules.payment.entity.PaymentRecord;
import com.scaffold.modules.payment.mapper.PaymentRecordMapper;
import com.scaffold.modules.payment.service.PaymentService;
import com.scaffold.modules.payment.vo.AdminPaymentRecordVO;
import com.scaffold.modules.payment.vo.CashPayVO;
import com.scaffold.modules.payment.vo.PaymentStatusVO;
import com.scaffold.modules.payment.vo.PaymentVO;
import com.scaffold.modules.system.entity.SysUser;
import com.scaffold.modules.system.mapper.SysUserMapper;
import com.scaffold.modules.table.entity.DiningTable;
import com.scaffold.modules.table.service.DiningTableService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 支付服务实现。
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecord> implements PaymentService {

    private static final DateTimeFormatter PAY_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int ORDER_CAS_RETRY_TIMES = 5;
    private static final long SYSTEM_OPERATOR_ID = 1L;
    private static final String SYSTEM_OPERATOR_NAME = "SYSTEM";
    private static final String WECHAT_SIGN_TYPE = "RSA";
    private static final String WECHAT_JSAPI_PATH = "/v3/pay/transactions/jsapi";
    private static final String WECHAT_NATIVE_PATH = "/v3/pay/transactions/native";
    private static final String WECHAT_API_BASE_URL = "https://api.mch.weixin.qq.com";

    private final OrderService orderService;
    private final CouponService couponService;
    private final OrderItemMapper orderItemMapper;
    private final OrderOperationLogMapper orderOperationLogMapper;
    private final SysUserMapper sysUserMapper;
    private final DiningTableService diningTableService;
    private final MemberSettlementService memberSettlementService;

    @Value("${wx.pay.enabled:false}")
    private boolean wechatPayEnabled;

    @Value("${wx.pay.app-id:}")
    private String nativeAppId;

    @Value("${wechat.miniapp.app-id:}")
    private String miniappAppId;

    @Value("${wx.pay.mch-id:}")
    private String merchantId;

    @Value("${wx.pay.api-v3-key:}")
    private String apiV3Key;

    @Value("${wx.pay.merchant-serial-no:}")
    private String merchantSerialNo;

    @Value("${wx.pay.private-key-path:}")
    private String privateKeyPath;

    @Value("${wx.pay.public-key-path:}")
    private String publicKeyPath;

    @Value("${wx.pay.notify-url:}")
    private String notifyUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * 输出微信支付基础配置加载结果。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 启动时记录核心配置是否加载成功，避免运行期才发现配置缺失。
     */
    @PostConstruct
    public void logWechatPayConfig() {
        log.info("微信支付配置加载: enabled={}, mchId={}, miniappAppId={}, nativeAppId={}, serialNo={}, notifyUrl={}, privateKeyPath={}, publicKeyPath={}",
                wechatPayEnabled,
                maskValue(merchantId),
                maskValue(miniappAppId),
                maskValue(nativeAppId),
                maskValue(merchantSerialNo),
                notifyUrl,
                privateKeyPath,
                publicKeyPath);
    }

    /**
     * 发起小程序微信支付预下单。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 调用微信支付 JSAPI 下单接口并返回小程序调起支付所需参数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO wechatPay(String openid, WechatPayDTO dto) {
        Order order = getAndValidateOrder(dto.getOrderId());
        String payerOpenid = firstNonBlank(openid, order.getCustomerOpenid());
        if (isBlank(payerOpenid)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "未获取到用户openid，无法发起微信支付");
        }

        PaymentRecord record = findOrCreatePendingWechatRecord(order, order.getActualAmount(), payerOpenid);
        if (!wechatPayEnabled) {
            log.warn("微信支付未启用，返回占位支付记录: paymentNo={}", record.getPaymentNo());
            return toPaymentVO(record);
        }

        JsonNode response = createWechatJsapiOrder(order, record, payerOpenid);
        String prepayId = textOrNull(response, "prepay_id");
        if (isBlank(prepayId)) {
            throw new BusinessException(ResultCode.PAYMENT_INIT_FAILED, "微信支付预下单失败，未返回 prepay_id");
        }

        PaymentVO vo = buildMiniappPaymentVO(record, payerOpenid, prepayId);
        updateCallbackData(record.getId(), buildPaymentCacheData(vo));
        return vo;
    }

    /**
     * 支付宝支付占位实现。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 当前项目统一切到微信支付，保留接口仅用于兼容旧调用。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO alipay(String openid, AlipayDTO dto) {
        throw new BusinessException(ResultCode.PARAM_ERROR, "当前版本仅支持微信支付");
    }

    /**
     * 现金支付。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 管理端现金收款后立即落账并联动订单、桌台状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CashPayVO cashPay(CashPayDTO dto) {
        Order order = getAndValidateOrder(dto.getOrderId());

        BigDecimal remaining = order.getActualAmount().subtract(order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO);
        if (dto.getReceivedAmount().compareTo(remaining) < 0) {
            throw new BusinessException(ResultCode.PAYMENT_AMOUNT_MISMATCH, "收款金额不足");
        }

        PaymentRecord record = new PaymentRecord();
        record.setOrderId(order.getId());
        record.setPaymentNo(generatePaymentNo());
        record.setPaymentMethod(2);
        record.setAmount(remaining);
        record.setStatus(1);
        save(record);

        BigDecimal changeAmount = dto.getReceivedAmount().subtract(remaining);
        OrderPaymentApplyResult payResult = applyPaymentToOrder(order.getId(), remaining);
        if (!payResult.applied) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单状态不允许支付");
        }

        logOperation(order.getId(), "PAY_CASH", null,
                String.format("{\"amount\":%s,\"receivedAmount\":%s,\"changeAmount\":%s,\"fullyPaid\":%s}",
                        remaining, dto.getReceivedAmount(), changeAmount, payResult.fullyPaid));

        if (payResult.fullyPaid) {
            markRemainingOrderItemsPaid(order.getId());
            // 订单已全部支付后再执行会员结算，避免部分支付时重复累计。
            memberSettlementService.settleAfterOrderPaid(order.getId());
            tryFinishTableAfterAdminCheckout(order, "CASH");
        }

        CashPayVO vo = new CashPayVO();
        BeanUtils.copyProperties(record, vo);
        vo.setChangeAmount(changeAmount);
        return vo;
    }

    /**
     * AA 分摊支付。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 兼容原有 AA 分摊场景，仍按内部已支付逻辑处理。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO aaPay(String openid, AAPayDTO dto) {
        Order order = getAndValidateOrder(dto.getOrderId());

        BigDecimal paidAmount = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal remaining = order.getActualAmount().subtract(paidAmount);
        if (dto.getAmount().compareTo(remaining) > 0) {
            throw new BusinessException(ResultCode.PAYMENT_SPLIT_AMOUNT_ERROR, "分摊金额超过剩余待支付金额");
        }

        PaymentRecord record = new PaymentRecord();
        record.setOrderId(order.getId());
        record.setPaymentNo(generatePaymentNo());
        record.setPaymentMethod(0);
        record.setAmount(dto.getAmount());
        record.setStatus(1);
        record.setPayerOpenid(openid);
        save(record);

        OrderPaymentApplyResult payResult = applyPaymentToOrder(order.getId(), dto.getAmount());
        if (!payResult.applied) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单状态不允许支付");
        }

        logOperation(order.getId(), "PAY_AA", null,
                String.format("{\"amount\":%s,\"fullyPaid\":%s}", dto.getAmount(), payResult.fullyPaid));

        if (payResult.fullyPaid) {
            markRemainingOrderItemsPaid(order.getId());
            // 仅在最终结清时结算会员权益。
            memberSettlementService.settleAfterOrderPaid(order.getId());
        }

        return toPaymentVO(record);
    }

    /**
     * 查询支付状态。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 对待支付的微信订单主动查单，降低前端轮询结果不一致的概率。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentStatusVO getPaymentStatus(Long paymentId) {
        PaymentRecord record = getById(paymentId);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "支付记录不存在");
        }

        if (record.getStatus() == 0 && wechatPayEnabled && record.getPaymentMethod() != null && record.getPaymentMethod() != 2) {
            WechatQueryResult queryResult = queryWechatOrder(record.getPaymentNo());
            if (queryResult.success) {
                processPaymentCallback(record.getPaymentNo(), queryResult.transactionId, queryResult.rawResponse);
                record = getById(paymentId);
            } else if (queryResult.closed) {
                markPaymentFailed(record.getId(), queryResult.rawResponse);
                record = getById(paymentId);
            }
        }

        Order order = orderService.getById(record.getOrderId());
        PaymentStatusVO vo = new PaymentStatusVO();
        vo.setPaymentNo(record.getPaymentNo());
        vo.setStatus(record.getStatus());

        if (order != null) {
            BigDecimal paidAmount = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
            vo.setPaidAmount(paidAmount);
            vo.setRemainingAmount(order.getActualAmount().subtract(paidAmount));
        } else {
            vo.setPaidAmount(record.getAmount());
            vo.setRemainingAmount(BigDecimal.ZERO);
        }

        return vo;
    }

    /**
     * 生成管理端扫码支付二维码。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 调用微信支付 Native 下单接口，返回 `code_url` 给管理端展示二维码。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO generateQrCode(Long orderId) {
        Order order = getAndValidateOrder(orderId);
        BigDecimal paidAmount = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal remaining = order.getActualAmount().subtract(paidAmount);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PAYMENT_AMOUNT_MISMATCH, "订单无待支付金额");
        }

        PaymentRecord record = findOrCreatePendingWechatRecord(order, remaining, null);
        if (!wechatPayEnabled) {
            return toPaymentVO(record);
        }

        PaymentVO cached = buildPaymentVOFromCallbackData(record);
        if (cached != null && !isBlank(cached.getPayUrl())) {
            return cached;
        }

        JsonNode response = createWechatNativeOrder(order, record);
        String codeUrl = textOrNull(response, "code_url");
        if (isBlank(codeUrl)) {
            throw new BusinessException(ResultCode.PAYMENT_INIT_FAILED, "微信扫码支付下单失败，未返回 code_url");
        }

        PaymentVO vo = toPaymentVO(record);
        vo.setPayUrl(codeUrl);
        vo.setQrCodeUrl(codeUrl);
        updateCallbackData(record.getId(), buildPaymentCacheData(vo));
        return vo;
    }

    /**
     * 分单结账。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 现金分单即时落账，微信分单保留待支付记录供后续收款。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void splitBill(SplitBillDTO dto) {
        Order order = getAndValidateOrder(dto.getOrderId());
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分单子项不能为空");
        }

        BigDecimal totalSplitAmount = BigDecimal.ZERO;
        for (SplitBillItemDTO splitItem : dto.getItems()) {
            BigDecimal splitAmount = BigDecimal.ZERO;
            List<Long> orderItemIds = normalizeOrderItemIds(splitItem);
            for (Long orderItemId : orderItemIds) {
                OrderItem item = orderItemMapper.selectById(orderItemId);
                if (item == null || !item.getOrderId().equals(order.getId())) {
                    throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单项不存在或不属于该订单");
                }
                if (item.getPaymentStatus() != null && item.getPaymentStatus() == 2) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "所选菜品中存在已支付项，请刷新后重试");
                }
                splitAmount = splitAmount.add(item.getAmount());
            }

            Integer paymentMethod = splitItem.getPaymentMethod() != null ? splitItem.getPaymentMethod() : 0;
            boolean immediatePaid = paymentMethod == 2;

            PaymentRecord record = new PaymentRecord();
            record.setOrderId(order.getId());
            record.setPaymentNo(generatePaymentNo());
            record.setPaymentMethod(paymentMethod);
            record.setAmount(splitAmount);
            record.setStatus(immediatePaid ? 1 : 0);
            save(record);
            totalSplitAmount = totalSplitAmount.add(splitAmount);

            if (immediatePaid) {
                OrderPaymentApplyResult payResult = applyPaymentToOrder(order.getId(), splitAmount);
                if (!payResult.applied) {
                    throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单状态不允许支付");
                }
                markOrderItemsPaid(orderItemIds);
                logOperation(order.getId(), "SPLIT_PAY_CASH", null,
                        String.format("{\"paymentNo\":\"%s\",\"amount\":%s,\"fullyPaid\":%s}",
                                record.getPaymentNo(), splitAmount, payResult.fullyPaid));
                if (payResult.fullyPaid) {
                    markRemainingOrderItemsPaid(order.getId());
                    // 分单场景下，最后一笔付清后再累计积分和成长值。
                    memberSettlementService.settleAfterOrderPaid(order.getId());
                    tryFinishTableAfterAdminCheckout(order, "SPLIT_CASH");
                }
            } else {
                logOperation(order.getId(), "SPLIT_BILL", null,
                        String.format("{\"paymentNo\":\"%s\",\"amount\":%s,\"paymentMethod\":%d}",
                                record.getPaymentNo(), splitAmount, paymentMethod));
            }
        }

        log.info("分单结账创建完成: orderId={}, splitCount={}, totalAmount={}",
                order.getId(), dto.getItems().size(), totalSplitAmount);
    }

    /**
     * 管理端整单退款。
     *
     * @param orderId 订单ID
     * @param dto 退款参数
     * @author Henfon
     * @date 2026-07-01
     * @description 将订单和已支付支付单统一标记为已退款，并触发会员积分与成长值逆向回退。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(Long orderId, com.scaffold.modules.payment.dto.OrderRefundDTO dto) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != null && order.getStatus() == 3) {
            log.info("订单已退款，跳过重复处理: orderId={}", orderId);
            return;
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "仅已支付订单可退款");
        }

        List<PaymentRecord> paidRecords = list(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, orderId)
                .eq(PaymentRecord::getStatus, 1)
                .eq(PaymentRecord::getDeleted, 0));
        if (paidRecords.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未找到可退款的支付记录");
        }

        // 先更新支付记录，确保支付台账与订单状态保持一致。
        update(new LambdaUpdateWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, orderId)
                .eq(PaymentRecord::getStatus, 1)
                .set(PaymentRecord::getStatus, 2));

        order.setStatus(3);
        orderService.updateById(order);

        BigDecimal refundAmount = order.getPaidAmount() != null && order.getPaidAmount().compareTo(BigDecimal.ZERO) > 0
                ? order.getPaidAmount()
                : order.getActualAmount();
        if (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            memberSettlementService.rollbackAfterOrderRefund(orderId, refundAmount);
        }

        logOperation(orderId, "REFUND_ORDER", dto.getReason(),
                String.format("{\"refundAmount\":%s,\"paymentCount\":%d}", refundAmount, paidRecords.size()));
        log.info("管理端整单退款完成: orderId={}, refundAmount={}, paymentCount={}", orderId, refundAmount, paidRecords.size());
    }

    /**
     * 管理端分页查询支付记录。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 根据时间、支付方式、状态等条件查询支付记录。
     */
    @Override
    public PageResult<AdminPaymentRecordVO> listPaymentsForAdmin(int pageNum, int pageSize, AdminPaymentQueryDTO queryDTO) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            if (!isBlank(queryDTO.getAreaName())) {
                List<Long> orderIds = loadOrderIdsByAreaName(queryDTO.getAreaName());
                if (orderIds.isEmpty()) {
                    return PageResult.of(Collections.emptyList(), (long) pageNum, (long) pageSize, 0L);
                }
                wrapper.in(PaymentRecord::getOrderId, orderIds);
            }
            wrapper.eq(queryDTO.getOrderId() != null, PaymentRecord::getOrderId, queryDTO.getOrderId())
                    .like(queryDTO.getPaymentNo() != null && !queryDTO.getPaymentNo().isBlank(),
                            PaymentRecord::getPaymentNo, queryDTO.getPaymentNo())
                    .eq(queryDTO.getPaymentMethod() != null, PaymentRecord::getPaymentMethod, queryDTO.getPaymentMethod())
                    .eq(queryDTO.getStatus() != null, PaymentRecord::getStatus, queryDTO.getStatus());
            if (queryDTO.getStartDate() != null) {
                wrapper.ge(PaymentRecord::getCreateTime, queryDTO.getStartDate().atStartOfDay());
            }
            if (queryDTO.getEndDate() != null) {
                wrapper.le(PaymentRecord::getCreateTime, queryDTO.getEndDate().plusDays(1).atStartOfDay());
            }
        }
        wrapper.orderByDesc(PaymentRecord::getCreateTime);

        Page<PaymentRecord> pageResult = page(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, Order> orderMap = loadOrderMap(pageResult.getRecords());
        Map<Long, DiningTable> tableMap = loadDiningTableMap(orderMap);
        Map<String, String> payerNameMap = loadPayerNameMap(pageResult.getRecords());
        List<AdminPaymentRecordVO> voList = pageResult.getRecords().stream().map(record -> {
            AdminPaymentRecordVO vo = new AdminPaymentRecordVO();
            BeanUtils.copyProperties(record, vo);
            Order order = orderMap.get(record.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                vo.setTableCode(order.getTableCode());
                // 基于订单关联桌台，补齐支付记录页的区域名称，便于前台/财务对账定位。
                DiningTable table = tableMap.get(order.getTableId());
                if (table != null) {
                    vo.setAreaName(table.getAreaName());
                }
            }
            vo.setPayerName(payerNameMap.get(record.getPayerOpenid()));
            return vo;
        }).toList();
        return PageResult.of(voList, pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
    }

    /**
     * 根据区域名称加载订单ID列表
     *
     * @param areaName 区域名称
     * @return 订单ID列表
     * @author Henfon
     * @date 2026-07-03
     * @description 支付记录按区域筛选时，先定位区域桌台，再反查关联订单，保证分页总数和筛选结果一致。
     */
    private List<Long> loadOrderIdsByAreaName(String areaName) {
        String normalizedAreaName = areaName == null ? null : areaName.trim();
        if (isBlank(normalizedAreaName)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<DiningTable> tableWrapper = new LambdaQueryWrapper<>();
        tableWrapper.eq(DiningTable::getDeleted, 0);
        if (Objects.equals(normalizedAreaName, "未分区")) {
            tableWrapper.and(wrapper -> wrapper.isNull(DiningTable::getAreaName).or().eq(DiningTable::getAreaName, ""));
        } else {
            tableWrapper.eq(DiningTable::getAreaName, normalizedAreaName);
        }
        List<Long> tableIds = diningTableService.list(tableWrapper).stream()
                .map(DiningTable::getId)
                .toList();
        if (tableIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(Order::getTableId, tableIds)
                .eq(Order::getDeleted, 0);

        // 通过订单表做一次中间映射，避免支付记录直接依赖桌台字段。
        return orderService.list(orderWrapper).stream()
                .map(Order::getId)
                .toList();
    }

    /**
     * 批量加载订单映射
     *
     * @param records 支付记录列表
     * @return 订单映射
     * @author Henfon
     * @date 2026-06-27
     * @description 按订单ID批量查询订单，避免管理端支付记录列表出现逐条查单的 N+1 问题
     */
    private Map<Long, Order> loadOrderMap(List<PaymentRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> orderIds = records.stream()
                .map(PaymentRecord::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return orderService.listByIds(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, order -> order, (left, right) -> left));
    }

    /**
     * 批量加载桌台映射
     *
     * @param orderMap 订单映射
     * @return 桌台映射
     * @author Henfon
     * @date 2026-07-03
     * @description 根据订单中的桌台ID批量查询桌台信息，统一给支付记录等页面补齐区域展示。
     */
    private Map<Long, DiningTable> loadDiningTableMap(Map<Long, Order> orderMap) {
        if (orderMap == null || orderMap.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> tableIds = orderMap.values().stream()
                .map(Order::getTableId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (tableIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 批量查桌台，避免在支付记录列表中按行查询区域导致额外数据库开销。
        return diningTableService.listByIds(tableIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(DiningTable::getId, table -> table, (left, right) -> left));
    }

    /**
     * 批量加载付款人名称映射
     *
     * @param records 支付记录列表
     * @return openid 到付款人名称的映射
     * @author Henfon
     * @date 2026-06-27
     * @description 根据支付记录中的 payerOpenid 批量匹配系统用户，优先返回昵称，其次返回用户名
     */
    private Map<String, String> loadPayerNameMap(List<PaymentRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> openids = records.stream()
                .map(PaymentRecord::getPayerOpenid)
                .filter(openid -> !isBlank(openid))
                .distinct()
                .toList();
        if (openids.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getOpenid, openids)
                .eq(SysUser::getDeleted, 0);

        return sysUserMapper.selectList(wrapper).stream()
                .filter(user -> !isBlank(user.getOpenid()))
                .collect(Collectors.toMap(
                        SysUser::getOpenid,
                        user -> firstNonBlank(user.getNickname(), user.getUsername()),
                        (left, right) -> left
                ));
    }

    /**
     * 处理微信支付回调。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 验证微信支付签名并解密通知报文，之后按支付成功结果执行落账。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleWechatCallback(Map<String, String> callbackHeaders, String callbackData) {
        verifyWechatSignature(callbackHeaders, callbackData);

        JsonNode callbackRoot = readJson(callbackData, "微信支付回调报文解析失败");
        JsonNode decryptNode = decryptWechatNotifyResource(callbackRoot.path("resource"));
        String tradeState = textOrNull(decryptNode, "trade_state");
        if (!Objects.equals(tradeState, "SUCCESS")) {
            log.info("微信支付回调状态非成功，忽略: tradeState={}", tradeState);
            return;
        }

        String paymentNo = textOrNull(decryptNode, "out_trade_no");
        if (isBlank(paymentNo)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "微信支付回调缺少 out_trade_no");
        }

        String transactionId = textOrNull(decryptNode, "transaction_id");
        processPaymentCallback(paymentNo, transactionId, decryptNode.toString());
    }

    /**
     * 支付宝回调占位实现。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 当前版本不再接收支付宝回调，保留接口用于兼容。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleAlipayCallback(String callbackData) {
        throw new BusinessException(ResultCode.PARAM_ERROR, "当前版本仅支持微信支付");
    }

    /**
     * 调用微信支付 JSAPI 下单接口。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 将订单信息转换成微信支付统一下单报文，并返回微信平台响应。
     */
    private JsonNode createWechatJsapiOrder(Order order, PaymentRecord record, String openid) {
        ensureWechatPayConfig(miniappAppId);

        Map<String, Object> body = new HashMap<>();
        body.put("appid", miniappAppId);
        body.put("mchid", merchantId);
        body.put("description", buildPaymentTitle(order));
        body.put("out_trade_no", record.getPaymentNo());
        body.put("notify_url", notifyUrl);
        body.put("amount", Map.of("total", toFen(record.getAmount()), "currency", "CNY"));
        body.put("payer", Map.of("openid", openid));

        return sendWechatPost(WECHAT_JSAPI_PATH, body);
    }

    /**
     * 调用微信支付 Native 下单接口。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 生成管理端扫码支付使用的 `code_url`。
     */
    private JsonNode createWechatNativeOrder(Order order, PaymentRecord record) {
        String nativePayAppId = firstNonBlank(nativeAppId, miniappAppId);
        ensureWechatPayConfig(nativePayAppId);

        Map<String, Object> body = new HashMap<>();
        body.put("appid", nativePayAppId);
        body.put("mchid", merchantId);
        body.put("description", buildPaymentTitle(order));
        body.put("out_trade_no", record.getPaymentNo());
        body.put("notify_url", notifyUrl);
        body.put("amount", Map.of("total", toFen(record.getAmount()), "currency", "CNY"));

        return sendWechatPost(WECHAT_NATIVE_PATH, body);
    }

    /**
     * 主动查单。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 通过商户订单号查询微信支付状态，便于管理端轮询确认结果。
     */
    private WechatQueryResult queryWechatOrder(String paymentNo) {
        WechatQueryResult result = new WechatQueryResult();
        if (!wechatPayEnabled || isBlank(paymentNo)) {
            return result;
        }

        String encodedPaymentNo = URLEncoder.encode(paymentNo, StandardCharsets.UTF_8);
        String path = "/v3/pay/transactions/out-trade-no/" + encodedPaymentNo + "?mchid=" + merchantId;
        String body = sendWechatGet(path);
        result.rawResponse = body;

        JsonNode response = readJson(body, "微信支付查单响应解析失败");
        String tradeState = textOrNull(response, "trade_state");
        result.transactionId = textOrNull(response, "transaction_id");
        result.success = Objects.equals(tradeState, "SUCCESS");
        result.closed = Objects.equals(tradeState, "CLOSED")
                || Objects.equals(tradeState, "REVOKED")
                || Objects.equals(tradeState, "PAYERROR");
        return result;
    }

    /**
     * 发送微信支付 POST 请求。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 统一完成请求序列化、签名和响应校验。
     */
    private JsonNode sendWechatPost(String path, Map<String, Object> bodyMap) {
        String body = writeJson(bodyMap);
        HttpRequest request = HttpRequest.newBuilder(URI.create(WECHAT_API_BASE_URL + path))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", buildWechatAuthorization("POST", path, body))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        String responseBody = sendWechatRequest(request);
        return readJson(responseBody, "微信支付下单响应解析失败");
    }

    /**
     * 发送微信支付 GET 请求。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 统一完成查询接口的签名和响应体读取。
     */
    private String sendWechatGet(String path) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(WECHAT_API_BASE_URL + path))
                .header("Accept", "application/json")
                .header("Authorization", buildWechatAuthorization("GET", path, ""))
                .GET()
                .build();
        return sendWechatRequest(request);
    }

    /**
     * 执行 HTTP 请求并校验响应码。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 对微信支付接口的非 2xx 响应直接抛业务异常，便于快速定位问题。
     */
    private String sendWechatRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("微信支付接口调用失败: status={}, body={}", response.statusCode(), response.body());
                throw new BusinessException(ResultCode.PAYMENT_INIT_FAILED,
                        "微信支付调用失败: HTTP " + response.statusCode() + "，" + summarizeWechatError(response.body()));
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.PAYMENT_INIT_FAILED, "微信支付请求被中断: " + e.getMessage());
        } catch (IOException e) {
            throw new BusinessException(ResultCode.PAYMENT_INIT_FAILED, "微信支付请求失败: " + e.getMessage());
        }
    }

    /**
     * 提取微信支付错误响应摘要
     *
     * @author Henfon
     * @date 2026-07-12
     * @description 从微信支付错误响应中提取 code/message，方便小程序和日志直接展示真实失败原因。
     * @param responseBody 微信支付响应体
     * @return 错误摘要
     */
    private String summarizeWechatError(String responseBody) {
        if (isBlank(responseBody)) {
            return "微信未返回错误详情";
        }

        try {
            JsonNode node = OBJECT_MAPPER.readTree(responseBody);
            String code = textOrNull(node, "code");
            String message = textOrNull(node, "message");
            if (!isBlank(code) || !isBlank(message)) {
                return firstNonBlank(code, "UNKNOWN") + ": " + firstNonBlank(message, "微信未返回错误详情");
            }
        } catch (Exception e) {
            log.warn("微信支付错误响应解析失败: {}", e.getMessage());
        }

        return responseBody.length() > 180 ? responseBody.substring(0, 180) + "..." : responseBody;
    }

    /**
     * 构造微信支付 Authorization 头。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 按微信支付 API v3 规范签名请求。
     */
    private String buildWechatAuthorization(String method, String canonicalUrl, String body) {
        ensureWechatPayConfig(firstNonBlank(nativeAppId, miniappAppId));
        String nonceStr = generateNonce();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signMessage = method + "\n" + canonicalUrl + "\n" + timestamp + "\n" + nonceStr + "\n" + body + "\n";
        String signature = signWithMerchantPrivateKey(signMessage);

        return "WECHATPAY2-SHA256-RSA2048 "
                + "mchid=\"" + merchantId + "\","
                + "nonce_str=\"" + nonceStr + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + merchantSerialNo + "\","
                + "signature=\"" + signature + "\"";
    }

    /**
     * 生成小程序调起支付参数。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 将微信返回的 `prepay_id` 转换为 `wx.requestPayment` 所需参数。
     */
    private PaymentVO buildMiniappPaymentVO(PaymentRecord record, String openid, String prepayId) {
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = generateNonce();
        String packageValue = "prepay_id=" + prepayId;
        String signMessage = miniappAppId + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageValue + "\n";

        PaymentVO vo = toPaymentVO(record);
        vo.setAppId(miniappAppId);
        vo.setPayerOpenid(openid);
        vo.setTimeStamp(timeStamp);
        vo.setNonceStr(nonceStr);
        vo.setPackageValue(packageValue);
        vo.setSignType(WECHAT_SIGN_TYPE);
        vo.setPaySign(signWithMerchantPrivateKey(signMessage));
        return vo;
    }

    /**
     * 验证微信支付回调签名。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 基于你已配置的微信支付公钥校验回调通知合法性。
     */
    private void verifyWechatSignature(Map<String, String> callbackHeaders, String callbackData) {
        String timestamp = getHeaderIgnoreCase(callbackHeaders, "Wechatpay-Timestamp");
        String nonce = getHeaderIgnoreCase(callbackHeaders, "Wechatpay-Nonce");
        String signature = getHeaderIgnoreCase(callbackHeaders, "Wechatpay-Signature");
        if (isBlank(timestamp) || isBlank(nonce) || isBlank(signature)) {
            throw new BusinessException(ResultCode.PAYMENT_SIGN_VERIFY_FAILED, "微信支付回调缺少签名头");
        }

        String message = timestamp + "\n" + nonce + "\n" + callbackData + "\n";
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(loadWechatPublicKey());
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            boolean verified = verifier.verify(Base64.getDecoder().decode(signature));
            if (!verified) {
                throw new BusinessException(ResultCode.PAYMENT_SIGN_VERIFY_FAILED, "微信支付回调验签失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PAYMENT_SIGN_VERIFY_FAILED, "微信支付回调验签异常: " + e.getMessage());
        }
    }

    /**
     * 解密微信支付回调资源。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 使用 API v3 Key 解密微信支付通知资源明文。
     */
    private JsonNode decryptWechatNotifyResource(JsonNode resourceNode) {
        String algorithm = textOrNull(resourceNode, "algorithm");
        String ciphertext = textOrNull(resourceNode, "ciphertext");
        String nonce = textOrNull(resourceNode, "nonce");
        String associatedData = textOrNull(resourceNode, "associated_data");

        if (!Objects.equals(algorithm, "AEAD_AES_256_GCM") || isBlank(ciphertext) || isBlank(nonce)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "微信支付回调资源格式非法");
        }

        try {
            byte[] cipherData = Base64.getDecoder().decode(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            if (!isBlank(associatedData)) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }

            byte[] plainData = cipher.doFinal(cipherData);
            return readJson(new String(plainData, StandardCharsets.UTF_8), "微信支付回调解密后报文解析失败");
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PAYMENT_SIGN_VERIFY_FAILED, "微信支付回调解密失败: " + e.getMessage());
        }
    }

    /**
     * 使用商户私钥签名。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 微信支付下单和小程序调起支付都依赖该签名结果。
     */
    private String signWithMerchantPrivateKey(String message) {
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(loadMerchantPrivateKey());
            signer.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PAYMENT_INIT_FAILED, "微信支付签名失败: " + e.getMessage());
        }
    }

    /**
     * 加载商户私钥。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 从 PEM 文件中读取商户私钥，用于请求签名。
     */
    private PrivateKey loadMerchantPrivateKey() {
        try {
            String pem = Files.readString(Path.of(privateKeyPath), StandardCharsets.UTF_8);
            String privateKeyContent = normalizePem(pem);
            byte[] decoded = Base64.getDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PAYMENT_INIT_FAILED, "读取微信支付商户私钥失败: " + e.getMessage());
        }
    }

    /**
     * 加载微信支付公钥。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 从 PEM 文件中读取微信支付公钥，用于回调验签。
     */
    private PublicKey loadWechatPublicKey() {
        try {
            String pem = Files.readString(Path.of(publicKeyPath), StandardCharsets.UTF_8);
            String publicKeyContent = normalizePem(pem);
            byte[] decoded = Base64.getDecoder().decode(publicKeyContent);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PAYMENT_INIT_FAILED, "读取微信支付公钥失败: " + e.getMessage());
        }
    }

    /**
     * 标准化 PEM 文本内容。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 去除 BEGIN/END 标记和换行，便于 Base64 解码。
     */
    private String normalizePem(String pem) {
        return pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\r", "")
                .replace("\n", "")
                .trim();
    }

    /**
     * 校验微信支付配置完整性。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 在发起微信支付相关请求前统一校验必填配置。
     */
    private void ensureWechatPayConfig(String appId) {
        if (!wechatPayEnabled) {
            throw new BusinessException(ResultCode.PAYMENT_INIT_FAILED, "微信支付未启用");
        }
        if (isBlank(appId) || isBlank(merchantId) || isBlank(apiV3Key) || isBlank(merchantSerialNo)
                || isBlank(privateKeyPath) || isBlank(publicKeyPath) || isBlank(notifyUrl)) {
            throw new BusinessException(ResultCode.PAYMENT_INIT_FAILED, "微信支付配置不完整");
        }
    }

    /**
     * 读取支付缓存数据。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 从支付记录 callbackData 中恢复上次下单返回的支付参数，减少重复下单。
     */
    private PaymentVO buildPaymentVOFromCallbackData(PaymentRecord record) {
        if (record == null || isBlank(record.getCallbackData())) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(record.getCallbackData());
            PaymentVO vo = toPaymentVO(record);
            vo.setAppId(textOrNull(root, "appId"));
            vo.setTimeStamp(textOrNull(root, "timeStamp"));
            vo.setNonceStr(textOrNull(root, "nonceStr"));
            vo.setPackageValue(textOrNull(root, "packageValue"));
            vo.setSignType(textOrNull(root, "signType"));
            vo.setPaySign(textOrNull(root, "paySign"));
            vo.setPayUrl(textOrNull(root, "payUrl"));
            vo.setQrCodeUrl(textOrNull(root, "qrCodeUrl"));
            return vo;
        } catch (Exception e) {
            log.warn("支付缓存数据解析失败: paymentNo={}, err={}", record.getPaymentNo(), e.getMessage());
            return null;
        }
    }

    /**
     * 将支付参数持久化到 callbackData。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 复用现有字段保存微信下单返回结果，避免额外改表。
     */
    private String buildPaymentCacheData(PaymentVO vo) {
        Map<String, Object> data = new HashMap<>();
        data.put("appId", vo.getAppId());
        data.put("timeStamp", vo.getTimeStamp());
        data.put("nonceStr", vo.getNonceStr());
        data.put("packageValue", vo.getPackageValue());
        data.put("signType", vo.getSignType());
        data.put("paySign", vo.getPaySign());
        data.put("payUrl", vo.getPayUrl());
        data.put("qrCodeUrl", vo.getQrCodeUrl());
        return writeJson(data);
    }

    /**
     * 更新支付记录缓存数据。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 下单成功后保存支付参数，便于轮询和重复进入页面时复用。
     */
    private void updateCallbackData(Long paymentRecordId, String callbackData) {
        update(new LambdaUpdateWrapper<PaymentRecord>()
                .eq(PaymentRecord::getId, paymentRecordId)
                .set(PaymentRecord::getCallbackData, callbackData));
    }

    /**
     * 标记支付失败。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 对已关闭或支付失败的微信订单同步更新本地支付状态。
     */
    private void markPaymentFailed(Long paymentRecordId, String callbackData) {
        update(new LambdaUpdateWrapper<PaymentRecord>()
                .eq(PaymentRecord::getId, paymentRecordId)
                .eq(PaymentRecord::getStatus, 0)
                .set(PaymentRecord::getStatus, 3)
                .set(PaymentRecord::getCallbackData, callbackData));
    }

    /**
     * 查找或创建待支付微信记录。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 同一订单同一支付入口优先复用待支付记录；若订单金额已变化，则作废旧记录并生成新支付单。
     */
    private PaymentRecord findOrCreatePendingWechatRecord(Order order, BigDecimal amount, String payerOpenid) {
        BigDecimal expectedAmount = amount != null ? amount : BigDecimal.ZERO;
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, order.getId())
                .eq(PaymentRecord::getPaymentMethod, 0)
                .eq(PaymentRecord::getStatus, 0)
                .eq(PaymentRecord::getDeleted, 0)
                .orderByDesc(PaymentRecord::getCreateTime)
                .last("LIMIT 1");

        if (payerOpenid == null) {
            wrapper.isNull(PaymentRecord::getPayerOpenid);
        } else {
            wrapper.eq(PaymentRecord::getPayerOpenid, payerOpenid);
        }

        PaymentRecord record = getOne(wrapper);
        if (record != null) {
            BigDecimal recordAmount = record.getAmount() != null ? record.getAmount() : BigDecimal.ZERO;
            // 订单加菜、优惠重算后金额会变化，旧 prepay 记录不能继续复用。
            if (recordAmount.compareTo(expectedAmount) != 0) {
                log.warn("待支付记录金额和订单金额不一致，作废旧支付记录: orderId={}, paymentNo={}, oldAmount={}, newAmount={}",
                        order.getId(), record.getPaymentNo(), recordAmount, expectedAmount);
                markPaymentFailed(record.getId(),
                        String.format("{\"reason\":\"amount_changed\",\"oldAmount\":%s,\"newAmount\":%s}",
                                recordAmount, expectedAmount));
                return createPaymentRecord(order, 0, expectedAmount, payerOpenid);
            }
            return record;
        }
        return createPaymentRecord(order, 0, expectedAmount, payerOpenid);
    }

    /**
     * 规范化分单订单项 ID。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 同时兼容新旧前端字段，统一转换成列表处理。
     */
    private List<Long> normalizeOrderItemIds(SplitBillItemDTO splitItem) {
        List<Long> orderItemIds = new ArrayList<>();
        if (splitItem.getOrderItemIds() != null && !splitItem.getOrderItemIds().isEmpty()) {
            orderItemIds.addAll(splitItem.getOrderItemIds());
        } else if (splitItem.getOrderItemId() != null) {
            orderItemIds.add(splitItem.getOrderItemId());
        }
        if (orderItemIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分单子项缺少订单项ID");
        }
        return orderItemIds;
    }

    /**
     * 获取并校验订单。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 仅允许待支付订单进入支付流程。
     */
    private Order getAndValidateOrder(Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单状态不允许支付");
        }
        return order;
    }

    /**
     * 创建待支付记录。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 统一生成支付流水号并保存本地待支付记录。
     */
    private PaymentRecord createPaymentRecord(Order order, int paymentMethod, BigDecimal amount, String openid) {
        PaymentRecord record = new PaymentRecord();
        record.setOrderId(order.getId());
        record.setPaymentNo(generatePaymentNo());
        record.setPaymentMethod(paymentMethod);
        record.setAmount(amount);
        record.setStatus(0);
        record.setPayerOpenid(openid);
        save(record);
        return record;
    }

    /**
     * 支付完成后应用到订单。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 通过 CAS 方式更新订单已支付金额与状态，避免并发覆盖。
     */
    private OrderPaymentApplyResult applyPaymentToOrder(Long orderId, BigDecimal payAmount) {
        if (orderId == null || payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return OrderPaymentApplyResult.notApplied();
        }

        for (int i = 0; i < ORDER_CAS_RETRY_TIMES; i++) {
            Order latest = orderService.getById(orderId);
            if (latest == null) {
                return OrderPaymentApplyResult.notApplied();
            }

            BigDecimal currentPaid = latest.getPaidAmount() != null ? latest.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal actualAmount = latest.getActualAmount() != null ? latest.getActualAmount() : BigDecimal.ZERO;
            BigDecimal remaining = actualAmount.subtract(currentPaid);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0 || latest.getStatus() != 0) {
                return OrderPaymentApplyResult.notApplied();
            }

            BigDecimal appliedAmount = payAmount.min(remaining);
            BigDecimal newPaid = currentPaid.add(appliedAmount);
            boolean fullyPaid = newPaid.compareTo(actualAmount) >= 0;

            LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<Order>()
                    .eq(Order::getId, orderId)
                    .eq(Order::getStatus, latest.getStatus())
                    .set(Order::getPaidAmount, newPaid)
                    .set(Order::getStatus, fullyPaid ? 1 : 0);
            if (latest.getPaidAmount() == null) {
                updateWrapper.isNull(Order::getPaidAmount);
            } else {
                updateWrapper.eq(Order::getPaidAmount, latest.getPaidAmount());
            }

            boolean updated = orderService.update(updateWrapper);
            if (!updated) {
                continue;
            }

            if (fullyPaid) {
                couponService.markCouponUsed(latest.getId());
            }
            return OrderPaymentApplyResult.applied(newPaid, actualAmount.subtract(newPaid), fullyPaid);
        }

        throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单支付处理中，请稍后重试");
    }

    /**
     * 管理端收款完成后尝试结台。
     *
     * @author Henfon
     * @date 2026-07-13
     * @description 仅管理端结账场景触发；同桌次仍有待支付订单时保留占用状态，供继续收款或加菜。
     * @param order 本次完成收款的订单
     * @param paymentScene 管理端收款场景
     */
    private void tryFinishTableAfterAdminCheckout(Order order, String paymentScene) {
        if (order == null || order.getTableId() == null) {
            return;
        }

        try {
            boolean settled = diningTableService.checkoutTableIfSettled(order.getTableId());
            if (settled) {
                log.info("管理端收款后结台完成: tableId={}, orderId={}, scene={}",
                        order.getTableId(), order.getId(), paymentScene);
                return;
            }
            log.info("管理端收款完成但当前桌次仍有待支付订单，桌台保持占用: tableId={}, orderId={}, scene={}",
                    order.getTableId(), order.getId(), paymentScene);
        } catch (Exception e) {
            // 桌台收尾异常不能回滚已经成功的收款，保留桌态供管理端人工处理。
            log.warn("管理端收款成功但自动结台失败: tableId={}, orderId={}, scene={}, msg={}",
                    order.getTableId(), order.getId(), paymentScene, e.getMessage());
        }
    }

    /**
     * 生成支付流水号。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 使用时间戳加随机串生成本地唯一支付单号。
     */
    private String generatePaymentNo() {
        String timestamp = LocalDateTime.now().format(PAY_NO_FORMATTER);
        return "PAY" + timestamp + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    /**
     * 支付记录转 VO。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 保持返回对象结构统一。
     */
    private PaymentVO toPaymentVO(PaymentRecord record) {
        PaymentVO vo = new PaymentVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }

    /**
     * 处理支付成功回调。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 幂等更新支付记录，再联动订单已支付金额。
     */
    private void processPaymentCallback(String paymentNo, String thirdPartyNo, String callbackData) {
        PaymentRecord record = getOne(new LambdaQueryWrapper<PaymentRecord>().eq(PaymentRecord::getPaymentNo, paymentNo));
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "支付记录不存在: " + paymentNo);
        }

        if (record.getStatus() == 1) {
            log.info("支付回调幂等校验：paymentNo={} 已处理，跳过", paymentNo);
            return;
        }

        LambdaUpdateWrapper<PaymentRecord> callbackUpdate = new LambdaUpdateWrapper<PaymentRecord>()
                .eq(PaymentRecord::getId, record.getId())
                .in(PaymentRecord::getStatus, 0, 3)
                .set(PaymentRecord::getStatus, 1)
                .set(PaymentRecord::getThirdPartyNo, thirdPartyNo)
                .set(PaymentRecord::getCallbackData, callbackData);

        boolean updated = update(callbackUpdate);
        if (!updated) {
            PaymentRecord latest = getById(record.getId());
            if (latest != null && latest.getStatus() != null && latest.getStatus() == 1) {
                log.info("支付回调并发幂等：paymentNo={} 已由其他请求处理", paymentNo);
                return;
            }
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "支付状态更新失败");
        }

        OrderPaymentApplyResult payResult = applyPaymentToOrder(record.getOrderId(), record.getAmount());
        if (!payResult.applied) {
            log.warn("支付回调：订单不存在或已处理, paymentNo={}, orderId={}", paymentNo, record.getOrderId());
            return;
        }

        logOperation(record.getOrderId(), "PAY_CALLBACK", null,
                String.format("{\"paymentNo\":\"%s\",\"paymentMethod\":%d,\"amount\":%s,\"fullyPaid\":%s}",
                        paymentNo, record.getPaymentMethod(), record.getAmount(), payResult.fullyPaid));
        if (payResult.fullyPaid) {
            markRemainingOrderItemsPaid(record.getOrderId());
            if (!isAdminCheckoutPayment(record)) {
                // 小程序餐前付订单在支付成功后才允许进入后厨，避免未支付订单提前播报和制作。
                orderService.notifyKitchenOrderPaid(record.getOrderId());
            }
            // 回调场景与现金/分单保持一致，只有整单付清后才做会员结算。
            memberSettlementService.settleAfterOrderPaid(record.getOrderId());
            if (isAdminCheckoutPayment(record)) {
                Order paidOrder = orderService.getById(record.getOrderId());
                tryFinishTableAfterAdminCheckout(paidOrder, "NATIVE_QR");
            }
        }
    }

    /**
     * 判断微信支付是否来自管理端收款码。
     *
     * @author Henfon
     * @date 2026-07-13
     * @description 管理端 Native 支付记录不携带付款人 openid；小程序 JSAPI 支付记录始终携带 openid。
     * @param record 支付记录
     * @return true 表示管理端收款码支付，false 表示小程序订单支付
     */
    private boolean isAdminCheckoutPayment(PaymentRecord record) {
        return record != null && isBlank(record.getPayerOpenid());
    }

    /**
     * 批量标记订单项已支付
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 现金分单支付成功后，将对应订单项同步为已支付，避免重复分单。
     * @param orderItemIds 订单项ID列表
     */
    private void markOrderItemsPaid(List<Long> orderItemIds) {
        if (orderItemIds == null || orderItemIds.isEmpty()) {
            return;
        }

        LambdaUpdateWrapper<OrderItem> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(OrderItem::getId, orderItemIds)
                .set(OrderItem::getPaymentStatus, 2);
        orderItemMapper.update(null, updateWrapper);
    }

    /**
     * 标记订单剩余订单项已支付
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 整单付清后兜底刷新全部未支付订单项的支付状态，保持订单明细与订单总状态一致。
     * @param orderId 订单ID
     */
    private void markRemainingOrderItemsPaid(Long orderId) {
        if (orderId == null) {
            return;
        }

        LambdaUpdateWrapper<OrderItem> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OrderItem::getOrderId, orderId)
                .eq(OrderItem::getDeleted, 0)
                .set(OrderItem::getPaymentStatus, 2);
        orderItemMapper.update(null, updateWrapper);
    }

    /**
     * 记录订单操作日志。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 为支付、分单、回调等关键环节补充审计信息。
     */
    private void logOperation(Long orderId, String operationType, String reason, String detail) {
        OrderOperationLog opLog = new OrderOperationLog();
        opLog.setOrderId(orderId);
        opLog.setOperationType(operationType);

        try {
            opLog.setOperatorId(Long.valueOf(StpUtil.getLoginIdAsString()));
        } catch (Exception ignored) {
            // 支付回调等无登录态场景允许为空
        }

        if (opLog.getOperatorId() != null) {
            try {
                SysUser operator = sysUserMapper.selectById(opLog.getOperatorId());
                if (operator != null) {
                    opLog.setOperatorName(operator.getNickname() != null && !operator.getNickname().isBlank()
                            ? operator.getNickname()
                            : operator.getUsername());
                }
            } catch (Exception ignored) {
                // ignore
            }
        }

        if (opLog.getOperatorName() == null || opLog.getOperatorName().isBlank()) {
            try {
                opLog.setOperatorName(StpUtil.getLoginIdAsString());
            } catch (Exception ignored) {
                // 支付回调等无登录态场景允许为空
            }
        }

        if (opLog.getOperatorId() == null) {
            opLog.setOperatorId(SYSTEM_OPERATOR_ID);
        }
        if (opLog.getOperatorName() == null || opLog.getOperatorName().isBlank()) {
            opLog.setOperatorName(SYSTEM_OPERATOR_NAME);
        }

        opLog.setReason(reason);
        opLog.setDetail(detail);
        orderOperationLogMapper.insert(opLog);
    }

    /**
     * 解析 JSON。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 统一封装 JSON 解析异常，减少重复 try/catch。
     */
    private JsonNode readJson(String rawJson, String errorMessage) {
        try {
            return OBJECT_MAPPER.readTree(rawJson);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PAYMENT_INIT_FAILED, errorMessage + ": " + e.getMessage());
        }
    }

    /**
     * 序列化 JSON。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 将请求或缓存对象转成 JSON 字符串。
     */
    private String writeJson(Object data) {
        try {
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PAYMENT_INIT_FAILED, "JSON 序列化失败: " + e.getMessage());
        }
    }

    /**
     * 安全读取 JSON 文本字段。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 兼容缺失字段和空字符串场景。
     */
    private String textOrNull(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return isBlank(text) ? null : text;
    }

    /**
     * 忽略大小写读取请求头。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 兼容 Servlet 容器不同的请求头大小写行为。
     */
    private String getHeaderIgnoreCase(Map<String, String> headers, String headerName) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (headerName.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 金额转分。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 微信支付统一使用分为最小货币单位。
     */
    private int toFen(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).intValueExact();
    }

    /**
     * 生成支付标题。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 控制标题长度，避免超出微信支付字段限制。
     */
    private String buildPaymentTitle(Order order) {
        String title = "点餐订单-" + order.getOrderNo();
        return title.length() > 127 ? title.substring(0, 127) : title;
    }

    /**
     * 生成随机串。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 微信支付请求、签名和小程序调起支付都需要唯一随机串。
     */
    private String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 判空。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 简化字符串判空判断。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 返回首个非空字符串。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 便于多配置、多来源兜底取值。
     */
    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 脱敏输出。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 日志里只展示关键值前后少量字符，避免泄露敏感配置。
     */
    private String maskValue(String value) {
        if (isBlank(value)) {
            return "<empty>";
        }
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    private static class WechatQueryResult {
        private boolean success;
        private boolean closed;
        private String transactionId;
        private String rawResponse;
    }

    private static class OrderPaymentApplyResult {
        private final boolean applied;
        private final BigDecimal newPaidAmount;
        private final BigDecimal remainingAmount;
        private final boolean fullyPaid;

        private OrderPaymentApplyResult(boolean applied, BigDecimal newPaidAmount, BigDecimal remainingAmount, boolean fullyPaid) {
            this.applied = applied;
            this.newPaidAmount = newPaidAmount;
            this.remainingAmount = remainingAmount;
            this.fullyPaid = fullyPaid;
        }

        private static OrderPaymentApplyResult notApplied() {
            return new OrderPaymentApplyResult(false, BigDecimal.ZERO, BigDecimal.ZERO, false);
        }

        private static OrderPaymentApplyResult applied(BigDecimal newPaidAmount, BigDecimal remainingAmount, boolean fullyPaid) {
            return new OrderPaymentApplyResult(true, newPaidAmount, remainingAmount, fullyPaid);
        }
    }
}
