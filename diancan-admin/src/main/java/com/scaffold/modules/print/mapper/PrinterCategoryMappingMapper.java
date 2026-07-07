package com.scaffold.modules.print.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.print.entity.PrinterCategoryMapping;
import org.apache.ibatis.annotations.Mapper;

/**
 * 打印机-分类映射 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface PrinterCategoryMappingMapper extends BaseMapper<PrinterCategoryMapping> {
}
