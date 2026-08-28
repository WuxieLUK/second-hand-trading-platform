package com.itheima.controller;

import com.itheima.anno.PreAuthorize;
import com.itheima.pojo.Comment;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.Result;
import com.itheima.service.CommentService;
import com.itheima.util.ThreadLocalUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comment")
@Slf4j
public class CommentController {

    @Resource
    private CommentService commentService;

    @PreAuthorize("/comment/add")
    @PostMapping("/add")
    public Result add(@RequestBody @Validated Comment comment){
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer commentUserId = (Integer) map.get("id");
        comment.setCommentUserId(commentUserId);
        commentService.add(comment);
        return Result.success();
    }

    @PreAuthorize("/comment/list")
    @GetMapping("/list")
    public Result<PageBean<Comment>> list(
            Integer pageNum,
            Integer pageSize,
            @RequestParam(required = false) Integer goodsId, // 核心：articleId改为goodsId
            @RequestParam(required = false) String content
    ){
        PageBean<Comment> pb =commentService.list(pageNum,pageSize,goodsId); // 参数同步修改
        return Result.success(pb);
    }

    @PreAuthorize("/comment/update")
    @PutMapping("/update")
    public Result update(@RequestBody Comment comment){
        commentService.update(comment);
        return Result.success("更新成功");
    }

    @PreAuthorize("/comment/delete/{id}")
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id){
        commentService.delete(id);
        return Result.success("删除成功");
    }

    // 核心：articleId改为goodsId
    @GetMapping("/commentList/{goodsId}")
    public Result<List<Comment>> commentList(@PathVariable Integer goodsId){
        List<Comment> cs = commentService.commentList(goodsId);
        return Result.success(cs);
    }

    @PreAuthorize("/comment/like/{id}")
    @PostMapping("/like/{id}")
    public Result like(@PathVariable Integer id){
        Integer islike=commentService.like(id);
        if(islike==1){
            return Result.success("1");
        }
        return Result.success("0");
    }


    //显示与某一个用户id有关的商品的全部评论
    @GetMapping("/commentallList/{userId}")
    public Result<List<Comment>> commentallList(@PathVariable Integer userId){
        List<Comment> cs = commentService.commentallListByUserId(userId);
        return Result.success(cs);
    }
}