package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    DishMapper dishMapper;
    @Autowired
    SetmealMapper setmealMapper;
    @Autowired
    SetmealDishMapper setmealDishMapper;
    @Autowired
    ShoppingCartMapper shoppingCartMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    public void addCart(ShoppingCartDTO shoppingCartDTO) {
        // 1. create shoppingCart object
        ShoppingCart shoppingCart = new ShoppingCart();
        // 2. copy the attribute from dto (lightly)
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        // 3. check whether products already exist or not
        // 3.1 set userId due to only check user's shopping cart of their own
        shoppingCart.setUserId(BaseContext.getCurrentId()) ;
        ShoppingCart cart = shoppingCartMapper.selectBy(shoppingCart);
        if (cart == null) { // carts contains the data of this product
            // 4. add the missed attribute into object
            // 4.1 check whether adding dish or setmeal
            if (shoppingCartDTO.getDishId() != null) {
                // search for dish table based on dish id
                Dish dish = dishMapper.selectById(shoppingCartDTO.getDishId());
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());

            } else if (shoppingCartDTO.getSetmealId() != null) {
                // search for dish table based on dish id
                Setmeal setmeal = setmealMapper.selectById(shoppingCartDTO.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            // 4.3 put data into shopping_cart table
            shoppingCartMapper.insert(shoppingCart);
        } else { //  carts didn't contain the data of this product
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.update(cart);
        }

        // 5. put the product (from user) into shopping_cart table
    }

    /**
     * 查询购物车
     * @return
     */
    public List<ShoppingCart> list() {
        // !!noticed: only working with user's shopping cart of their own
        return shoppingCartMapper.list(BaseContext.getCurrentId());
    }

    /**
     * 清空购物车
     */
    public void clean() {
        // !!noticed: only working with user's shopping cart of their own
        shoppingCartMapper.clean(BaseContext.getCurrentId());
    }

    /**
     * 删除购物车中一个商品
     * @param shoppingCartDTO
     */
    public void subCart(ShoppingCartDTO shoppingCartDTO) {
        // 1. create shoppingCart object
        ShoppingCart shoppingCart = new ShoppingCart();
        // 2. copy the attribute from dto (lightly)
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        // 3. check whether products already exist or not
        // 3.1 set userId due to only check user's shopping cart of their own
        shoppingCart.setUserId(BaseContext.getCurrentId()) ;
        ShoppingCart cart = shoppingCartMapper.selectBy(shoppingCart);
        if (cart != null) {
            Integer number = cart.getNumber();
            if (number == 1) { // if number is 1, delete it directly
                shoppingCartMapper.deleteById(cart.getId());
            } else { // if number is not 1, subtract the amount with 1
                cart.setNumber(number - 1);
                shoppingCartMapper.update(cart);
            }
        }
    }
}
