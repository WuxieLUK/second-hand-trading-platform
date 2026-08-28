package com.itheima.controller;

import com.itheima.anno.PreAuthorize;
import com.itheima.pojo.Result;
import com.itheima.service.AiService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@Slf4j
public class AIController {

    @Resource
    private AiService aiService;

    // 原有流式对话接口（保留）
    @PreAuthorize("/ai/chat")
    @PostMapping(value = "/chat",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody String message) {
        StringBuilder context = new StringBuilder();
        Flux<String> response = aiService.getAiResponse(message, context.toString());
        return response;
    }

    // 修复：返回Result<String>，统一格式
    @PreAuthorize("/ai/generateGoodsDesc")
    @PostMapping("/generateGoodsDesc")
    public Result<String> generateGoodsDesc(@RequestBody Map<String, Object> params) {
        try {
            // 参数校验
            if (params == null || params.isEmpty()) {
                return Result.error("参数不能为空"); // 统一用Result.error返回错误
            }

            Object keywordsObj = params.get("keywords");
            Object goodsNameObj = params.get("goodsName");
            Object isNewObj = params.get("isNew");
            Object sellPriceObj = params.get("sellPrice");

            if (keywordsObj == null || goodsNameObj == null || isNewObj == null || sellPriceObj == null) {
                return Result.error("缺少必要参数：keywords, goodsName, isNew, sellPrice");
            }

            String keywords = keywordsObj.toString().trim();
            String goodsName = goodsNameObj.toString().trim();
            Integer isNew = Integer.parseInt(isNewObj.toString());
            BigDecimal sellPrice = new BigDecimal(sellPriceObj.toString());

            // 额外校验
            if (keywords.isEmpty() || goodsName.isEmpty()) {
                return Result.error("关键词和商品名称不能为空");
            }
            if (isNew < 0 || isNew > 10) {
                return Result.error("新旧程度参数无效（0-10）");
            }
            if (sellPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error("价格必须大于 0");
            }

            // 调用AI服务生成描述，成功则返回Result.success
            String desc = aiService.generateGoodsDesc(keywords, goodsName, isNew, sellPrice);
            return Result.success(desc);

        } catch (NumberFormatException e) {
            log.error("参数格式错误", e);
            return Result.error("参数格式错误：请检查数字类型参数（新旧程度/价格）");
        } catch (Exception e) {
            log.error("生成商品描述失败", e);
            return Result.error("生成失败，请重试！");
        }
    }
}