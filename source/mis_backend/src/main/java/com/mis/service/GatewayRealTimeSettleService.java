package com.mis.service;

import com.mis.dto.GatewayRealTimeSettleDTO;
import com.mis.vo.GatewaySettleVO;

/**
 * 网关实时结算（HIS 收费窗口直调，区别于 2304 事后报盘）。
 *
 * <ul>
 *   <li>2201 预结算：只算 splits 不落库，给收费员看「医保报多少、个账扣多少、现金收多少」</li>
 *   <li>2301 正式结算：幂等落 mss_settlement、扣参保人个账、出医保结算单号</li>
 * </ul>
 */
public interface GatewayRealTimeSettleService {

    GatewaySettleVO preSettle(GatewayRealTimeSettleDTO dto);

    GatewaySettleVO formalSettle(GatewayRealTimeSettleDTO dto);
}
