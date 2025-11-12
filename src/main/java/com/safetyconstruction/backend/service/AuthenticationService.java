package com.safetyconstruction.backend.service;

import java.sql.SQLIntegrityConstraintViolationException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.safetyconstruction.backend.dto.request.*;
import com.safetyconstruction.backend.dto.response.AuthenticationResponse;
import com.safetyconstruction.backend.dto.response.IntrospectResponse;
import com.safetyconstruction.backend.entity.InvalidatedToken;
import com.safetyconstruction.backend.entity.Role;
import com.safetyconstruction.backend.entity.User;
import com.safetyconstruction.backend.exception.AppException;
import com.safetyconstruction.backend.exception.ErrorCode;
import com.safetyconstruction.backend.mapper.UserMapper;
import com.safetyconstruction.backend.repository.InvalidatedTokenRepository;
import com.safetyconstruction.backend.repository.RoleRepository;
import com.safetyconstruction.backend.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    InvalidatedTokenRepository invalidatedTokenRepository;
    PasswordEncoder passwordEncoder;
    //    RedisTemplate<String, Object> redisTemplate;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}") // 3600
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}") // 86400
    protected long REFRESHABLE_DURATION;

    public AuthenticationResponse register(UserCreationRequest request) {
        log.info("Service: Registering new user {}", request.getName());

        // 1. Kiểm tra (Check) Trùng lặp (Tốt hơn là bắt lỗi)
        if (userRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.INVALID_EMAIL);
        }

        User user = userMapper.toUser(request);

        // 2. Sửa: Dùng PasswordEncoder đã được tiêm (injected)
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 3. Sửa: Gán Role "USER" mặc định (Giả định Role là Entity)
        Role userRole = roleRepository
                .findByName("USER")
                .orElseThrow(() -> new RuntimeException("FATAL: Default USER Role not found in database."));
        user.setRoles(new HashSet<>(Collections.singleton(userRole)));

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Dùng logic bắt lỗi mạnh mẽ (robust) từ UserService
            handleDataIntegrityException(e);
        }

        // 4. Sửa: Tạo CẢ HAI token (Access và Refresh)
        String token = generateToken(user, false); // Access Token
        String refreshToken = generateToken(user, true); // Refresh Token

        // 5. Sửa: Trả về CẢ HAI token
        return AuthenticationResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    private void handleDataIntegrityException(DataIntegrityViolationException ex) {
        Throwable rootCause = ex.getRootCause();
        String errorMessage = null;

        if (rootCause instanceof ConstraintViolationException) {
            errorMessage =
                    ((ConstraintViolationException) rootCause).getSQLException().getMessage();
        } else if (rootCause instanceof SQLIntegrityConstraintViolationException) {
            errorMessage = ((SQLIntegrityConstraintViolationException) rootCause).getMessage();
        }

        if (errorMessage != null) {
            log.error("->>>>> SQL Error Message: " + errorMessage);
            if (errorMessage.contains("user.email")) {
                throw new AppException(ErrorCode.INVALID_EMAIL);
            }
            if (errorMessage.contains("user.PRIMARY") || errorMessage.contains("user.name")) {
                throw new AppException(ErrorCode.USER_EXISTED);
            }
        }
        throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;

        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder().valid(isValid).build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

        // Check for account lock due to too many failed attempts
        //        String key = "login_attempt:" + request.getName();
        //        Integer failedAttempts = (Integer) redisTemplate.opsForValue().get(key);
        //        if (failedAttempts != null && failedAttempts >= 5) {
        //            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        //        }

        var user = userRepository
                .findByName(request.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());
        log.info("Authenticating user: {}", request.getName());
        if (!authenticated) throw new AppException(ErrorCode.UNAUTHENTICATED);
        var token = generateToken(user, false);
        // 2. Tạo Refresh Token (hạn dài)
        var refreshToken = generateToken(user, true);

        return AuthenticationResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signToken = verifyToken(request.getToken(), true);

            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken =
                    InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException exception) {
            log.info("Token already expired");
        }
    }

    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        var signedJWT = verifyToken(request.getToken(), true);

        var jit = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken =
                InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

        invalidatedTokenRepository.save(invalidatedToken);

        var username = signedJWT.getJWTClaimsSet().getSubject();

        var user = userRepository.findByName(username).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        var token = generateToken(user, false); // Access token mới
        var refreshToken = generateToken(user, true); // Refresh token mới

        return AuthenticationResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        // Kiểm tra xem token có hợp lệ VÀ còn hạn không
        if (!(verified && expiryTime.after(new Date()))) throw new AppException(ErrorCode.UNAUTHENTICATED);

        // Kiểm tra xem token có bị 'logout' (vô hiệu hóa) không
        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }

    private String generateToken(User user, boolean isRefresh) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        // 1. Quyết định thời gian hết hạn
        long duration = isRefresh ? REFRESHABLE_DURATION : VALID_DURATION;

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(user.getName())
                .issuer("ssims.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(duration, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString());

        // 2. Chỉ thêm 'scope' (quyền) cho Access Token
        // Refresh Token không cần quyền, nó chỉ dùng để xác thực
        if (!isRefresh) {
            builder.claim("scope", buildScope(user));
        }

        JWTClaimsSet jwtClaimsSet = builder.build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles()))
            user.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions()))
                    role.getPermissions().forEach(permission -> {
                        stringJoiner.add(permission.getName());
                    });
            });

        return stringJoiner.toString();
    }
}
