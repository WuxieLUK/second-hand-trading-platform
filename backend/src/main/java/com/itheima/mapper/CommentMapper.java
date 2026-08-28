package com.itheima.mapper;

import com.itheima.pojo.Comment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentMapper {

 // 核心：article_id改为goods_id
 @Insert("insert into comment(goods_id,nickname,content,like_count,user_url,parent_id,comment_user_id,create_time,update_time)" +
         "values(#{goodsId},#{nickname},#{content},#{likeCount},#{userUrl},#{parentId},#{commentUserId},now(),now())")
 void add(Comment comment);

 // 参数从articleId改为goodsId
 List<Comment> list(Integer goodsId);

 @Update("update comment set nickname = #{nickname},content = #{content},like_count=#{likeCount},update_time = now() where id = #{id}")
 void update(Comment comment);

 @Delete("delete from comment where id = #{id}")
 void delete(Integer id);

 // 核心：article_id改为goods_id
 @Select("select * from comment where goods_id=#{goodsId}")
 List<Comment> commentList(Integer goodsId);

 @Select("select * from comment")
 List<Comment> commentallList();

 @Select("select * from comment where id= #{id}")
 Comment findById(Integer id);

 @Select("select COUNT(*)>0 from comment_likes where user_id= #{userId} and comment_id= #{commentId}")
 boolean checkExists(Integer userId, Integer commentId);

 @Delete("delete from comment_likes where user_id= #{userId} and comment_id= #{commentId}")
 void deleteLike(Integer userId, Integer commentId);

 @Insert("insert into comment_likes(user_id,comment_id) values(#{userId},#{id})")
 void insertLike(Integer userId, Integer id);

 List<Comment> commentallListByUserId(Integer userId);
}