package ec.edu.uta.utaped.identity;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import javax.sound.sampled.AudioSystem;
import org.junit.jupiter.api.Test;

class CaptchaAudioTests {
    @Test void everyBundledCharacterProducesBoundedAudiblePcmWithoutTextMetadata() throws Exception {
        var renderer=new CaptchaAudio();
        for(char character:CaptchaService.ALPHABET.toCharArray()) {
            byte[] bytes=renderer.render(String.valueOf(character).repeat(6));
            assertTrue(bytes.length<1048576);
            try(var audio=AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes))) {
                assertEquals(1,audio.getFormat().getChannels());assertEquals(16,audio.getFormat().getSampleSizeInBits());
                assertTrue(audio.getFrameLength()>22050*4);assertTrue(audio.getFrameLength()<22050*20);
                byte[] pcm=audio.readAllBytes();int nonzero=0;for(byte sample:pcm) if(sample!=0) nonzero++;
                assertTrue(nonzero>10000,"Audible samples for "+character);
            }
        }
    }
}
