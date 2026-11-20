package com.demo.nimn.entity.auth;

import com.demo.nimn.dto.auth.UserDetails;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "t_user")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // 254 is the longest deliverable email address (SMTP path limit, #19).
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phoneNumber;

    private String roadAddress;

    private String detailAddress;

    private String role;

//    private String imgUrl;

    public void updateUser(UserDetails user){
        this.phoneNumber = user.getPhoneNumber();
        this.roadAddress = user.getRoadAddress();
        this.detailAddress = user.getDetailAddress();
    }
}
