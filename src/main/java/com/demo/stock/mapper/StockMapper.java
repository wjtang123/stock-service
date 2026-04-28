package com.demo.stock.mapper;

import com.demo.stock.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockMapper {

    Stock findByProductId(@Param("productId") String productId);

    /**
     * 乐观锁更新库存
     * WHERE product_id=#{productId} AND version=#{version}
     * 返回影响行数：0说明被别人抢先更新了（并发冲突），需要重试
     */
    int deductWithVersion(@Param("productId") String productId,
                          @Param("quantity")  int quantity,
                          @Param("version")   int version);
}
