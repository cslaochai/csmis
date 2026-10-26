package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.CatalogQueryDTO;
import com.mis.dto.MappingMapDTO;
import com.mis.dto.MappingQueryDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.CatalogStatsVO;
import com.mis.vo.CatalogVO;
import com.mis.vo.ItemMappingVO;

/**
 * 医保三大目录 + 院内项目对照
 */
public interface CatalogService {

    PageVO<CatalogVO> catalogPage(CatalogQueryDTO query);

    CatalogStatsVO catalogStats();

    PageVO<ItemMappingVO> mappingPage(MappingQueryDTO query);

    ActionVO map(MappingMapDTO dto);

    ActionVO unmap(Long id);

    ActionVO autoMatch();
}
