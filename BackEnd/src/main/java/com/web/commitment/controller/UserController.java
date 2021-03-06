package com.web.commitment.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.web.commitment.dao.UserBadgeDao;
import com.web.commitment.dao.UserDao;
import com.web.commitment.dto.Badge;
import com.web.commitment.dto.BasicResponse;
import com.web.commitment.dto.Profile;
import com.web.commitment.dto.User;
import com.web.commitment.request.LoginRequest;
import com.web.commitment.response.BoardDto;
import com.web.commitment.response.UserDto;
import com.web.commitment.service.JwtService;

import io.swagger.annotations.ApiOperation;

@CrossOrigin
@RestController
public class UserController {
	@Autowired
	UserDao userDao;
	@Autowired
	FollowController followController;
	@Autowired
	CommitController commitController;
	@Autowired
	BadgeController badgeController;
	@Autowired
	UserBadgeDao userBadgeDao;
	@Autowired
	private JwtService jwtService;

	@PostMapping("/account/login")
	@ApiOperation(value = "?????????")
	public Object login(@RequestBody LoginRequest request) {
		Optional<User> userOpt;
		ResponseEntity response = null;
		Map<String, Object> resultMap = new HashMap<>();
		System.out.println("login");
		if (request.getToken() != null) {// ?????? ????????? ?????????
			userOpt = userDao.findByEmail(request.getEmail());
			User user = new User();

			if (!userOpt.isPresent()) {// db??? ?????? ??????
				user.setEmail(request.getEmail());
				user.setNickname(request.getName());
				userDao.save(user);// ??????????????????
			}
			Profile profile = new Profile();
			profile.setEmail(request.getEmail());
			profile.setFilePath(request.getImage());

		}

		userOpt = userDao.findUserByEmailAndPass(request.getEmail(), request.getPass());

		if (userOpt.isPresent()) {
//        	jwt.io?????? ??????
//			????????? ??????????????? ????????? ????????????.
			User user = userOpt.get();
			UserDto userDto = new UserDto();
			BeanUtils.copyProperties(user, userDto);
			String token = jwtService.create(userDto);
//			logger.trace("????????? ???????????? : {}", token);

			resultMap.put("auth-token", token);
			resultMap.put("data", "success");
			resultMap.put("email", user.getEmail());
			resultMap.put("pass", user.getPass());
			resultMap.put("nickname", user.getNickname());
			resultMap.put("tel", user.getTel());
			resultMap.put("age", user.getAge());
			resultMap.put("gender", user.getGender());
			resultMap.put("mystory", user.getMystory());
			resultMap.put("auth", user.getAuth());
			return new ResponseEntity<>(resultMap, HttpStatus.OK);
		}
		resultMap.put("data", "fail");

		return new ResponseEntity<>(resultMap, HttpStatus.OK);
	}

	@GetMapping("/account/info")
	@ApiOperation(value = "????????? ???????????? ????????????")
	public ResponseEntity<Map<String, Object>> getInfo(HttpServletRequest req) {
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus status = HttpStatus.ACCEPTED;
		System.out.println(">>>>>> " + jwtService.get(req.getHeader("auth-token")));
		try {
			// ??????????????? ????????? ????????????.
			resultMap.putAll(jwtService.get(req.getHeader("auth-token")));
			Map<String, String> s = (Map<String, String>) resultMap.get("user");
			resultMap.put("commitCnt", commitController.totalCommitNum(s.get("email")));
			resultMap.put("followerCnt", followController.followCnt(s.get("email")));
			resultMap.put("badgeCnt", badgeController.badgeCnt(s.get("email")));

			status = HttpStatus.ACCEPTED;

		} catch (RuntimeException e) {
//			logger.error("???????????? ?????? : {}", e);
			resultMap.put("message", e.getMessage());
			status = HttpStatus.ACCEPTED;
		}
		return new ResponseEntity<Map<String, Object>>(resultMap, status);
	}

	@PostMapping("/account/signup")
	@ApiOperation(value = "??????????????????")
	@Transactional
	public User signup(@Valid @RequestBody User request) {
		User user = userDao.findUserByEmail(request.getEmail());// ??????

		if (user == null)// ??????
			user = request;
		else {
			user.setEmail(request.getEmail());
			user.setPass(request.getPass());
			user.setNickname(request.getNickname());
			user.setTel(request.getTel());
			user.setAge(request.getAge());
			user.setGender(request.getGender());
			user.setMystory(request.getMystory());
			// ????????? ??????????????? ?????? ?????? ???????????? ?????????
		}

		System.out.println(user);
		userDao.save(user);

		return user;
	}

	@DeleteMapping("/account/delete")
	@ApiOperation(value = "????????????")
	@Transactional
	public Object userDelete(@RequestParam(required = true) final String email) {
		User user = userDao.getUserByEmail(email);
		userDao.delete(user);

		final BasicResponse result = new BasicResponse();
		result.status = true;
		result.data = "success";

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@GetMapping("/account/nickCheck")
	@ApiOperation(value = "????????? ????????????")
	public Map<String, String> uidCheck(@RequestParam(required = true) final String nickname) throws IOException {
		Optional<User> user = userDao.findUserByNickname(nickname);
		Map<String, String> hm = new HashMap<>();

		if (user.isPresent()) {
			hm.put("data", "fail");
			return hm;
		}
		hm.put("data", "success");
		return hm;
	}

	@GetMapping("/account/emailCheck")
	@ApiOperation(value = "????????? ????????????")
	public Map<String, String> emailCheck(@RequestParam(required = true) final String email) throws IOException {
		Optional<User> user = userDao.findByEmail(email);
		Map<String, String> hm = new HashMap<>();

		if (user.isPresent()) {
			hm.put("data", "fail");
			return hm;
		}
		hm.put("data", "success");
		return hm;
	}

	// ????????? ?????? ??????
	@GetMapping("/account/smtp")
	@ApiOperation(value = "smtp")
	@Transactional
	protected String smtp(@Valid @RequestParam(required = true) final String email) {
		User user = userDao.getUserByEmail(email);
		// ?????? ???????????? ??????????????? ???????????? ????????? ??????????????? email?????? ???????????? ???????????? ????????? ???????????? ????????? ??????
		// mail server ??????
		String host = "smtp.naver.com";
		String id = ""; // ????????? ????????? ??????
		String password = "";// ????????? ????????? ????????????

		// ?????? ?????? ??????
		String to_email = email;

		// SMTP ?????? ????????? ????????????.
		Properties props = new Properties();
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", 465);
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.ssl.enable", "true");
		// ?????? ?????? ?????????
		StringBuffer temp = new StringBuffer();
		Random rnd = new Random();
		for (int i = 0; i < 10; i++) {
			int rIndex = rnd.nextInt(3);
			switch (rIndex) {
			case 0:
				// a-z
				temp.append((char) ((int) (rnd.nextInt(26)) + 97));
				break;
			case 1:
				// A-Z
				temp.append((char) ((int) (rnd.nextInt(26)) + 65));
				break;
			case 2:
				// 0-9
				temp.append((rnd.nextInt(10)));
				break;
			}
		}
		String AuthenticationKey = temp.toString();
		user.setAuthkey(AuthenticationKey);// ????????? ??????
		userDao.save(user);
		System.out.println(AuthenticationKey);
		userDao.AuthkeyUpdate(to_email, AuthenticationKey);// ???????????? KEY ??????

		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(id, password);
			}
		});
		session.setDebug(true);
		// email ??????
		try {
			MimeMessage msg = new MimeMessage(session);
			MimeMessageHelper messageHelper = new MimeMessageHelper(msg, true, "UTF-8");
			messageHelper.setFrom(new InternetAddress("1693013@naver.com"));
			messageHelper.setTo(email);
			// ?????? ??????
			messageHelper.setSubject("??????????????? Commitment ?????? ???????????????.");
			// ?????? ??????
			messageHelper.setText(new StringBuffer().append("<center>").append("<div height=\"1000\">").append(
					"<img src=\"https://commitmentbucket.s3.ap-northeast-2.amazonaws.com/%EB%A9%94%EC%9D%BC+%EC%9D%B8%EC%A6%9D.PNG\" width=\"550\" >")
					.append("<br>").append("<a  href='https://i4a308.p.ssafy.io:8080/user/signUpConfirm?email=")
					.append(user.getEmail()).append("&authKey=").append(AuthenticationKey)
					.append("' target='_blenk'><font size=\"5px\"  color=\"black\">[?????? ??????]</a></font>").append("</div>")
					.append("</center>").toString(), true);// true??? ?????? html???????????? ???

			Transport.send(msg);
			System.out.println("????????? ??????");

		} catch (Exception e) {
			e.printStackTrace();// TODO: handle exception
		}
		return AuthenticationKey;
	}

	@GetMapping("/user/signUpConfirm")
	@ApiOperation(value = "?????? ?????? ??????")
	public void signUpConfirm(@RequestParam(required = true) final String email,
			@RequestParam(required = true) final String authKey, HttpServletResponse response) throws IOException {
		// ?????? ????????? ?????? ?????? authStatus ????????????
		Optional<User> userOpt = userDao.findUserByEmailAndAuthkey(email, authKey);// ????????? ??????????????? ??????
		if (userOpt.isPresent()) {
			userDao.AuthUpdate(email);// ?????? ????????? ??????
			response.sendRedirect("http://i4a308.p.ssafy.io/user/mailCheck");// ??????????????? ????????????????????? ???????????? ??????(????????? ??????)
		} else
			response.sendRedirect("http://i4a308.p.ssafy.io/404");
	}

	@GetMapping("/user/loaction")
	@ApiOperation(value = "??????????????????")
	public Object loaction(@RequestParam(required = true) final String email,
			@RequestParam(required = true) final String lat, @RequestParam(required = true) final String lng)
			throws IOException {
		User user = userDao.getUserByEmail(email);
		user.setLat(lat);
		user.setLng(lng);
		userDao.save(user);

		final BasicResponse result = new BasicResponse();
		result.status = true;
		result.data = "success";

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	/// ??????????????? ??????
	@GetMapping("/user/nickname")
	@ApiOperation(value = "??????????????? ???????????? ????????????")
	public Map<String, Object> searchByNickname(@RequestParam String nickname) {
		HashMap<String, Object> hm = new HashMap<>();

		Optional<User> option = userDao.findUserByNickname(nickname);
		UserDto userdto = new UserDto();
		if (option.isPresent()) {
			BeanUtils.copyProperties(option.get(), userdto);
			hm.put("user", userdto);
			hm.put("commitCnt", commitController.totalCommitNum(userdto.getEmail()));
			hm.put("followerCnt", followController.followCnt(userdto.getEmail()));
			hm.put("badgeCnt", badgeController.badgeCnt(userdto.getEmail()));
			hm.put("data", "success");
		} else
			hm.put("data", "fail");
		return hm;
	}

	/// ???????????? ??????
	@GetMapping("/search/email")
	@ApiOperation(value = "???????????? ??????")
	public Page<User> searchByEmail(@RequestParam String keyword, final Pageable pageable) {

		return userDao.findByEmailContainingIgnoreCase(keyword, pageable);
	}
	@GetMapping("/search/nickname")
	@ApiOperation(value = "??????????????? ??????")
	public Page<User> searchBynick(@RequestParam String keyword, final Pageable pageable) {
		
		return userDao.findByNicknameContainingIgnoreCase(keyword, pageable);
	}

	@GetMapping("/user/map")
	@ApiOperation(value = "???????????? ??????")
	public String mapSelect(@RequestParam String email, @RequestParam String region) {
		User user = userDao.getUserByEmail(email);
		try {
			user.setRegion_name(region);
			userDao.save(user);
		} catch (Exception e) {
			return "?????????????????? ????????? ??????";
		}

		return "success";
	}
}
