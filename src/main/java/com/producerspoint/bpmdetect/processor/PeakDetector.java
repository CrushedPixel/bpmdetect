package com.producerspoint.bpmdetect.processor;

import at.ofai.music.beatroot.Agent;
import at.ofai.music.beatroot.AgentList;
import at.ofai.music.beatroot.AudioProcessor;
import at.ofai.music.util.Event;
import at.ofai.music.util.EventList;
import at.ofai.music.util.Peaks;
import com.producerspoint.bpmdetect.BPMNotRecognizedException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Iterator;

public class PeakDetector extends AudioProcessor {

    private static Method BEAT_INDUCTION;
    private static Method FILL_BEATS;

    private boolean detected = false;

    private EventList beats = new EventList();

    static {
        try {
            BEAT_INDUCTION = Class.forName("at.ofai.music.beatroot.Induction").getDeclaredMethod("beatInduction", EventList.class);
            BEAT_INDUCTION.setAccessible(true);

            FILL_BEATS = Agent.class.getDeclaredMethod("fillBeats");
            FILL_BEATS.setAccessible(true);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public PeakDetector() {}

    public PeakDetector(File file) throws IOException, UnsupportedAudioFileException {
        this.setInputFile(file);
    }

    public void setInputFile(File file) throws IOException, UnsupportedAudioFileException {
        this.setInputURL(file.toURI().toURL());
    }

    public void setInputURL(URL url) throws IOException, UnsupportedAudioFileException {
        this.closeStreams();

        this.rawInputStream = AudioSystem.getAudioInputStream(url);
        this.audioFormat = this.rawInputStream.getFormat();
        this.channels = this.audioFormat.getChannels();
        this.sampleRate = this.audioFormat.getSampleRate();
        this.pcmInputStream = this.rawInputStream;

        if(this.audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || this.audioFormat.getFrameSize() != this.channels * 2 || this.audioFormat.isBigEndian()) {
            AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, this.sampleRate, 16, this.channels, this.channels * 2, this.sampleRate, false);
            this.pcmInputStream = AudioSystem.getAudioInputStream(audioFormat, this.rawInputStream);
            this.audioFormat = audioFormat;
        }

        this.init();
        this.initialize();
        this.detectPeaks();
    }

    public int detectBPM() throws BPMNotRecognizedException {
        Double bpm = getBPM();

        if(bpm == null || bpm == 0) throw new BPMNotRecognizedException();

        return (int)Math.round(bpm);
    }

    public EventList getBeats() {
        return beats;
    }

    public double getDurationInSeconds() {
        return hopTime * totalFrames;
    }

    protected void initialize() {
        detected = false;

        //process the file
        do {
            if(this.pcmInputStream == null) {
                Peaks.normalise(this.spectralFlux);
                break;
            }

            this.processFrame();
        } while(!Thread.currentThread().isInterrupted());
    }

    protected void detectPeaks() {
        if(detected) return;

        double threshold = 0.8;
        double decayRate = 0.9;

        while(beats.size() == 0 && threshold > 0) {
            this.findOnsets(threshold, decayRate);
            beats = findBeats();
            threshold -= 0.1;
        }

        detected = beats.size() > 0;
    }

    protected Double getBPM() {
        if(beats.size() < 2) return null;

        Double lastTimestamp = null;

        Iterator<Event> it = beats.iterator();

        int total = 0;
        double sum = 0;

        while(it.hasNext()) {
            Event event = it.next();

            if(lastTimestamp != null) {
                double diff = event.keyDown - lastTimestamp;
                sum += diff;
                total++;
            }

            lastTimestamp = event.keyDown;
        }

        double avgDifference = sum/total;

        return 1/avgDifference * 60;
    }

    private EventList findBeats() {
        try {
            AgentList agentList = (AgentList)BEAT_INDUCTION.invoke(null, onsetList);

            agentList.beatTrack(onsetList, -1.0D);
            Agent bestAgent = agentList.bestAgent();
            if(bestAgent != null) {
                FILL_BEATS.invoke(bestAgent);
                return bestAgent.events;
            }
            return new EventList();
        } catch(Exception e) {
            e.printStackTrace();
            return new EventList();
        }
    }
}
