package com.scaffold.modules.print.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.print.entity.Printer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 打印机 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface PrinterMapper extends BaseMapper<Printer> {
}
