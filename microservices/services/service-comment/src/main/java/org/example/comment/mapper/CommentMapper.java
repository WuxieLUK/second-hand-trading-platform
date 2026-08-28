package org.example.comment.mapper;

import org.apache.ibatis.annotations.*;
import org.example.goods.POJO.Comment;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Insert("insert into comment(goods_id,nickname,content,like_count,user_url,parent_id,comment_user_id,create_time,update_time) values(#{goodsId},#{nickname},#{content},#{likeCount},#{userUrl},#{parentId},#{commentUserId},now(),now())")
    void add(Comment comment);

    List<Comment> list(@Param("goodsId") Integer goodsId, @Param("content") String content, @Param("username") String username);

    @Update("update comment set nickname = #{nickname}, content = #{content}, like_count = #{likeCount}, update_time = now() where id = #{id}")
    void update(Comment comment);

    @Delete("delete from comment where id = #{id}")
    void delete(Integer id);

    @Select("select * from comment where goods_id = #{goodsId}")
    List<Comment> commentList(Integer goodsId);

    @Select("select * from comment where id = #{id}")
    Comment findById(Integer id);

    @Select("select count(*) > 0 from comment_likes where user_id = #{userId} and comment_id = #{commentId}")
    boolean checkExists(@Param("userId") Integer userId, @Param("commentId") Integer commentId);

    @Delete("delete from comment_likes where user_id = #{userId} and comment_id = #{commentId}")
    void deleteLike(@Param("userId") Integer userId, @Param("commentId") Integer commentId);

    @Insert("insert into comment_likes(user_id, comment_id) values(#{userId}, #{commentId})")
    void insertLike(@Param("userId") Integer userId, @Param("commentId") Integer commentId);

    List<Comment> commentallListByUserId(Integer userId);
}
