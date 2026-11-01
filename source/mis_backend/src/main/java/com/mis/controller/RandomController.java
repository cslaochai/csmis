package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.RandomResultHandleDTO;
import com.mis.dto.RandomResultQueryDTO;
import com.mis.dto.RandomTaskQueryDTO;
import com.mis.dto.RandomTaskUpsertDTO;
import com.mis.service.RandomService;
import com.mis.vo.ActionVO;
import com.mis.vo.RandomResultVO;
import com.mis.vo.RandomTaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 双随机一公开
 */
@RestController
@RequestMapping("/supervise/random")
@RequiredArgsConstructor
public class RandomController {

    private final RandomService randomService;

    @GetMapping("/task/listPage")
    public Result<PageVO<RandomTaskVO>> pageTask(@Valid RandomTaskQueryDTO q) {
        return Result.success(randomService.pageTask(q));
    }

    @PostMapping("/task/upsert")
    public Result<ActionVO> upsertTask(@RequestBody @Valid RandomTaskUpsertDTO dto) {
        return Result.success(randomService.upsertTask(dto));
    }

    /** 随机抽取检查对象与检查人员 */
    @PostMapping("/task/draw")
    public Result<ActionVO> draw(@RequestBody Map<String, Object> body) {
        Long taskId = Long.valueOf(String.valueOf(body.get("taskId")));
        return Result.success(randomService.draw(taskId));
    }

    /** 抽查结果公开 */
    @PostMapping("/task/publish")
    public Result<ActionVO> publish(@RequestBody Map<String, Object> body) {
        Long taskId = Long.valueOf(String.valueOf(body.get("taskId")));
        return Result.success(randomService.publish(taskId));
    }

    @GetMapping("/result/listPage")
    public Result<PageVO<RandomResultVO>> pageResult(@Valid RandomResultQueryDTO q) {
        return Result.success(randomService.pageResult(q));
    }

    @PostMapping("/result/handle")
    public Result<ActionVO> handleResult(@RequestBody @Valid RandomResultHandleDTO dto) {
        return Result.success(randomService.handleResult(dto));
    }
}
