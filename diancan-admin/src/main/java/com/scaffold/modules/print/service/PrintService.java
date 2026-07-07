package com.scaffold.modules.print.service;

import com.scaffold.modules.print.dto.CategoryMappingDTO;
import com.scaffold.modules.print.dto.PrinterCreateDTO;
import com.scaffold.modules.print.dto.PrinterUpdateDTO;
import com.scaffold.modules.print.vo.PrinterVO;

import java.util.List;

/**
 * 打印服务接口
 *
 * @author Henfon
 */
public interface PrintService {

    /**
     * 获取所有打印机列表（含关联的分类ID）
     *
     * @return 打印机列表
     */
    List<PrinterVO> listPrinters();

    /**
     * 创建打印机
     *
     * @param dto 创建参数
     */
    void createPrinter(PrinterCreateDTO dto);

    /**
     * 更新打印机
     *
     * @param dto 更新参数
     */
    void updatePrinter(PrinterUpdateDTO dto);

    /**
     * 删除打印机
     *
     * @param id 打印机ID
     */
    void deletePrinter(Long id);

    /**
     * 批量更新打印机-分类映射
     *
     * @param dto 映射列表
     */
    void updateCategoryMapping(CategoryMappingDTO dto);

    /**
     * 打印订单（分单打印：按菜品分类拆分至对应打印机）
     *
     * @param orderId 订单ID
     */
    void printOrder(Long orderId);

    /**
     * 重新打印订单
     *
     * @param orderId 订单ID
     */
    void reprintOrder(Long orderId);
}
