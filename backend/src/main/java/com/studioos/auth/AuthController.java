package com.studioos.auth;
import com.studioos.security.*;
import com.studioos.studio.*;
import com.studioos.common.ApiException;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import java.util.*;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final SessionManager sessions;
    private final StudioAuthorization authorization;
    private final StudioService studios;
    private final ActiveStudioSession activeStudio;
    private final org.springframework.security.oauth2.client.registration.ClientRegistrationRepository providers;
    public AuthController(AuthService auth,SessionManager sessions,StudioAuthorization authorization,StudioService studios,
        org.springframework.security.oauth2.client.registration.ClientRegistrationRepository providers,ActiveStudioSession activeStudio) {
        this.auth=auth; this.sessions=sessions; this.authorization=authorization; this.studios=studios;
        this.providers=providers;
        this.activeStudio=activeStudio;
    }
    public record Signup(@NotBlank @Email @Size(max=254) String email,
                         @NotBlank @Size(max=100) String name,@NotBlank @Size(max=200) String password) {
        @Override public String toString() { return "Signup[redacted]"; }
    }
    public record Credentials(@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(max=200) String password) {
        @Override public String toString() { return "Credentials[redacted]"; }
    }
    public record EmailRequest(@NotBlank @Email @Size(max=254) String email) {}
    public record TokenRequest(@NotBlank @Size(max=100) String token) {
        @Override public String toString() { return "TokenRequest[redacted]"; }
    }
    public record Reset(@NotBlank @Size(max=100) String token,@NotBlank @Size(max=200) String password) {
        @Override public String toString() { return "Reset[redacted]"; }
    }
    public record Result(String message) {}
    public record Csrf(String headerName,String token) {
        @Override public String toString() { return "Csrf[redacted]"; }
    }
    public record Me(UUID id,String email,String name,List<StudioService.View> studios,UUID activeStudioId) {}
    @GetMapping("/csrf") Csrf csrf(CsrfToken token) { return new Csrf(token.getHeaderName(),token.getToken()); }
    @GetMapping("/providers") Map<String,Boolean> providers() {
        return Map.of("google",providers.findByRegistrationId("google")!=null,"kakao",providers.findByRegistrationId("kakao")!=null);
    }
    @PostMapping("/signup") @ResponseStatus(HttpStatus.ACCEPTED)
    Result signup(@Valid @RequestBody Signup body) {
        try { auth.signup(body.email(),body.password(),body.name()); }
        catch (org.springframework.dao.DataIntegrityViolationException race) {
            // A simultaneous duplicate signup has the same public outcome.
        }
        return sent();
    }
    @PostMapping("/login") Result login(@Valid @RequestBody Credentials body,HttpServletRequest request,HttpServletResponse response) {
        sessions.authenticate(auth.login(body.email(),body.password()),request,response); return new Result("로그인되었습니다.");
    }
    @PostMapping("/email-verification/request") @ResponseStatus(HttpStatus.ACCEPTED)
    Result verification(@Valid @RequestBody EmailRequest body) { request(body.email(),TokenStore.Kind.VERIFY); return sent(); }
    @PostMapping("/email-verification/confirm") Result verify(@Valid @RequestBody TokenRequest body) {
        auth.verify(body.token()); return new Result("이메일 인증이 완료되었습니다.");
    }
    @PostMapping("/password-reset/request") @ResponseStatus(HttpStatus.ACCEPTED)
    Result resetRequest(@Valid @RequestBody EmailRequest body) { request(body.email(),TokenStore.Kind.RESET); return sent(); }
    private void request(String email,TokenStore.Kind kind) {
        try { auth.requestToken(email,kind); }
        catch (ApiException e) {
            // Transport failures must not turn request responses into an account oracle.
            if (!e.code.equals("DELIVERY_UNAVAILABLE")) throw e;
        }
    }
    @PostMapping("/password-reset/confirm") Result reset(@Valid @RequestBody Reset body) {
        sessions.invalidateUser(auth.reset(body.token(),body.password())); return new Result("비밀번호가 변경되었습니다. 다시 로그인해 주세요.");
    }
    @GetMapping("/me") Me me(HttpSession session) {
        var user=authorization.user(); var list=studios.list();
        return new Me(user.id,user.email,user.name,list,activeStudio.current(session));
    }
    private Result sent() { return new Result("해당하는 계정이 있으면 안내 메일을 보냈습니다."); }
}
