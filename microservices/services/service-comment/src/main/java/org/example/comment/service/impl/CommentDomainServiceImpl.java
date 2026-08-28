package org.example.comment.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.example.comment.feign.ProductFeignClient;
import org.example.comment.feign.UserFeignClient;
import org.example.comment.mapper.CommentMapper;
import org.example.comment.service.CommentDomainService;
import org.example.comment.util.ThreadLocalUtil;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.goods.POJO.Comment;
import org.example.goods.VO.GoodsDetailVO;
import org.example.user.POJO.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class CommentDomainServiceImpl implements CommentDomainService {

    private final CommentMapper commentMapper;
    private final UserFeignClient userFeignClient;
    private final ProductFeignClient productFeignClient;

    public CommentDomainServiceImpl(CommentMapper commentMapper,
                                    UserFeignClient userFeignClient,
                                    ProductFeignClient productFeignClient) {
        this.commentMapper = commentMapper;
        this.userFeignClient = userFeignClient;
        this.productFeignClient = productFeignClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Comment comment) {
        Result<User> userResult = userFeignClient.getUserById(comment.getCommentUserId());
        Result<GoodsDetailVO> goodsResult = productFeignClient.getGoodsById(comment.getGoodsId());
        if (userResult.getData() == null) {
            throw new IllegalArgumentException("评论用户不存在");
        }
        if (goodsResult.getData() == null) {
            throw new IllegalArgumentException("评论商品不存在");
        }
        comment.setNickname(userResult.getData().getNickname());
        commentMapper.add(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageBean<Comment> list(Integer pageNum, Integer pageSize, Integer goodsId, String content, String username) {
        PageHelper.startPage(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        List<Comment> comments = commentMapper.list(goodsId, content, username);
        Page<Comment> page = (Page<Comment>) comments;
        PageBean<Comment> pageBean = new PageBean<>();
        pageBean.setTotal(page.getTotal());
        pageBean.setItems(page.getResult());
        return pageBean;
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
    public List<Comment> commentList(Integer goodsId) {
        return commentMapper.commentList(goodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer like(Integer id) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        boolean exists = commentMapper.checkExists(userId, id);
        Comment comment = commentMapper.findById(id);
        int count = comment.getLikeCount() == null ? 0 : comment.getLikeCount();
        if (exists) {
            commentMapper.deleteLike(userId, id);
            comment.setLikeCount(Math.max(0, count - 1));
            commentMapper.update(comment);
            return 0;
        }
        commentMapper.insertLike(userId, id);
        comment.setLikeCount(count + 1);
        commentMapper.update(comment);
        return 1;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> commentallListByUserId(Integer userId) {
        return commentMapper.commentallListByUserId(userId);
    }
}
