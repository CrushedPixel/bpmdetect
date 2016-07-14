import processor.PeakDetector;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

public class BPMDetect {

    /**
     * Detects the tempo of a wav music file.
     * @param file The input file
     * @return The tempo in BPM
     * @throws IOException
     * @throws UnsupportedAudioFileException
     */
    public static int detectBPM(File file) throws IOException, UnsupportedAudioFileException {
        PeakDetector peakDetector = new PeakDetector();
        peakDetector.setInputURL(file.toURI().toURL());

        double threshold = 0.8;

        Double bpm = null;
        while (bpm == null && threshold > 0) {
            bpm = peakDetector.getBPM(0.9, 0.9);
            threshold -= 0.1;
        }

        if(bpm == null) return 0;

        return (int)Math.round(bpm);
    }

}
