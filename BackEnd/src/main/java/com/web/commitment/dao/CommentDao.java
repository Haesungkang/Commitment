package com.web.commitment.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.web.commitment.dto.Comment;

@Repository
public interface CommentDao extends JpaRepository<Comment, String> {

	List<Comment> findBySnsId(String sns_id);


}