package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.anno.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;


public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 新增菜品
     * @param dish
     */
    @AutoFill(OperationType.INSERT) // fill the common fields automatically
//    @Options(useGeneratedKeys = true, keyProperty = "id") //  get the prime key and assign it to attribute of id
//    @Insert("insert into dish values (null, #{name}, #{categoryId}, #{price}, #{image}, " +
//            "#{description}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    void insert(Dish dish);

    /**
     * 分页查询菜品列表
     * @param dishPageQueryDTO
     * @return
     */
    Page<DishVO> list(DishPageQueryDTO dishPageQueryDTO);
}
