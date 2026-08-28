package com.itheima.controller;

import com.itheima.anno.PreAuthorize;
import com.itheima.DTO.GoodsDTO;
import com.itheima.DTO.GoodsQueryDTO;
import com.itheima.pojo.Enum.GoodsStatusEnum;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.Result;
import com.itheima.service.GoodsService;
import com.itheima.util.QwenChatUtil;
import com.itheima.util.ThreadLocalUtil;
import com.itheima.vo.BuyerViewSellerVO;
import com.itheima.vo.GoodsDetailVO;
import com.itheima.vo.GoodsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/goods")
@Slf4j
@Tag(name = "商品接口", description = "二手商品管理接口")
public class GoodsController {
    @Resource
    private GoodsService goodsService;

    @Resource
    private QwenChatUtil qwenChatUtil;

    // 添加商品
    @PreAuthorize("/goods/add")
    @PostMapping("/add")
    @Operation(summary = "新增商品", description = "发布二手商品")
    public Result add(@RequestBody @Validated GoodsDTO goodsDTO) {
        try {
            goodsService.add(goodsDTO);
            return Result.success("商品发布成功");
        } catch (RuntimeException e) {
            log.error("新增商品失败", e);
            return Result.error(e.getMessage());
        }
    }

    // 后台分页查询商品（全状态）
    @PreAuthorize("/goods/list")
    @PostMapping("/list")
    @Operation(summary = "分页查询商品", description = "多条件筛选二手商品（后台管理）")
    public Result<PageBean<GoodsVO>> list(@RequestBody GoodsQueryDTO queryDTO) {
        try {
            PageBean<GoodsVO> pb = goodsService.alllist(queryDTO);
            return Result.success(pb);
        } catch (Exception e) {
            log.error("分页查询商品失败", e);
            return Result.error("查询商品失败：" + e.getMessage());
        }
    }

    // 商品详情
    @PostMapping("/detail/{id}")
    @Operation(summary = "商品详情", description = "查询商品详细信息")
    public Result<GoodsDetailVO> detail(@PathVariable Integer id) {
        try {
            if (id == null || id < 1) {
                return Result.error("商品ID不合法");
            }
            GoodsDetailVO vo = goodsService.findById(id);
            if (vo == null) {
                return Result.error("商品不存在");
            }
            return Result.success(vo);
        } catch (Exception e) {
            log.error("查询商品详情失败，ID：{}", id, e);
            return Result.error("查询商品详情失败：" + e.getMessage());
        }
    }

    // 修改商品
    @PreAuthorize("/goods/update")
    @PostMapping("/update")
    @Operation(summary = "修改商品", description = "更新商品信息")
    public Result update(@RequestBody @Validated GoodsDTO goodsDTO) {
        try {
            if (goodsDTO.getId() == null || goodsDTO.getId() < 1) {
                return Result.error("商品ID不能为空且必须为正整数");
            }
            goodsService.update(goodsDTO);
            return Result.success("商品更新成功");
        } catch (RuntimeException e) {
            log.error("修改商品失败", e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("修改商品异常", e);
            return Result.error("商品更新失败：" + e.getMessage());
        }
    }

    // 删除商品
    @PreAuthorize("/goods/delete")
    @PostMapping("/delete/{id}")
    @Operation(summary = "删除商品", description = "下架并删除商品（同步清理向量数据）")
    public Result delete(@PathVariable Integer id) {
        try {
            if (id == null || id < 1) {
                return Result.error("商品ID不合法");
            }
            goodsService.delete(id);
            return Result.success("商品删除成功");
        } catch (Exception e) {
            log.error("删除商品失败，ID：{}", id, e);
            return Result.error("商品删除失败：" + e.getMessage());
        }
    }

    // 更新商品状态
    @PreAuthorize("/goods/updateStatus")
    @PostMapping("/updateStatus/{id}/{status}")
    @Operation(summary = "更新商品状态", description = "在售/已售罄/下架/审核中/违规封禁")
    public Result updateStatus(
            @PathVariable Integer id,
            @Parameter(description = "状态码：1-在售 2-已售罄 3-下架 4-审核中 5-违规封禁") @PathVariable Integer status) {
        try {
            // 参数校验
            if (id == null || id < 1) {
                return Result.error("商品ID不合法");
            }
            // 校验状态码合法性
            boolean statusValid = false;
            for (GoodsStatusEnum enumItem : GoodsStatusEnum.values()) {
                if (enumItem.getCode().equals(status)) {
                    statusValid = true;
                    break;
                }
            }
            if (!statusValid) {
                return Result.error("商品状态码不合法，允许值：1-在售 2-已售罄 3-下架 4-审核中 5-违规封禁");
            }

            goodsService.updateStatus(id, status);
            return Result.success("商品状态更新成功");
        } catch (RuntimeException e) {
            log.error("更新商品状态失败，ID：{}，状态：{}", id, status, e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新商品状态异常，ID：{}，状态：{}", id, status, e);
            return Result.error("商品状态更新失败：" + e.getMessage());
        }
    }

    // 公开商品列表（仅在售）
    @PostMapping("/goodsopenlist")
    @Operation(summary = "公开商品列表", description = "分页查询仅在售的二手商品")
    public Result<PageBean<GoodsVO>> goodsOpenList(@RequestBody GoodsQueryDTO queryDTO) {
        try {
            // 强制设置状态为在售
            queryDTO.setGoodsStatus(GoodsStatusEnum.ON_SALE.getCode());
            PageBean<GoodsVO> pb = goodsService.list(queryDTO);
            return Result.success(pb);
        } catch (Exception e) {
            log.error("查询公开商品列表失败", e);
            return Result.error("查询公开商品失败：" + e.getMessage());
        }
    }

    // 我的商品
    @PreAuthorize("/goods/mylist")
    @PostMapping("/mylist")
    @Operation(summary = "我的商品列表", description = "分页查询当前登录用户发布的商品")
    public Result<PageBean<GoodsVO>> myList(@RequestBody GoodsQueryDTO queryDTO) {
        try {
            Map<String, Object> map = ThreadLocalUtil.get();
            Integer userId = (Integer) map.get("id");
            if (userId == null) {
                return Result.error("用户未登录");
            }
            queryDTO.setSellerId(userId);
            PageBean<GoodsVO> pb = goodsService.alllist(queryDTO);
            return Result.success(pb);
        } catch (Exception e) {
            log.error("查询我的商品列表失败", e);
            return Result.error("查询我的商品失败：" + e.getMessage());
        }
    }

    // 通过卖家id查询卖家信息
    @PostMapping("/findSellerByUserId/{id}")
    @Operation(summary = "查询商品卖家信息", description = "通过用户id查询卖家基本信息")
    public Result<BuyerViewSellerVO> findSellerByUserId(@PathVariable Integer id) {
        try {
            if (id == null || id < 1) {
                return Result.error("用户ID不合法");
            }
            BuyerViewSellerVO vo = goodsService.findSellerByUserId(id);
            if (vo == null) {
                return Result.error("卖家信息不存在");
            }
            return Result.success(vo);
        } catch (Exception e) {
            log.error("查询卖家信息失败，用户ID：{}", id, e);
            return Result.error("查询卖家信息失败：" + e.getMessage());
        }
    }

    // RAG检索（返回包含总结的结果）
    @PostMapping("/rag/search")
    @Operation(summary = "RAG搜索", description = "通过语义检索相似商品并生成总结")
    public Result<Map<String, Object>> ragSearch(@RequestBody Map<String, String> request) {
        try {
            String query = request.get("query");
            // 参数校验
            if (query == null || query.trim().isEmpty()) {
                return Result.error("查询内容不能为空");
            }
            // 执行RAG搜索
            List<GoodsVO> goodsList = goodsService.ragSearch(query.trim());
            // 生成搜索总结（兼容无结果场景）
            String summary;
            if (goodsList.isEmpty()) {
                summary = String.format("未找到与「%s」相关的在售商品", query.trim());
            } else {
                summary = qwenChatUtil.generateSearchSummary(query.trim(), goodsList);
            }
            // 构造返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("summary", summary);
            result.put("goodsList", goodsList);
            result.put("total", goodsList.size()); // 新增：返回商品数量
            return Result.success(result);
        } catch (Exception e) {
            log.error("RAG搜索失败，查询词：{}", request.get("query"), e);
            return Result.error("商品搜索失败：" + e.getMessage());
        }
    }

    // 清理Milvus脏数据（仅管理员可调用）
    @PreAuthorize("/goods/cleanMilvusDirtyData")
    @PostMapping("/cleanMilvusDirtyData")
    @Operation(summary = "清理Milvus脏数据", description = "全量校验并删除向量库中的无效数据（仅管理员）")
    public Result cleanMilvusDirtyData() {
        try {
            goodsService.cleanMilvusDirtyData();
            return Result.success("Milvus脏数据清理完成");
        } catch (Exception e) {
            log.error("清理Milvus脏数据失败", e);
            return Result.error("清理脏数据失败：" + e.getMessage());
        }
    }


    /**
     * 推荐模块商品查询（按分类关键词获取前3个商品）
     * 适配前端推荐模块展示需求
     */
    @PostMapping("/recommend/byKeyword")
    @Operation(summary = "按关键词推荐商品", description = "根据分类关键词随机返回3个在售商品，适配推荐模块展示")
    public Result<Map<String, Object>> recommendByKeyword(@RequestBody Map<String, String> request) {
        try {
            // 适配前端传递的参数名：keyword
            String query = request.get("keyword");
            // 参数校验
            if (query == null || query.trim().isEmpty()) {
                return Result.error("查询内容不能为空");
            }
            String cleanQuery = query.trim();

            // 执行RAG搜索
            List<GoodsVO> goodsList = goodsService.ragSearch(cleanQuery);

            // 随机筛选3个商品
            List<GoodsVO> randomGoodsList = new ArrayList<>();
            if (!goodsList.isEmpty()) {
                // 打乱列表顺序
                Collections.shuffle(goodsList);
                // 截取前3个
                randomGoodsList = goodsList.subList(0, Math.min(3, goodsList.size()));
            }

            // 生成搜索总结（前端可忽略，保留兼容）
            String summary;
            if (randomGoodsList.isEmpty()) {
                summary = String.format("未找到与「%s」相关的在售商品", cleanQuery);
            } else {
                summary = qwenChatUtil.generateSearchSummary(cleanQuery, randomGoodsList);
            }

            // 构造返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("summary", summary);
            result.put("goodsList", randomGoodsList); // 返回GoodsVO列表
            result.put("total", randomGoodsList.size());

            return Result.success(result);
        } catch (Exception e) {
            log.error("按关键词推荐商品失败，关键词：{}", request.get("keyword"), e);
            return Result.error("推荐商品查询失败：" + e.getMessage());
        }
    }


    //直接分页获取卖家的全部商品
    @PostMapping("/seller/alllist")
    @Operation(summary = "卖家商品列表", description = "分页查询当前登录用户发布的商品")
    public Result<PageBean<GoodsVO>> sellerAllList(@RequestBody GoodsQueryDTO queryDTO) {
        return Result.success(goodsService.alllist(queryDTO));
    }

}