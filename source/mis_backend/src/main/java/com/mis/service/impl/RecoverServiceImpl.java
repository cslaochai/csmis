package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.config.UserContext;
import com.mis.dto.RecoverHandleDTO;
import com.mis.dto.RecoverQueryDTO;
import com.mis.entity.MifRecover;
import com.mis.mapper.MifRecoverMapper;
import com.mis.service.RecoverService;
import com.mis.vo.ActionVO;
import com.mis.vo.RecoverVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecoverServiceImpl implements RecoverService {

    private final MifRecoverMapper recoverMapper;

    @Override
    public PageVO<RecoverVO> recoverPage(RecoverQueryDTO q) {
        Page<MifRecover> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifRecover> w = new LambdaQueryWrapper<>();
        if (hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            w.and(x -> x.like(MifRecover::getRecoverNo, kw)
                    .or().like(MifRecover::getSourceNo, kw)
                    .or().like(MifRecover::getTargetOrgName, kw));
        }
        if (q.getSourceType() != null) {
            w.eq(MifRecover::getSourceType, q.getSourceType());
        }
        if (hasText(q.getStatus())) {
            w.eq(MifRecover::getStatus, q.getStatus());
        }
        w.orderByDesc(MifRecover::getId);
        Page<MifRecover> res = recoverMapper.selectPage(page, w);
        List<RecoverVO> vos = res.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public RecoverVO getDetail(Long id) {
        MifRecover r = recoverMapper.selectById(id);
        return r == null ? null : toVO(r);
    }

    private RecoverVO toVO(MifRecover r) {
        RecoverVO vo = new RecoverVO();
        BeanUtils.copyProperties(r, vo);
        return vo;
    }

    @Override
    public ActionVO doRecover(RecoverHandleDTO dto) {
        MifRecover r = recoverMapper.selectById(dto.getId());
        if (r == null) {
            return ActionVO.fail("追回单不存在");
        }
        if (!"待追回".equals(r.getStatus())) {
            return ActionVO.fail("仅待追回状态可确认追回");
        }
        r.setStatus("已追回");
        r.setRecoverTime(LocalDateTime.now());
        r.setHandler(UserContext.username());
        r.setRemark(dto.getRemark());
        recoverMapper.updateById(r);
        return ActionVO.ok();
    }

    @Override
    public ActionVO writeOff(RecoverHandleDTO dto) {
        MifRecover r = recoverMapper.selectById(dto.getId());
        if (r == null) {
            return ActionVO.fail("追回单不存在");
        }
        if (!"待追回".equals(r.getStatus())) {
            return ActionVO.fail("仅待追回状态可核销");
        }
        r.setStatus("已核销");
        r.setRemark(dto.getRemark());
        recoverMapper.updateById(r);
        return ActionVO.ok();
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
