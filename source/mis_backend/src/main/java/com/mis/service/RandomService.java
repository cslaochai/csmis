package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.RandomResultHandleDTO;
import com.mis.dto.RandomResultQueryDTO;
import com.mis.dto.RandomTaskQueryDTO;
import com.mis.dto.RandomTaskUpsertDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.RandomResultVO;
import com.mis.vo.RandomTaskVO;

public interface RandomService {

    /* ---------------- 抽查任务 ---------------- */

    PageVO<RandomTaskVO> pageTask(RandomTaskQueryDTO q);

    ActionVO upsertTask(RandomTaskUpsertDTO dto);

    /**
     * 双随机抽取：随机抽取检查对象（定点机构）与检查人员，结果落库并记录抽取时间。
     */
    ActionVO draw(Long taskId);

    /** 结果公开（把该任务下已检查结果全部公开） */
    ActionVO publish(Long taskId);

    /* ---------------- 抽取结果 ---------------- */

    PageVO<RandomResultVO> pageResult(RandomResultQueryDTO q);

    ActionVO handleResult(RandomResultHandleDTO dto);
}
