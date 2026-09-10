package com.studioos.studio.configuration;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static com.studioos.studio.configuration.ConfigurationDto.*;

@RestController
@RequestMapping("/api/v1/studios/{studioId}")
public class ConfigurationController {
    private final ConfigurationService configurations;
    public ConfigurationController(ConfigurationService configurations) { this.configurations=configurations; }
    @GetMapping({"/onboarding","/configuration"})
    View get(@PathVariable UUID studioId) { return configurations.get(studioId); }
    @PostMapping("/onboarding/complete")
    View complete(@PathVariable UUID studioId,@Valid @RequestBody Command command) {
        return configurations.complete(studioId,command);
    }
    @PutMapping("/configuration")
    View update(@PathVariable UUID studioId,@Valid @RequestBody Command command) {
        return configurations.update(studioId,command);
    }
    @GetMapping("/configuration/lesson-policy")
    Lesson lesson(@PathVariable UUID studioId) { return configurations.lessonPolicy(studioId); }
    @GetMapping("/configuration/beauty-policy")
    Beauty beauty(@PathVariable UUID studioId) { return configurations.beautyPolicy(studioId); }
}
