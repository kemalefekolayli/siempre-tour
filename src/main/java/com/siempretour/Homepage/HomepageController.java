package com.siempretour.Homepage;

import com.siempretour.Homepage.Dto.HomepageConfigDto;
import com.siempretour.Homepage.Dto.HomepagePublicDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HomepageController {

    private final HomepageService homepageService;

    /** Public: drives the two dynamic sections on index.html. */
    @GetMapping("/api/homepage")
    public ResponseEntity<HomepagePublicDto> getHomepage(
            @RequestParam(defaultValue = "tr") String lang) {
        return ResponseEntity.ok(homepageService.getPublic(lang));
    }

    /** Admin: load the raw config into the editor. */
    @GetMapping("/api/admin/homepage")
    public ResponseEntity<HomepageConfigDto> getAdminHomepage() {
        return ResponseEntity.ok(homepageService.getConfig());
    }

    /** Admin: save the edited config. */
    @PutMapping("/api/admin/homepage")
    public ResponseEntity<HomepageConfigDto> saveAdminHomepage(@RequestBody HomepageConfigDto dto) {
        log.info("Saving homepage config: section1={} cards, section2={} tours",
                dto.getSection1() == null ? 0 : dto.getSection1().size(),
                dto.getSection2() == null ? 0 : dto.getSection2().size());
        return ResponseEntity.ok(homepageService.saveConfig(dto));
    }
}
