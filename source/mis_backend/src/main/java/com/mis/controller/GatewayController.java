package com.mis.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mis.common.base.Result;
import com.mis.common.base.PageVO;
import com.mis.dto.CancelSettlementDTO;
import com.mis.dto.GatewayRealTimeSettleDTO;
import com.mis.dto.UploadSettlementDTO;
import com.mis.entity.MssCatalog;
import com.mis.mapper.MssCatalogMapper;
import com.mis.service.GatewayRealTimeSettleService;
import com.mis.service.SettlementGatewayService;
import com.mis.vo.DayBillItemVO;
import com.mis.vo.GatewaySettleVO;
import com.mis.vo.ReceiptVO;
import com.mis.vo.SettlementVO;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 医保前置网关（HIS 前置机对端）：2304 上传 / 2305 撤销 / 日对账
 */
@RestController
public class GatewayController {

    private final SettlementGatewayService gatewayService;
    private final GatewayRealTimeSettleService realTimeSettleService;
    private final MssCatalogMapper catalogMapper;

    public GatewayController(SettlementGatewayService gatewayService,
                             GatewayRealTimeSettleService realTimeSettleService,
                             MssCatalogMapper catalogMapper) {
        this.gatewayService = gatewayService;
        this.realTimeSettleService = realTimeSettleService;
        this.catalogMapper = catalogMapper;
    }

    /** 2304 结算信息上传（接收 HIS 报盘原文）；报文非法时按前置机口径回 infcode=-1，而非 HTTP 5xx */
    @PostMapping("/gateway/2304")
    public Result<ReceiptVO> upload2304(@RequestBody String body) {
        return Result.success(gatewayService.receive2304Raw(body));
    }

    /** 2305 结算信息撤销 */
    @PostMapping("/gateway/2305")
    public Result<ReceiptVO> upload2305(@RequestBody String body) {
        return Result.success(gatewayService.receive2305Raw(body));
    }

    /** 拉取账期内平台侧“对方账” */
    @GetMapping("/gateway/dayBill")
    public Result<List<DayBillItemVO>> dayBill(@RequestParam String billDate) {
        return Result.success(gatewayService.dayBill(billDate));
    }

    /**
     * 2201 费用预结算（HIS 收费窗口实时调，不落库）。
     * 传患者身份 + 费用明细，返回统筹/个账/现金 splits，供收费员与患者核对。
     */
    @PostMapping("/gateway/2201")
    public Result<GatewaySettleVO> preSettle(@RequestBody GatewayRealTimeSettleDTO dto) {
        return Result.success(realTimeSettleService.preSettle(dto));
    }

    /**
     * 2301 门诊正式结算（HIS 收费窗口实时调，落库 + 扣个账）。
     * 同一 tradeNo 幂等；成功回医保结算单号 receiptNo 与 splits。
     */
    @PostMapping("/gateway/2301")
    public Result<GatewaySettleVO> formalSettle(@RequestBody GatewayRealTimeSettleDTO dto) {
        return Result.success(realTimeSettleService.formalSettle(dto));
    }

    /** 管理端：平台侧结算单分页（机构 2304 报盘接收台账，可按患者/单号过滤） */
    @GetMapping("/admin/settlement/listPage")
    public Result<PageVO<SettlementVO>> settlementListPage(@RequestParam(required = false) String month,
                                                          @RequestParam(required = false) Integer status,
                                                          @RequestParam(required = false) String patientName,
                                                          @RequestParam(required = false) String settlementNo,
                                                          @RequestParam(required = false) String billNo,
                                                          @RequestParam(required = false) String tradeNo,
                                                          @RequestParam(defaultValue = "1") int pageNum,
                                                          @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(gatewayService.settlementPage(month, status, patientName, settlementNo, billNo, tradeNo, pageNum, pageSize));
    }

    /** 管理端：平台侧结算单明细（含 2304 原始报文） */
    @GetMapping("/admin/settlement/getById")
    public Result<SettlementVO> settlementById(@RequestParam Long id) {
        return Result.success(gatewayService.getById(id));
    }

    // ================================================================
    // 目录下载（国家医保接口 1301/1302/1303：药品/诊疗项目/医用耗材）
    // 供医院前置机（csmig）定期拉取医保目录，离线缓存后做院内↔医保对照
    // ================================================================

    /** 1301 药品目录下载（西药/中成药/中草药） */
    @PostMapping("/gateway/1301")
    public Result<Map<String, Object>> download1301() {
        return catalogDownload("DRUG");
    }

    /** 1302 诊疗项目目录下载 */
    @PostMapping("/gateway/1302")
    public Result<Map<String, Object>> download1302() {
        return catalogDownload("DIAG");
    }

    /** 1303 医用耗材目录下载 */
    @PostMapping("/gateway/1303")
    public Result<Map<String, Object>> download1303() {
        return catalogDownload("MAT");
    }

    private Result<Map<String, Object>> catalogDownload(String dictType) {
        LambdaQueryWrapper<MssCatalog> q = new LambdaQueryWrapper<>();
        switch (dictType) {
            case "DRUG" -> q.in(MssCatalog::getCategory, "西药", "中成药", "中草药");
            case "DIAG" -> q.eq(MssCatalog::getCategory, "诊疗项目");
            case "MAT" -> q.eq(MssCatalog::getCategory, "医用耗材");
            default -> throw new IllegalArgumentException("不支持的目录类型：" + dictType);
        }
        List<MssCatalog> list = catalogMapper.selectList(q.orderByAsc(MssCatalog::getYbCode));
        Map<String, Object> data = new HashMap<>();
        data.put("infcode", 0);
        data.put("errMsg", null);
        data.put("catalogType", dictType);
        data.put("total", list.size());
        data.put("list", list.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("ybCode", c.getYbCode());
            m.put("name", c.getName());
            m.put("category", c.getCategory());
            m.put("spec", c.getSpec());
            m.put("unit", c.getUnit());
            m.put("payClass", c.getPayClass());
            m.put("limitPrice", c.getLimitPrice());
            m.put("status", c.getStatus());
            return m;
        }).toList());
        return Result.success(data);
    }
}
