package com.demo.nimn.service.auth;

import com.demo.nimn.dto.auth.PasswordChangeDTO;
import com.demo.nimn.dto.auth.UserDTO;
import com.demo.nimn.dto.auth.UserDetails;
import com.demo.nimn.dto.auth.UsersEmailDTO;
import com.demo.nimn.entity.auth.Users;
import com.demo.nimn.enums.EmailVerificationPurpose;
import com.demo.nimn.service.email.EmailVerificationService;
import com.demo.nimn.filter.JWTUtil;
import com.demo.nimn.repository.auth.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JWTUtil jwtUtil;
    private final EmailVerificationService emailVerificationService;

    public UserServiceImpl(BCryptPasswordEncoder bCryptPasswordEncoder, @Autowired UserRepository userRepository, RefreshTokenService refreshTokenService, JWTUtil jwtUtil, EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
        this.emailVerificationService = emailVerificationService;
    }

    @Override
    public UserDetails userSignup(UserDetails userDetails) {

        if (userDetails.getName() == null || userDetails.getName().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be null or empty");
        }
        if (userDetails.getEmail() == null || userDetails.getEmail().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be null or empty");
        }
        if (userDetails.getPassword() == null || userDetails.getPassword().isEmpty()) {
            throw new IllegalArgumentException("User password cannot be null or empty");
        }

        emailVerificationService.consumeProof(
                userDetails.getEmailVerificationToken(), userDetails.getEmail(), EmailVerificationPurpose.SIGNUP);

        if (userRepository.existsByEmail(userDetails.getEmail())) {
            throw new IllegalStateException("동일한 이메일이 이미 존재합니다.");
        }

        Users user = Users.builder()
                .name(userDetails.getName())
                .email(userDetails.getEmail())
                .password(bCryptPasswordEncoder.encode(userDetails.getPassword()))
                .phoneNumber(userDetails.getPhoneNumber())
                .roadAddress(userDetails.getRoadAddress())
                .detailAddress(userDetails.getDetailAddress())
                .role("ROLE_USER")
                .build();

        userRepository.save(user);

        return UserDetails.builder()
                .name(userDetails.getName())
                .email(userDetails.getEmail())
                .phoneNumber(userDetails.getPhoneNumber())
                .roadAddress(userDetails.getRoadAddress())
                .detailAddress(userDetails.getDetailAddress())
                .build();
    }

    public boolean existsByEmail(String email){
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserDetails getUserDetail(String email) {
        Users user = findUserByEmail(email);

        if (user == null) {
            // null이면 빈 UserDetails 리턴하거나 메시지를 포함한 기본 객체 반환
            return UserDetails.builder()
                    .name(null)
                    .email(email)
                    .phoneNumber(null)
                    .roadAddress(null)
                    .detailAddress(null)
                    .build();
        }

        return UserDetails.builder()
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .roadAddress(user.getRoadAddress())
                .detailAddress(user.getDetailAddress())
                .build();
    }

    @Override
    public UserDetails updateUser(String email, UserDetails user) {
        // 수정 대상은 요청 본문의 이메일이 아니라 로그인한 회원이다 (#8).
        Users userEntity = findUserByEmail(email);

        userEntity.updateUser(user);
        userRepository.save(userEntity);
        return getUserDetail(email);
    }

    @Transactional
    @Override
    public UserDTO changePassword(String email, PasswordChangeDTO passwordChangeDto) {
        String oldPassword = passwordChangeDto.getOldPassword();
        String newPassword = passwordChangeDto.getNewPassword();

        UserDTO user = UserDTO.builder()
                .email(email)
                .build();

        Users userEntity = userRepository.findByEmail(email);
        if (userEntity == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        // oldPassword가 일치함
        if(isPasswordMatch(oldPassword, userEntity.getPassword())){
            if(isPasswordMatch(newPassword, userEntity.getPassword())){
                user = user.toBuilder()
                        .password("중복")
                        .build();
                return user;
            }
            // 새로운 비밀번호를 encode 한 이후 set
            userEntity.setPassword(bCryptPasswordEncoder.encode(passwordChangeDto.getNewPassword()));
        }
        // oldPassword가 일치하지 않음
        else{
            user = user.toBuilder()
                    .password("불일치")
                    .build();
        }
        return user;
    }

    @Transactional
    @Override
    public UserDTO updatePassword(PasswordChangeDTO passwordChangeDto) {
        String email = passwordChangeDto.getEmail();
        String newPassword = passwordChangeDto.getNewPassword();

        // 증명 확인을 가장 먼저 한다. 뒤의 "중복" 응답이 증명 없이 현재 비밀번호를 맞혀 보는 수단이 되지 않게 한다.
        emailVerificationService.consumeProof(
                passwordChangeDto.getEmailVerificationToken(), email, EmailVerificationPurpose.PASSWORD_RESET);

        UserDTO user = UserDTO.builder()
                .email(email)
                .build();

        Users userEntity = userRepository.findByEmail(email);
        if (userEntity == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        if(isPasswordMatch(newPassword, userEntity.getPassword())){
            user = user.toBuilder()
                    .password("중복")
                    .build();
            return user;
        }
        // 새로운 비밀번호를 encode 한 이후 set
        userEntity.setPassword(bCryptPasswordEncoder.encode(passwordChangeDto.getNewPassword()));
        // 기존 로그인 세션이 새 비밀번호 이후에도 재발급되지 않게 한다.
        refreshTokenService.deleteRefreshToken(email);

        return user;
    }

    @Override
    public UsersEmailDTO getAllUsersEmail() {
        List<Users> usersList = userRepository.findAll();

        // Stream을 사용하여 변환
        List<String> emails = usersList.stream()
                .map(Users::getEmail)
                .toList();

        return UsersEmailDTO.builder()
                .email(emails)
                .build();
    }

    private Users findUserByEmail(String email) {
        Users user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }
        return user;
    }

    private boolean isPasswordMatch(String raw, String encoded) {
        return bCryptPasswordEncoder.matches(raw, encoded);
    }

}
