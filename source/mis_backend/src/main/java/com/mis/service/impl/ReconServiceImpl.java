package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.BizPageParam;
import com.mis.entity.MisRecon;
import com.mis.mapper.MisReconMapper;
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

    private final MisReconMapper reconMapper;

    @Override
    public PageVO<ReconVO> reconPage(BizPageParam q) {
        Page<MisRecon> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MisRecon> w = new LambdaQueryWrapper<>();
        w.orderByAsc(MisRecon::getPeriod);
        Page<MisRecon> res = reconMapper.selectPage(page, w);
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
        MisRecon r = reconMapper.selectOne(new LambdaQueryWrapper<MisRecon>()
                .eq(MisRecon::getPeriod, period).last("limit 1"));
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
