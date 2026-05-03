package com.example.user_service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_service.dto.login.LoginDTO;
import com.example.user_service.dto.login.ResponseLoginDTO;
import com.example.user_service.exception.CustomException;
import com.example.user_service.model.Department;
import com.example.user_service.model.User;
import com.example.user_service.service.PermissionService;
import com.example.user_service.service.UserService;
import com.example.user_service.utils.JwtUtil;
import com.example.user_service.utils.SecurityUtil;
import com.example.user_service.utils.annotation.ApiMessage;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/user-service/auth")
public class AuthController {

        private final AuthenticationManagerBuilder authenticationManagerBuilder;
        private final JwtUtil jwtUtil;
        private final UserService userService;
        private final PermissionService permissionService;

        @Value("${user-service.jwt.refresh-token-validity-in-seconds}")
        private long refreshTokenExpiration;

        public AuthController(AuthenticationManagerBuilder authenticationManagerBuilder, JwtUtil jwtUtil,
                        UserService userService, PermissionService permissionService) {
                this.authenticationManagerBuilder = authenticationManagerBuilder;
                this.jwtUtil = jwtUtil;
                this.userService = userService;
                this.permissionService = permissionService;
        }

        @PostMapping("/login")
        @ApiMessage("Login thành công")
        public ResponseEntity<ResponseLoginDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
                // create input
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                loginDTO.getUsername(), loginDTO.getPassword());
                // authen
                Authentication authentication = authenticationManagerBuilder.getObject()
                                .authenticate(authenticationToken);

                // save to context
                SecurityContextHolder.getContext().setAuthentication(authentication);

                ResponseLoginDTO responseLoginDTO = new ResponseLoginDTO();

                User currentUserDB = this.userService.handleGetUserByUsername(loginDTO.getUsername());

                // Lấy thông tin từ Employee
                Long employeeId = currentUserDB.getEmployee() != null ? currentUserDB.getEmployee().getId() : null;
                String employeeName = currentUserDB.getEmployee() != null ? currentUserDB.getEmployee().getName()
                                : null;
                String employeeEmail = currentUserDB.getEmployee() != null ? currentUserDB.getEmployee().getEmail()
                                : currentUserDB.getEmail();

                // Tạo token với cả userId và employeeId
                Department department = currentUserDB.getEmployee() != null
                                ? currentUserDB.getEmployee().getDepartment()
                                : null;
                ResponseLoginDTO.UserToken userToken = new ResponseLoginDTO.UserToken(
                                currentUserDB.getId(),
                                employeeId != null ? employeeId : 0L,
                                employeeEmail,
                                employeeName != null ? employeeName : "",
                                currentUserDB.getRole().getName(),
                                department != null ? department.getId() : null,
                                department != null ? department.getCode() : null);

                if (currentUserDB != null) {
                        ResponseLoginDTO.UserLogin userLogin = new ResponseLoginDTO.UserLogin(
                                        currentUserDB.getId(),
                                        employeeId != null ? employeeId : 0L,
                                        employeeEmail,
                                        employeeName != null ? employeeName : "",
                                        currentUserDB.getRole(),
                                        currentUserDB.getEmployee() != null
                                                        ? currentUserDB.getEmployee().getDepartment()
                                                        : null);
                        responseLoginDTO.setUser(userLogin);
                }
                // access token
                String accessToken = this.jwtUtil.createAccessToken(authentication.getName(), userToken);
                responseLoginDTO.setAccessToken(accessToken);
                // refresh token
                String refresh_token = this.jwtUtil.createRefreshToken(loginDTO.getUsername(), userToken);
                // save refresh token to db
                this.userService.updateUserRefreshToken(refresh_token, loginDTO.getUsername());
                // set cookies
                ResponseCookie resCookie = ResponseCookie
                                .from("refresh_token", refresh_token)
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(refreshTokenExpiration)
                                .build();
                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, resCookie.toString())
                                .body(responseLoginDTO);
        }

        @GetMapping("/account")
        @ApiMessage("fetch account")
        public ResponseEntity<ResponseLoginDTO.UserGetAccount> getAccount() {
                String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get()
                                : "";

                User currentUserDB = this.userService.handleGetUserByUsername(email);
                ResponseLoginDTO.UserLogin userLogin = new ResponseLoginDTO.UserLogin();
                ResponseLoginDTO.UserGetAccount userGetAccount = new ResponseLoginDTO.UserGetAccount();
                if (currentUserDB != null) {
                        Long employeeId = currentUserDB.getEmployee() != null ? currentUserDB.getEmployee().getId()
                                        : null;
                        String employeeName = currentUserDB.getEmployee() != null
                                        ? currentUserDB.getEmployee().getName()
                                        : null;
                        String employeeEmail = currentUserDB.getEmployee() != null
                                        ? currentUserDB.getEmployee().getEmail()
                                        : currentUserDB.getEmail();

                        userLogin.setUserId(currentUserDB.getId());
                        userLogin.setEmployeeId(employeeId != null ? employeeId : 0L);
                        userLogin.setEmail(employeeEmail);
                        userLogin.setName(employeeName != null ? employeeName : "");
                        userLogin.setRole(currentUserDB.getRole());
                        userLogin.setDepartment(currentUserDB.getEmployee() != null
                                        ? currentUserDB.getEmployee().getDepartment()
                                        : null);
                        userGetAccount.setUser(userLogin);
                }
                return ResponseEntity.ok().body(userGetAccount);
        }

        @GetMapping("/refresh")
        @ApiMessage("refresh token")
        public ResponseEntity<ResponseLoginDTO> getRefreshToken(
                        @CookieValue(name = "refresh_token") String refresh_token)
                        throws CustomException {
                // check valid token
                Jwt decodedToken = this.jwtUtil.checkValidRefreshToken(refresh_token);
                String email = decodedToken.getSubject();

                // check user by token + email
                User currentUser = this.userService.getUserByRefreshTokenAndEmail(refresh_token, email);

                if (currentUser == null) {
                        throw new CustomException("Refresh Token không hợp lệ");
                }
                // new token

                ResponseLoginDTO responseLoginDTO = new ResponseLoginDTO();

                User currentUserDB = this.userService.handleGetUserByUsername(email);

                // Lấy thông tin từ Employee
                Long employeeId = currentUserDB.getEmployee() != null ? currentUserDB.getEmployee().getId() : null;
                String employeeName = currentUserDB.getEmployee() != null ? currentUserDB.getEmployee().getName()
                                : null;
                String employeeEmail = currentUserDB.getEmployee() != null ? currentUserDB.getEmployee().getEmail()
                                : currentUserDB.getEmail();

                // Tạo token với cả userId và employeeId
                Department department = currentUserDB.getEmployee() != null
                                ? currentUserDB.getEmployee().getDepartment()
                                : null;
                ResponseLoginDTO.UserToken userToken = new ResponseLoginDTO.UserToken(
                                currentUserDB.getId(),
                                employeeId != null ? employeeId : 0L,
                                employeeEmail,
                                employeeName != null ? employeeName : "",
                                currentUserDB.getRole().getName(),
                                department != null ? department.getId() : null,
                                department != null ? department.getCode() : null);

                if (currentUserDB != null) {
                        ResponseLoginDTO.UserLogin userLogin = new ResponseLoginDTO.UserLogin(
                                        currentUserDB.getId(),
                                        employeeId != null ? employeeId : 0L,
                                        employeeEmail,
                                        employeeName != null ? employeeName : "",
                                        currentUserDB.getRole(),
                                        currentUserDB.getEmployee() != null
                                                        ? currentUserDB.getEmployee().getDepartment()
                                                        : null);
                        responseLoginDTO.setUser(userLogin);
                }
                // access token
                String accessToken = this.jwtUtil.createAccessToken(email, userToken);
                responseLoginDTO.setAccessToken(accessToken);
                // new refresh token
                String new_refresh_token = this.jwtUtil.createRefreshToken(email, userToken);
                // save refresh token to db
                this.userService.updateUserRefreshToken(new_refresh_token, email);
                // set cookies
                ResponseCookie resCookie = ResponseCookie
                                .from("refresh_token", new_refresh_token)
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(refreshTokenExpiration)
                                .build();
                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, resCookie.toString())
                                .body(responseLoginDTO);
        }

        @PostMapping("/logout")
        @ApiMessage("Logout")
        public ResponseEntity<Void> logout() throws CustomException {
                String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get()
                                : "";

                if (email.equals("")) {
                        throw new CustomException("Access Token không hợp lệ");
                }
                // remove refresh token in db
                this.userService.updateUserRefreshToken(null, email);
                // remove cookie
                ResponseCookie deleteSpringCookie = ResponseCookie
                                .from("refresh_token", null)
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(0)
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, deleteSpringCookie.toString())
                                .body(null);
        }

        @GetMapping("/check")
        @ApiMessage("Kiểm tra quyền")
        public ResponseEntity<Boolean> checkPermission(@RequestParam("name") String name) {
                Long userId = SecurityUtil.extractUserId();
                if (userId == null) {
                        return ResponseEntity.ok(false);
                }
                System.out.println("name: " + name);
                System.out.println("userId: " + userId);
                return ResponseEntity.ok(permissionService.check(name, userId));
        }

}
