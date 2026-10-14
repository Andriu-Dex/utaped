package ec.edu.uta.utaped.identity;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.*;
import javax.sound.sampled.AudioSystem;
import org.springframework.stereotype.Component;

@Component
public class CaptchaAudio {
    private final Map<Character,byte[]> clips;
    private final SecureRandom random=new SecureRandom();
    public CaptchaAudio() {
        var loaded=new HashMap<Character,byte[]>();
        for(char character:CaptchaService.ALPHABET.toCharArray()) {
            try(var resource=getClass().getResourceAsStream("/captcha-audio/"+character+".wav")) {
                if(resource==null) throw new IllegalStateException("Missing CAPTCHA audio clip");
                try(var audio=AudioSystem.getAudioInputStream(new BufferedInputStream(resource))) {
                    var format=audio.getFormat();
                    if(format.getSampleRate()!=22050 || format.getChannels()!=1 || format.getSampleSizeInBits()!=16 || format.isBigEndian())
                        throw new IllegalStateException("Invalid CAPTCHA audio format");
                    loaded.put(character,audio.readAllBytes());
                }
            } catch(Exception error) { throw new IllegalStateException("Cannot load CAPTCHA audio assets",error); }
        }
        clips=Map.copyOf(loaded);
    }
    public byte[] render(String answer) {
        var samples=new ByteArrayOutputStream();
        samples.writeBytes(new byte[11024]);
        for(char character:answer.toCharArray()) {
            byte[] clip=clips.get(character);
            if(clip==null) throw new IllegalArgumentException("Invalid CAPTCHA character");
            // Slightly vary timing and gain without adding noise that obscures speech.
            double speed=.96+random.nextDouble()*.08,gain=.85+random.nextDouble()*.15;
            for(double offset=0;offset<clip.length/2;offset+=speed) {
                int i=((int)offset)*2;short value=(short)((clip[i]&255)|(clip[i+1]<<8));
                short adjusted=(short)(value*gain);samples.write(adjusted&255);samples.write((adjusted>>>8)&255);
            }
            samples.writeBytes(new byte[(2205+random.nextInt(2205))*2]);
        }
        byte[] pcm=samples.toByteArray();
        return ByteBuffer.allocate(44+pcm.length).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(36+pcm.length)
            .put("WAVEfmt ".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(16)
            .putShort((short)1).putShort((short)1).putInt(22050).putInt(44100).putShort((short)2).putShort((short)16)
            .put("data".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(pcm.length).put(pcm).array();
    }
}
