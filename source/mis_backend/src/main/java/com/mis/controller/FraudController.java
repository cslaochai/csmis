package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.FraudClueHandleDTO;
import com.mis.dto.FraudClueQueryDTO;
import com.mis.dto.FraudModelQueryDTO;
import com.mis.dto.FraudModelStatusDTO;
import com.mis.dto.FraudModelUpsertDTO;
import com.mis.service.FraudService;
import com.mis.vo.ActionVO;
import com.mis.vo.FraudClueVO;
import com.mis.vo.FraudModelVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/supervise/fraud")
public class FraudController {

    private final FraudService fraudService;

    public FraudController(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    @GetMapping("/model/listPage")
    public Result<PageVO<FraudModelVO>> pageModel(@Valid FraudModelQueryDTO q) {
        return Result.success(fraudService.pageModel(q));
    }

    @PostMapping("/model/upsert")
    public Result<ActionVO> upsertModel(@RequestBody @Valid FraudModelUpsertDTO dto) {
        return Result.success(fraudService.upsertModel(dto));
    }

    @PostMapping("/model/status")
    public Result<ActionVO> changeModelStatus(@RequestBody @Valid FraudModelStatusDTO dto) {
        return Result.success(fraudService.changeModelStatus(dto));
    }

    @GetMapping("/clue/listPage")
    public Result<PageVO<FraudClueVO>> pageClue(@Valid FraudClueQueryDTO q) {
        return Result.success(fraudService.pageClue(q));
    }

    @PostMapping("/clue/run")
    public Result<ActionVO> run(@RequestBody(required = false) java.util.Map<String, Long> body) {
        Long modelId = body == null ? null : body.get("modelId");
        return Result.success(fraudService.run(modelId));
    }

    @PostMapping("/clue/handle")
    public Result<ActionVO> handleClue(@RequestBody @Valid FraudClueHandleDTO dto) {
        return Result.success(fraudService.handleClue(dto));
    }
}
