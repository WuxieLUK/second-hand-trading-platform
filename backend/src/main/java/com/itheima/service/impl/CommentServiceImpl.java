package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.CommentMapper;
import com.itheima.pojo.Comment;
import com.itheima.pojo.Goods; // 新增：导入商品实体
import com.itheima.pojo.PageBean;
import com.itheima.pojo.User;
import com.itheima.service.CommentService;
import com.itheima.service.GoodsService; // 新增：导入商品服务
import com.itheima.util.ThreadLocalUtil;
import com.itheima.vo.GoodsDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private UserServiceImpl userService;
    @Autowired
    private GoodsService goodsService; // 新增：注入商品服务（删除原ArticleService）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Comment comment) {
        // 1. 校验用户是否存在（保留）
        User user = userService.getById(comment.getCommentUserId());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + comment.getNickname());
        }
        comment.setNickname(user.getNickname());

        // 2. 核心：替换文章校验为商品校验
        GoodsDetailVO goods = goodsService.findById(comment.getGoodsId());
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在: " + comment.getGoodsId());
        }

        commentMapper.add(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageBean<Comment> list(Integer pageNum, Integer pageSize, Integer goodsId) {
        PageBean<Comment> pb = new PageBean<>();
        PageHelper.startPage(pageNum,pageSize);
        // 核心：参数从articleId改为goodsId
        List<Comment> as =commentMapper.list(goodsId);
        Page<Comment> p = (Page<Comment>) as;
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Comment comment) {
        commentMapper.update(comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        commentMapper.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> commentList(Integer goodsId) { // 参数从articleId改为goodsId
        return commentMapper.commentList(goodsId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> commentallList() {
        return commentMapper.commentallList();
    }

    @Override
    public Integer like(Integer id) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");

        boolean exists = commentMapper.checkExists(userId, id);
        if (exists) {
            commentMapper.deleteLike(userId, id);
            Comment comment =commentMapper.findById(id);
            Integer currentLikeCount = comment.getLikeCount();
            if (currentLikeCount == null) {
                currentLikeCount = 0;
            }
            comment.setLikeCount(Math.max(0, currentLikeCount - 1));
            commentMapper.update(comment);
            return 0;
        } else {
            commentMapper.insertLike(userId, id);
            Comment comment =commentMapper.findById(id);
            Integer currentLikeCount = comment.getLikeCount();
            if (currentLikeCount == null) {
                currentLikeCount = 0;
            }
            comment.setLikeCount(currentLikeCount + 1);
            commentMapper.update(comment);
            return 1;
        }
    }

    @Override
    public List<Comment> commentallListByUserId(Integer userId) {
        return commentMapper.commentallListByUserId(userId);
    }
}