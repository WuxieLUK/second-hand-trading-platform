package org.example.comment.service;

import org.example.common.PageBean;
import org.example.goods.POJO.Comment;

import java.util.List;

public interface CommentDomainService {
    void add(Comment comment);

    PageBean<Comment> list(Integer pageNum, Integer pageSize, Integer goodsId, String content, String username);

    void update(Comment comment);

    void delete(Integer id);

    List<Comment> commentList(Integer goodsId);

    Integer like(Integer id);

    List<Comment> commentallListByUserId(Integer userId);
}
