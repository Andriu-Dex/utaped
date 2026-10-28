package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import ec.edu.uta.utaped.identity.CaptchaAudio;
import ec.edu.uta.utaped.identity.CaptchaService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;

class CaptchaGenerationTests {
    @Test void generatedImageAndAudioUseFiveUnambiguousCharacters() throws Exception {
        var audio=mock(CaptchaAudio.class);
        when(audio.render(anyString())).thenReturn(new byte[0]);
        var service=new CaptchaService(mock(JdbcTemplate.class),audio);
        for(int i=0;i<12;i++) {
            var challenge=service.issue(new MockHttpServletRequest());
            var image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(challenge.image()));
            assertEquals(300,image.getWidth());assertEquals(88,image.getHeight());
        }
        var codes=ArgumentCaptor.forClass(String.class);
        verify(audio,times(12)).render(codes.capture());
        assertTrue(codes.getAllValues().stream().allMatch(code->code.matches("[A-HJ-NP-Z2-9]{5}")));
    }
}
