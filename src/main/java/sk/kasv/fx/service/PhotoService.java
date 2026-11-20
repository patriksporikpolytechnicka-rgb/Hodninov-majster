package sk.kasv.fx.service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sk.kasv.fx.dto.ApiMapper;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.ProviderPhotoRepository;
import sk.kasv.fx.repository.ProviderProfileRepository;
import sk.kasv.fx.security.CurrentUser;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import static sk.kasv.fx.dto.ApiDtos.*;

/**
 * Handyman profile photos: JPEG, PNG or WebP up to 2 MB. The type is taken from the file's first
 * bytes, not from its name or the browser, so other files cannot be passed off as images.
 */
@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class PhotoService {
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    private final ProviderPhotoRepository photos;
    private final ProviderProfileRepository providers;
    private final CurrentUser current;
    private final Clock clock;
    public PhotoService(ProviderPhotoRepository photos, ProviderProfileRepository providers, CurrentUser current, Clock clock) {
        this.photos = photos; this.providers = providers; this.current = current; this.clock = clock;
    }

    @Transactional
    public ProviderResponse upload(MultipartFile file) {
        ProviderProfile provider = myProvider();
        byte[] data;
        try { data = file.getBytes(); } catch (IOException e) { throw new ApiException(400, "File could not be read"); }
        if (data.length == 0) throw new ApiException(400, "File is empty");
        if (data.length > MAX_BYTES) throw new ApiException(413, "File is too large (max 2 MB)");
        String type = detectType(data);
        if (type == null) throw new ApiException(415, "Only JPEG, PNG or WebP images are allowed");
        Instant now = clock.instant();
        photos.save(new ProviderPhoto(provider.getId(), type, data, now));
        // ?v= changes with every upload, so browsers do not show an old cached photo.
        provider.setProfileImage("/api/providers/" + provider.getId() + "/photo?v=" + now.getEpochSecond());
        return ApiMapper.provider(providers.saveAndFlush(provider));
    }

    @Transactional
    public void delete() {
        ProviderProfile provider = myProvider();
        photos.deleteById(provider.getId());
        provider.setProfileImage(null);
        providers.saveAndFlush(provider);
    }

    public ProviderPhoto get(Long providerId) {
        ProviderProfile provider = providers.findById(providerId).orElseThrow(() -> new ApiException(404, "Provider not found"));
        if (provider.getUser().isBlocked()) throw new ApiException(404, "Provider not found");
        return photos.findById(providerId).orElseThrow(() -> new ApiException(404, "Photo not found"));
    }

    /** image/jpeg, image/png or image/webp from the file signature; null for anything else. */
    static String detectType(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xff) == 0xFF && (d[1] & 0xff) == 0xD8 && (d[2] & 0xff) == 0xFF) return "image/jpeg";
        if (d.length >= 8 && (d[0] & 0xff) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) return "image/png";
        if (d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') return "image/webp";
        return null;
    }

    private ProviderProfile myProvider() {
        User user = current.require();
        if (user.getRole() != Role.PROVIDER) throw new ApiException(403, "Role PROVIDER required");
        return providers.findByUserId(user.getId()).orElseThrow(() -> new ApiException(404, "Create your provider profile first"));
    }
}
