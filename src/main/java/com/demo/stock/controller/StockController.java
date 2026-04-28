package com.demo.stock.controller;

import com.demo.stock.mapper.StockMapper;
import com.demo.stock.mapper.OutboxMessageMapper;
import com.demo.stock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/stock")
public class StockController {

    @Autowired private StockService        stockService;
    @Autowired private StockMapper         stockMapper;
    @Autowired private OutboxMessageMapper outboxMapper;

    /**
     * 扣减库存接口
     * POST /stock/deduct?userId=U001&productId=P001&quantity=2
     */
    @PostMapping("/deduct")
    public Map<String, Object> deduct(
            @RequestParam String userId,
            @RequestParam String productId,
            @RequestParam int    quantity) {

        Map<String, Object> result = new HashMap<>();
        try {
            String messageId = stockService.deductStock(userId, productId, quantity);
            result.put("success",   true);
            result.put("messageId", messageId);
            result.put("msg",       "库存扣减成功，消息已入库，等待异步发送到MQ");
        } catch (Exception e) {
            result.put("success", false);
            result.put("msg",     e.getMessage());
        }
        return result;
    }

    /**
     * 查看商品库存（调试用）
     */
    @GetMapping("/{productId}")
    public Object getStock(@PathVariable String productId) {
        return stockMapper.findByProductId(productId);
    }

    /**
     * 查看本地消息表状态（调试用，可以直接在浏览器看消息是否SENT）
     */
    @GetMapping("/outbox/pending")
    public Object getPendingMessages() {
        return outboxMapper.findPendingMessages();
    }
}
