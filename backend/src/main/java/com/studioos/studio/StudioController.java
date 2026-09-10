package com.studioos.studio;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.studioos.security.ActiveStudioSession;

@RestController @RequestMapping("/api/v1/studios")
public class StudioController {
    private final StudioService studios;
    private final ActiveStudioSession activeStudio;
    public StudioController(StudioService studios,ActiveStudioSession activeStudio) { this.studios=studios; this.activeStudio=activeStudio; }
    public record Create(@NotBlank @Size(max=100) String name, @NotBlank @Size(max=63) String slug,
                         @NotBlank @Size(max=64) String timezone) {}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    StudioService.View create(@Valid @RequestBody Create body,HttpSession session) {
        var studio=studios.create(body.name(),body.slug(),body.timezone());
        activeStudio.select(studio.id(),session); return studio;
    }
    @GetMapping List<StudioService.View> list() { return studios.list(); }
    @GetMapping("/{studioId}") StudioService.View get(@PathVariable UUID studioId) { return studios.get(studioId); }
    @PostMapping("/{studioId}/activate")
    StudioService.View activate(@PathVariable UUID studioId,HttpSession session) {
        activeStudio.select(studioId,session);
        return studios.get(studioId);
    }
}
