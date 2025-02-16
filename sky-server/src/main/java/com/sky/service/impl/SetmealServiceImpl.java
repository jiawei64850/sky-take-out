package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Transactional
    public void addSetmeal(SetmealDTO setmealDTO) {
        log.info("新增套餐: {}", setmealDTO);
        // 1. construct the data of basic information
        Setmeal setmeal = new Setmeal();
        // copy the attribute
        BeanUtils.copyProperties(setmealDTO, setmeal);
        log.info("setmeal before: {}", setmeal);
        // call the mapper to store method
        setmealMapper.insert(setmeal);
        // 2. construct the setmeal dish list
        List<SetmealDish> dishList = setmealDTO.getSetmealDishes();
        log.info("dishList before: {}", dishList);
        // 2.1 associate the id of dish
        dishList.forEach(dish -> {
            dish.setSetmealId(setmeal.getId());
        });
        log.info("dishList after: {}", dishList);
        // 2.2 call the mapper to store method with batch insert
        setmealDishMapper.insertBatch(dishList);
    }

    /**
     * 分页查询套餐列表
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        // 1. set the parameter to split the page
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        // 2. call the mapper to store the method
        Page<SetmealVO> page = setmealMapper.list(setmealPageQueryDTO);
        // 3. get the PageResult encapsulation object and return it
        return new PageResult(page.getTotal(), page.getResult());
    }


    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    public SetmealVO getById(Long id) {
        System.out.println("id: " + id);
        // 1. construct the setmealVO
        SetmealVO setmealVO = new SetmealVO();
        // 2. add the basic information for the setmeal, and encapsulate it into setmealVO
        Setmeal setmeal = setmealMapper.selectById(id);
        BeanUtils.copyProperties(setmeal, setmealVO);
        System.out.println("setmeal: " + setmeal);
        // 3. get the dish list based on setmealId, and also encapsulate it into setmealVO
        List<SetmealDish> setmealDishes = setmealDishMapper.selectBySetmealId(id);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Transactional // 0. multiple operation
    public void update(SetmealDTO setmealDTO) {
        // 1. construct setmeal
        Setmeal setmeal = new Setmeal();
        // 2. copy the properties
        BeanUtils.copyProperties(setmealDTO, setmeal);
        // 3. update the basic information of setmeal
        setmealMapper.update(setmeal);
        // 4. update the associated dish for setmeal
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());
        List<SetmealDish> dishList = setmealDTO.getSetmealDishes();
        if (dishList != null && dishList.size() > 0) {
            dishList.forEach(dish -> {
                dish.setSetmealId(setmeal.getId());
            });
            setmealDishMapper.insertBatch(dishList);
        }
    }
}
