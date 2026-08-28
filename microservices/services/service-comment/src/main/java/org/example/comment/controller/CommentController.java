package org.example.comment.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import jakarta.validation.Valid;
import org.example.comment.exception.SentinelBlockHandler;
import org.example.comment.service.CommentDomainService;
import org.example.comment.util.ThreadLocalUtil;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.goods.POJO.Comment;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comment")
public class CommentController {

    private final CommentDomainService commentDomainService;

    public CommentController(CommentDomainService commentDomainService) {
        this.commentDomainService = commentDomainService;
    }

    @PostMapping("/add")
    @SentinelResource(value = "commentAdd", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "commentAddBlocked")
    public Result<Void> add(@RequestBody @Valid Comment comment) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        comment.setCommentUserId((Integer) claims.get("id"));
        commentDomainService.add(comment);
        return Result.success();
    }

    @GetMapping("/list")
    @SentinelResource(value = "commentList", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "commentListBlocked")
    public Result<PageBean<Comment>> list(Integer pageNum, Integer pageSize,
                                          @RequestParam(required = false) Integer goodsId,
                                          @RequestParam(required = false) String content,
                                          @RequestParam(required = false) String username) {
        return Result.success(commentDomainService.list(pageNum, pageSize, goodsId, content, username));
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody Comment comment) {
        commentDomainService.update(comment);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        commentDomainService.delete(id);
        return Result.success();
    }

    @GetMapping("/commentList/{goodsId}")
    public Result<List<Comment>> commentList(@PathVariable Integer goodsId) {
        return Result.success(commentDomainService.commentList(goodsId));
    }

    @PostMapping("/like/{id}")
    public Result<String> like(@PathVariable Integer id) {
        return Result.success(String.valueOf(commentDomainService.like(id)));
    }

    @GetMapping("/commentallList/{userId}")
    public Result<List<Comment>> commentallList(@PathVariable Integer userId) {
        return Result.success(commentDomainService.commentallListByUserId(userId));
    }
}
