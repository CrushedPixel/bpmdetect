package processor;

import at.ofai.music.beatroot.Agent;
import at.ofai.music.beatroot.AgentList;
import at.ofai.music.beatroot.AudioProcessor;
import at.ofai.music.util.Event;
import at.ofai.music.util.EventList;
import at.ofai.music.util.Peaks;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PeakDetector extends AudioProcessor {

    private static Method BEAT_INDUCTION;
    private static Method FILL_BEATS;

    private boolean initialized = false;

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

        initialized = false;
    }

    protected void initialize() {
        //process the file
        do {
            if(this.pcmInputStream == null) {
                Peaks.normalise(this.spectralFlux);
                break;
            }

            this.processFrame();
        } while(!Thread.currentThread().isInterrupted());

        initialized = true;
    }

    public Double getBPM(double threshold, double decayRate) {
        if(!initialized) initialize();

        this.findOnsets(threshold, decayRate);

        EventList eventList = findBeats();
        if(eventList.size() == 0) return null;

        Double lastTimestamp = null;

        Iterator<Event> it = eventList.iterator();

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

    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
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
