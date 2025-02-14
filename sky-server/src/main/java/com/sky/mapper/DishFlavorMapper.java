package com.sky.mapper;

import com.sky.anno.AutoFill;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;

import java.util.List;

// omit the anno of mapper if activation class have already added the anno @MapperScan
public interface DishFlavorMapper {

    /**
     * 批量插入口味列表数据
     * @param dishFlavorList
     */
    void insertBatch(List<DishFlavor> dishFlavorList);
}
