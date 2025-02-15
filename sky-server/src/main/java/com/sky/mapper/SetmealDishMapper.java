package com.sky.mapper;

import java.util.List;

public interface SetmealDishMapper {
    /**
     *
     * @param dishIds
     * @return
     */
    Integer countByDishId(List<Long> dishIds);
}
