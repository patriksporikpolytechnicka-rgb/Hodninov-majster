package sk.kasv.fx.controller;

import jakarta.validation.constraints.Positive;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sk.kasv.fx.service.PhotoService;
import java.time.Duration;
import static sk.kasv.fx.dto.ApiDtos.*;

@RestController
@Validated
@RequestMapping("/api/providers")
public class PhotoController {
    private final PhotoService photos;
    public PhotoController(PhotoService photos) { this.photos = photos; }

    /** multipart/form-data with the image in the field "file". */
    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProviderResponse upload(@RequestParam("file") MultipartFile file) { return photos.upload(file); }

    @DeleteMapping("/me/photo") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete() { photos.delete(); }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> get(@PathVariable @Positive Long id) {
        var photo = photos.get(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(photo.getData());
    }
}
