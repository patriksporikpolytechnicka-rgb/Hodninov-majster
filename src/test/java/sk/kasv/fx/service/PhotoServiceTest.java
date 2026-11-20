package sk.kasv.fx.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.*;
import sk.kasv.fx.security.CurrentUser;
import java.time.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PhotoServiceTest {
    ProviderPhotoRepository photos = mock(ProviderPhotoRepository.class);
    ProviderProfileRepository providers = mock(ProviderProfileRepository.class);
    CurrentUser current = mock(CurrentUser.class);
    PhotoService subject = new PhotoService(photos, providers, current, Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC));
    User providerUser = new User("p@x.sk", "h", Role.PROVIDER);
    ProviderProfile provider = new ProviderProfile();
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    @BeforeEach void setup() {
        providerUser.setId(2L); provider.setId(3L); provider.setUser(providerUser); provider.setName("Peter");
        when(current.require()).thenReturn(providerUser);
        when(providers.findByUserId(2L)).thenReturn(Optional.of(provider));
        when(providers.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }
    @Test void detectsImageTypesFromContent() {
        assertThat(PhotoService.detectType(PNG)).isEqualTo("image/png");
        assertThat(PhotoService.detectType(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0})).isEqualTo("image/jpeg");
        assertThat(PhotoService.detectType("RIFF1234WEBPVP8 ".getBytes())).isEqualTo("image/webp");
        assertThat(PhotoService.detectType("<svg onload=alert(1)>".getBytes())).isNull();
        assertThat(PhotoService.detectType("GIF89a".getBytes())).isNull();
    }
    @Test void uploadStoresPhotoAndSetsProfileImageUrl() {
        var response = subject.upload(new MockMultipartFile("file", "x.png", "image/png", PNG));
        assertThat(response.profileImage()).isEqualTo("/api/providers/3/photo?v=1000");
        verify(photos).save(argThat(p -> p.getProviderId() == 3L && p.getContentType().equals("image/png")));
    }
    @Test void fakeImageIsRejectedEvenWithImageName() {
        var fake = new MockMultipartFile("file", "x.png", "image/png", "<html>".getBytes());
        assertThatThrownBy(() -> subject.upload(fake)).isInstanceOf(ApiException.class).hasMessageContaining("JPEG, PNG or WebP");
        verify(photos, never()).save(any());
    }
    @Test void tooLargeFileIsRejected() {
        byte[] big = new byte[PhotoService.MAX_BYTES + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        assertThatThrownBy(() -> subject.upload(new MockMultipartFile("file", big))).isInstanceOf(ApiException.class).hasMessageContaining("too large");
    }
    @Test void deleteClearsProfileImage() {
        provider.setProfileImage("/api/providers/3/photo?v=1");
        subject.delete();
        verify(photos).deleteById(3L);
        assertThat(provider.getProfileImage()).isNull();
    }
}
