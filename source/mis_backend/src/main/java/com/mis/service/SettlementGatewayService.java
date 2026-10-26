package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.CancelSettlementDTO;
import com.mis.dto.UploadSettlementDTO;
import com.mis.vo.DayBillItemVO;
import com.mis.vo.ReceiptVO;
import com.mis.vo.SettlementVO;

import java.util.List;

/**
 * 医保前置网关：2304 上传 / 2305 撤销 / 日对账
 */
public interface SettlementGatewayService {

    /** 2304 结算信息上传：校验、独立落库、独立核定，返回回执 */
    ReceiptVO receive2304(UploadSettlementDTO dto, String rawPayload);

    /** 2304 上传原文入口：解析失败按前置机口径回 infcode=-1 */
    ReceiptVO receive2304Raw(String body);

    /** 2305 结算信息撤销：冲正平台侧结算单 */
    ReceiptVO receive2305(CancelSettlementDTO dto, String rawPayload);

    /** 2305 撤销原文入口：解析失败按前置机口径回 infcode=-1 */
    ReceiptVO receive2305Raw(String body);

    /** 拉取账期内“对方账”（仅有效未撤销） */
    List<DayBillItemVO> dayBill(String billDate);

    /** 管理端：平台侧结算单分页（可按患者姓名/结算单号/账单号/交易流水过滤） */
    PageVO<SettlementVO> settlementPage(String month, Integer status, String patientName,
                                        String settlementNo, String billNo, String tradeNo,
                                        int pageNum, int pageSize);

    /** 管理端：平台侧结算单明细（含 2304 原始报文） */
    SettlementVO getById(Long id);
}
