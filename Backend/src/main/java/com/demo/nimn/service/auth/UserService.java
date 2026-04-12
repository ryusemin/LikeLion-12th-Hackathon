package com.demo.nimn.service.auth;

import com.demo.nimn.dto.auth.PasswordChangeDTO;
import com.demo.nimn.dto.auth.UserDTO;
import com.demo.nimn.dto.auth.UserDetails;
import com.demo.nimn.dto.auth.UsersEmailDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.User;

public interface UserService {

    public UserDetails userSignup(UserDetails userDetails);

    public void userLogout(String refreshToken);

    public boolean existsByEmail(String email);

    public UserDetails getUserDetail(String email);

    public UserDetails updateUser(UserDetails user);

    public UserDTO changePassword(PasswordChangeDTO passwordChangeDto);

    public UserDTO updatePassword(PasswordChangeDTO passwordChangeDto);

    public UsersEmailDTO getAllUsersEmail();

}
