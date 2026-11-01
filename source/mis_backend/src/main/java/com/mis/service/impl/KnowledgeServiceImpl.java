package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.KnowledgeQueryDTO;
import com.mis.dto.KnowledgeUpsertDTO;
import com.mis.entity.MifKnowledge;
import com.mis.mapper.MifKnowledgeMapper;
import com.mis.service.KnowledgeService;
import com.mis.vo.ActionVO;
import com.mis.vo.KnowledgeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final MifKnowledgeMapper knowledgeMapper;

    @Override
    public PageVO<KnowledgeVO> page(KnowledgeQueryDTO q) {
        Page<MifKnowledge> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifKnowledge> w = new LambdaQueryWrapper<>();
        if (hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            w.and(x -> x.like(MifKnowledge::getKbName, kw)
                    .or().like(MifKnowledge::getKbCode, kw));
        }
        if (hasText(q.getKbType())) {
            w.eq(MifKnowledge::getKbType, q.getKbType());
        }
        if (q.getStatus() != null) {
            w.eq(MifKnowledge::getStatus, q.getStatus());
        }
        w.orderByAsc(MifKnowledge::getId);
        Page<MifKnowledge> res = knowledgeMapper.selectPage(page, w);
        List<KnowledgeVO> vos = res.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO upsert(KnowledgeUpsertDTO dto) {
        Long dup = knowledgeMapper.selectCount(new LambdaQueryWrapper<MifKnowledge>()
                .eq(MifKnowledge::getKbCode, dto.getKbCode())
                .ne(dto.getId() != null, MifKnowledge::getId, dto.getId()));
        if (dup != null && dup > 0) {
            return ActionVO.fail("知识编码已存在");
        }
        MifKnowledge k;
        if (dto.getId() == null) {
            k = new MifKnowledge();
            k.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        } else {
            k = knowledgeMapper.selectById(dto.getId());
            if (k == null) {
                return ActionVO.fail("知识条目不存在");
            }
        }
        k.setKbCode(dto.getKbCode());
        k.setKbName(dto.getKbName());
        k.setKbType(dto.getKbType());
        k.setContent(dto.getContent());
        k.setSource(dto.getSource());
        if (dto.getStatus() != null) {
            k.setStatus(dto.getStatus());
        }
        if (dto.getId() == null) {
            knowledgeMapper.insert(k);
        } else {
            knowledgeMapper.updateById(k);
        }
        return ActionVO.ok();
    }

    @Override
    public ActionVO changeStatus(Long id, Integer status) {
        MifKnowledge k = knowledgeMapper.selectById(id);
        if (k == null) {
            return ActionVO.fail("知识条目不存在");
        }
        k.setStatus(status);
        knowledgeMapper.updateById(k);
        return ActionVO.ok();
    }

    @Override
    public KnowledgeVO detail(Long id) {
        MifKnowledge k = knowledgeMapper.selectById(id);
        return k == null ? null : toVO(k);
    }

    private KnowledgeVO toVO(MifKnowledge k) {
        KnowledgeVO vo = new KnowledgeVO();
        BeanUtils.copyProperties(k, vo);
        return vo;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
