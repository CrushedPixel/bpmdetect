package com.producerspoint.bpmdetect;

import at.ofai.music.util.EventList;
import com.producerspoint.bpmdetect.processor.PeakDetector;

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
     * @throws BPMNotRecognizedException if the tempo of the input file could not be determined.
     */
    public static int detectBPM(File file) throws IOException,
            UnsupportedAudioFileException, BPMNotRecognizedException {

        PeakDetector peakDetector = new PeakDetector(file);

        return peakDetector.detectBPM();
    }

    /**
     * Detects onset times of beats in a wav music file.
     * @param file The input file
     * @return The onsets in an EventList
     * @throws IOException
     * @throws UnsupportedAudioFileException
     */
    public static EventList detectBeats(File file) throws IOException,
            UnsupportedAudioFileException {

        PeakDetector peakDetector = new PeakDetector(file);

        return peakDetector.getBeats();
    }

}
