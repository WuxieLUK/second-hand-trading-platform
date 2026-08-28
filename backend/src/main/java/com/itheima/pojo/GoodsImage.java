package com.itheima.pojo;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@TableName("goods_image")
public class GoodsImage {
    private Integer id;
    private Integer goodsId;
    private String imageUrl;
    // 扩展信息（JSON格式，需MyBatisPlus支持）
    @TableField("ext_info")
    private String extInfo;
    private LocalDateTime createTime;

    // 可选：把extInfo转为Map，方便前端使用
    @TableField(exist = false)
    private Map<String, Object> extInfoMap;

    // 转换方法
    public Map<String, Object> getExtInfoMap() {
        if (StringUtils.isEmpty(extInfo)) {
            return new HashMap<>();
        }
        return JSONUtil.parseObj(extInfo);
    }
}
