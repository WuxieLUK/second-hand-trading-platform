package com.itheima.service;


import com.itheima.pojo.Comment;
import com.itheima.pojo.PageBean;

import java.util.List;

public interface CommentService {
    //添加用户评论
    void add(Comment comment);

    //分页查询全部用户评论
    PageBean<Comment> list(Integer pageNum, Integer pageSize, Integer articleId);

    //更新用户评论
    void update(Comment comment);

    //删除用户评论
    void delete(Integer id);

    //查询全部用户评论
    List<Comment> commentList(Integer articleId);

    //获取所有文章详细信息
    List<Comment> commentallList();

    Integer like(Integer id);

    List<Comment> commentallListByUserId(Integer userId);
}
