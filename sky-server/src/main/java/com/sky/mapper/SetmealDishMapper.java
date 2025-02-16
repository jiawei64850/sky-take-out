package com.sky.mapper;

import com.sky.entity.SetmealDish;

import java.util.List;

public interface SetmealDishMapper {
    /**
     *
     * @param dishIds
     * @return
     */
    Integer countByDishId(List<Long> dishIds);

    /**
     * 批量插入菜品到所属套餐中
     * @param dishList
     */
    void insertBatch(List<SetmealDish> dishList);

    /**
     * 根据套餐id查询菜品
     * @param setmealId
     * @return
     */
    List<SetmealDish> selectBySetmealId(Long setmealId);

    /**
     * 根据套餐id删除菜品
     * @param SetmealId
     */
    void deleteBySetmealId(Long SetmealId);
}
