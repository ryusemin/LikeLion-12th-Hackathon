package com.example.authServer.Controller;

import com.example.authServer.DTO.UserDTO;
import com.example.authServer.DTO.UserDetails;
import com.example.authServer.DTO.UsersEmailDTO;
import com.example.authServer.Entity.Users;
import com.example.authServer.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/users")
public class UserController {

    private UserService userService;

    @Autowired
    public UserController (UserService userService){
        this.userService = userService;
    }

    @PostMapping(value = "/signup")
    public ResponseEntity<?> join (@RequestBody UserDetails userDetails){
        try {
        String errorMessage = validateUserDetails(userDetails);

        if (errorMessage != null) {
            return ResponseEntity.badRequest().body(errorMessage);
        }

        UserDetails result = userService.userSignup(userDetails);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            // 중복된 값이 존재할 경우 처리
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // 기타 예외 처리
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Service Unavailable: " + e.getMessage());
        }
    }



    @GetMapping(value = "/isExist")
    public boolean existEmail (@RequestParam String email){
        return userService.existsByEmail(email);
    }



    @GetMapping(value = "/all")
    public UsersEmailDTO getUsersEmail (){
        return userService.getUsersEmail();
    }



    @GetMapping
    public UserDetails getUserDetail(@RequestParam String email){
        return userService.getUserDetail(email);
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

    @PutMapping
    public UserDetails updateUser(@RequestBody UserDetails user){
        return userService.updateUser(user);
    }

}
