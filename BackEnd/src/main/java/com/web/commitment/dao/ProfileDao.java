package com.web.commitment.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import com.web.commitment.dto.Follow;
import com.web.commitment.dto.Profile;
@Repository
public interface ProfileDao extends JpaRepository<Profile, String> {

	Profile getProfileByEmail(String email);

	Profile findProfileByEmail(String email);

}
