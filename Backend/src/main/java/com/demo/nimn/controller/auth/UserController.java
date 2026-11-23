package com.demo.nimn.controller.auth;

import com.demo.nimn.dto.auth.CustomUserDetails;
import com.demo.nimn.dto.auth.*;
import com.demo.nimn.exception.CustomException;
import com.demo.nimn.service.auth.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name="유저 API", description = "유저 정보 관리")
@RestController
@RequestMapping(value = "/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController (UserService userService){
        this.userService = userService;
    }

    @Operation(summary = "회원가입", description = "SIGNUP 목적의 emailVerificationToken을 소비해 회원가입을 진행한다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락 또는 이미 존재하는 이메일", content = @Content()),
            @ApiResponse(responseCode = "403", description = "이메일 인증 증명 없음·만료·재사용·불일치", content = @Content()),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content())
    })
    @PostMapping(value = "/signup")
    public ResponseEntity<?> join (@RequestBody UserDetails userDetails){
        try {
        String errorMessage = validateUserDetails(userDetails);

        if (errorMessage != null) {
            return ResponseEntity.badRequest().body(errorMessage);
        }

        UserDetails result = userService.userSignup(userDetails);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (CustomException e) {
            // 이메일 인증 증명 실패 등은 공통 오류 응답으로 처리한다.
            throw e;
        } catch (IllegalStateException e) {
            // 중복된 값이 존재할 경우 처리
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // 기타 예외 처리
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Service Unavailable: " + e.getMessage());
        }
    }

    @Operation(summary = "이메일 존재 여부 조회", description = "해당 이메일의 존재 여부를 boolean 타입으로 확인")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content()),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content())
    })
    @GetMapping(value = "/isExist")
    public ResponseEntity<Boolean> existEmail (@RequestParam String email){
        return ResponseEntity.ok().body(userService.existsByEmail(email));
    }


    @Operation(summary = "모든 유저 이메일 조회", description = "존재하는 모든 유저의 이메일을 조회한다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content()),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content())
    })
    @GetMapping(value = "/all")
    public ResponseEntity<UsersEmailDTO> getAllUsersEmail (){

        return ResponseEntity.ok().body(userService.getAllUsersEmail());
    }


    @Operation(summary = "로그인 유저 정보조회", description = "로그인한 유저의 정보를 조회한다.", security = @SecurityRequirement(name = "accessToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content()),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content())
    })
    @GetMapping
    public ResponseEntity<UserDetails> getUserDetail(@AuthenticationPrincipal CustomUserDetails userDetail) {
        String email = userDetail.getUsername();

        UserDetails userDetails = userService.getUserDetail(email);

        if (userDetails.getName() == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(userDetails);
        }

        return ResponseEntity.ok(userDetails);
    }

    private String validateUserDetails(UserDetails userDetails) {
        if (userDetails.getName() == null || userDetails.getName().isEmpty()) {
            return "Name cannot be null or empty";
        }
        if (userDetails.getEmail() == null || userDetails.getEmail().isEmpty()) {
            return "Email cannot be null or empty";
        }
        if (userDetails.getPassword() == null || userDetails.getPassword().isEmpty()) {
            return "Password cannot be null or empty";
        }
        return null;
    }

    @Operation(summary = "로그인 유저 정보 수정", description = "로그인한 유저의 정보를 수정한다.", security = @SecurityRequirement(name = "accessToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content()),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content())
    })
    @PutMapping
    public ResponseEntity<UserDetails> updateUser(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @RequestBody UserDetails user){
        return ResponseEntity.ok().body(userService.updateUser(principal.getUsername(), user));
    }

    @Operation(summary = "로그인 상태 비밀번호 변경", description = "유저의 비밀번호를 변경한다.", security = @SecurityRequirement(name = "accessToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공, 응답 password 값 = null"),
            @ApiResponse(responseCode = "401", description = "실패, 응답 password 값 = 중복 or 불일치 ", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content()),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content())
    })
    @PutMapping("/password")
    public ResponseEntity<UserDTO> updatePassword(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @RequestBody PasswordChangeDTO passwordChangeDTO){
        UserDTO user = userService.changePassword(principal.getUsername(), passwordChangeDTO);
        if(user.getPassword() == null){
            return ResponseEntity.ok().body(user);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(user);
        }
    }


    @Operation(summary = "비로그인 상태 비밀번호 변경", description = "PASSWORD_RESET 목적의 emailVerificationToken을 소비해 비밀번호를 변경하고 기존 Refresh Token을 폐기한다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공, 응답 password 값 = null"),
            @ApiResponse(responseCode = "401", description = "실패, 응답 password 값 = 중복 (사용한 증명 토큰은 소비됨)", content = @Content()),
            @ApiResponse(responseCode = "403", description = "이메일 인증 증명 없음·만료·재사용·불일치", content = @Content()),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content())
    })
    @PutMapping("/reset-password")
    public ResponseEntity<UserDTO> changePassword(@RequestBody PasswordChangeDTO passwordChangeDTO){
        UserDTO user = userService.updatePassword(passwordChangeDTO);
        if(user.getPassword() == null){
            return ResponseEntity.ok().body(user);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(user);
        }
    }
}