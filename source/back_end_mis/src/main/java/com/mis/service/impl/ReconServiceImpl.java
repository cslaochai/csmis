package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.BizPageParam;
import com.mis.entity.MssRecon;
import com.mis.mapper.MssReconMapper;
import com.mis.service.ReconService;
import com.mis.vo.ActionVO;
import com.mis.vo.ReconVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReconServiceImpl implements ReconService {

    private final MssReconMapper reconMapper;

    @Override
    public PageVO<ReconVO> reconPage(BizPageParam q) {
        Page<MssRecon> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MssRecon> w = new LambdaQueryWrapper<>();
        w.orderByAsc(MssRecon::getPeriod);
        Page<MssRecon> res = reconMapper.selectPage(page, w);
        List<ReconVO> vos = res.getRecords().stream().map(r -> {
            ReconVO vo = new ReconVO();
            BeanUtils.copyProperties(r, vo);
            return vo;
        }).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO doReconcile(String period) {
        if (period == null || period.isBlank()) {
            return ActionVO.fail("缺少账期");
        }
        MssRecon r = reconMapper.selectOne(new LambdaQueryWrapper<MssRecon>()
                .eq(MssRecon::getPeriod, period).last("limit 1"));
        if (r == null) {
            return ActionVO.fail("账期不存在");
        }
        if (!"待对账".equals(r.getStatus())) {
            return ActionVO.fail("该账期已对账");
        }
        r.setReceivedFromFund(r.getInsurancePay());
        r.setDiff(BigDecimal.ZERO.setScale(2));
        r.setStatus("已对账");
        reconMapper.updateById(r);
        return ActionVO.ok();
    }
}
