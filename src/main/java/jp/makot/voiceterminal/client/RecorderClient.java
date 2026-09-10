package jp.makot.voiceterminal.client;

import jp.makot.voiceterminal.network.RecorderFinishPayload;
import jp.makot.voiceterminal.network.RecorderPlaybackPayload;
import jp.makot.voiceterminal.network.RecorderUploadPayload;
import jp.makot.voiceterminal.item.RecorderItem;
import jp.makot.voiceterminal.storage.RecorderAudioStore;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.AL10;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Client capture/playback modeled after CrazyPhone: 48kHz signed PCM via OpenAL. */
public final class RecorderClient {
    private static volatile boolean recording;
    private static long captureDevice;
    private static Thread captureThread;
    private static UUID activeId;
    private static ByteArrayOutputStream capturedAudio;
    private static ByteArrayOutputStream playback;
    private static UUID playbackId;
    private static int expectedChunks;
    private static int receivedChunks;
    private static int playbackSource;
    private static int playbackBuffer;

    public static void commandStart(UUID id) {
        if (recording) return;
        captureDevice = openCaptureDevice();
        if (captureDevice == 0L) {
            message("message.voiceterminal.recorder_microphone_error");
            PacketDistributor.sendToServer(new RecorderFinishPayload(id));
            return;
        }
        activeId = id;
        capturedAudio = new ByteArrayOutputStream();
        recording = true;
        ALC11.alcCaptureStart(captureDevice);
        captureThread = new Thread(RecorderClient::captureLoop, "voice-terminal-recorder");
        captureThread.setDaemon(true);
        captureThread.start();
        message("message.voiceterminal.recorder_started");
    }

    public static void commandStop(UUID id) {
        if (recording && id.equals(activeId)) stopAndUpload();
    }

    private static void captureLoop() {
        short[] samples = new short[2048];
        int captured = 0;
        while (recording && captureDevice != 0L && captured < RecorderAudioStore.SAMPLE_RATE * RecorderAudioStore.MAX_SECONDS) {
            int available = ALC10.alcGetInteger(captureDevice, ALC11.ALC_CAPTURE_SAMPLES);
            if (available <= 0) {
                try { Thread.sleep(10L); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); break; }
                continue;
            }
            int count = Math.min(Math.min(available, samples.length), RecorderAudioStore.SAMPLE_RATE * RecorderAudioStore.MAX_SECONDS - captured);
            ALC11.alcCaptureSamples(captureDevice, samples, count);
            byte[] pcm = new byte[count * 2];
            for (int i = 0; i < count; i++) { pcm[i * 2] = (byte) samples[i]; pcm[i * 2 + 1] = (byte) (samples[i] >>> 8); }
            synchronized (RecorderClient.class) {
                if (recording && capturedAudio != null) capturedAudio.writeBytes(pcm);
            }
            captured += count;
        }
        if (recording) stopAndUpload();
    }

    private static synchronized void stopAndUpload() {
        if (!recording) return;
        recording = false;
        if (captureDevice != 0L) {
            ALC11.alcCaptureStop(captureDevice);
            ALC11.alcCaptureCloseDevice(captureDevice);
            captureDevice = 0L;
        }
        UUID id = activeId;
        activeId = null;
        byte[] pcm = capturedAudio == null ? new byte[0] : capturedAudio.toByteArray();
        capturedAudio = null;
        // All recording packets are sent from this one call, in order.  In the
        // previous version the capture thread could still upload after FINISH.
        if (id != null) {
            for (int offset = 0; offset < pcm.length; offset += RecorderUploadPayload.CHUNK_BYTES) {
                PacketDistributor.sendToServer(new RecorderUploadPayload(id, Arrays.copyOfRange(pcm, offset, Math.min(pcm.length, offset + RecorderUploadPayload.CHUNK_BYTES))));
            }
            PacketDistributor.sendToServer(new RecorderFinishPayload(id));
        }
        message("message.voiceterminal.recorder_finished");
    }

    public static void receivePlayback(RecorderPlaybackPayload payload) {
        // A recorder can be played many times.  The recording UUID is the same
        // each time, so index zero (not just a new UUID) starts a new transfer.
        if (payload.index() == 0 || !payload.recordingId().equals(playbackId)) {
            playbackId = payload.recordingId(); expectedChunks = payload.total(); receivedChunks = 0; playback = new ByteArrayOutputStream();
        }
        if (payload.index() != receivedChunks) return;
        playback.writeBytes(payload.pcm());
        receivedChunks++;
        if (receivedChunks == expectedChunks) play(playback.toByteArray());
    }

    /** Playback belongs to the selected recorder, not to the player globally. */
    public static void tick() {
        if (playbackSource == 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || playbackId == null) {
            stopPlayback();
            return;
        }
        ItemStack selected = minecraft.player.getMainHandItem();
        CompoundTag tag = selected.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!RecorderItem.isRecorded(selected) || !tag.hasUUID(RecorderItem.ID) || !playbackId.equals(tag.getUUID(RecorderItem.ID)) ||
                AL10.alGetSourcei(playbackSource, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
            stopPlayback();
        }
    }

    private static void play(byte[] pcm) {
        stopPlayback();
        ByteBuffer data = normalisedPcm(pcm);
        playbackBuffer = AL10.alGenBuffers();
        AL10.alBufferData(playbackBuffer, AL10.AL_FORMAT_MONO16, data, RecorderAudioStore.SAMPLE_RATE);
        playbackSource = AL10.alGenSources();
        AL10.alSourcei(playbackSource, AL10.AL_BUFFER, playbackBuffer);
        // This recorder is private playback: keep it at the listener rather
        // than at world coordinate 0, 0, 0 where distance attenuation makes it
        // almost inaudible.
        AL10.alSourcei(playbackSource, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
        AL10.alSource3f(playbackSource, AL10.AL_POSITION, 0.0F, 0.0F, 0.0F);
        AL10.alSourcef(playbackSource, AL10.AL_GAIN, 1.0F);
        AL10.alSourcePlay(playbackSource);
        message("message.voiceterminal.recorder_playing");
    }

    private static void stopPlayback() {
        if (playbackSource != 0) { AL10.alSourceStop(playbackSource); AL10.alDeleteSources(playbackSource); playbackSource = 0; }
        if (playbackBuffer != 0) { AL10.alDeleteBuffers(playbackBuffer); playbackBuffer = 0; }
    }
    /** Raise quiet microphones to a usable level without boosting loud input into clipping. */
    private static ByteBuffer normalisedPcm(byte[] pcm) {
        double sumOfSquares = 0.0D;
        int peak = 0;
        int sampleCount = 0;
        for (int index = 0; index + 1 < pcm.length; index += 2) {
            int sample = (short) ((pcm[index] & 0xFF) | (pcm[index + 1] << 8));
            sumOfSquares += (double) sample * sample;
            peak = Math.max(peak, Math.abs(sample));
            sampleCount++;
        }
        // Normalize the perceived volume (RMS), but use a true peak limiter
        // afterwards. Unlike clipping/soft saturation, every original sample
        // remains linear and cannot acquire crackle from a clipped waveform.
        double rms = sampleCount == 0 ? 0.0D : Math.sqrt(sumOfSquares / sampleCount);
        float loudnessGain = rms < 1.0D ? 1.0F : Math.min(8.0F, (float) (13_500.0D / rms));
        float peakGain = peak == 0 ? 1.0F : 30_000.0F / peak;
        float gain = Math.min(loudnessGain, peakGain);
        ByteBuffer result = BufferUtils.createByteBuffer(pcm.length);
        for (int index = 0; index + 1 < pcm.length; index += 2) {
            int sample = (short) ((pcm[index] & 0xFF) | (pcm[index + 1] << 8));
            int output = Math.round(sample * gain);
            result.put((byte) output).put((byte) (output >>> 8));
        }
        return result.flip();
    }
    private static void message(String key) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.displayClientMessage(Component.translatable(key), true);
        });
    }

    /** Use the microphone picked in Plasmo Voice's device settings before falling back to Windows/OpenAL default. */
    private static long openCaptureDevice() {
        String selectedDevice = readPlasmoVoiceInputDevice();
        if (selectedDevice != null && !selectedDevice.isBlank()) {
            long selected = ALC11.alcCaptureOpenDevice(selectedDevice, RecorderAudioStore.SAMPLE_RATE, AL10.AL_FORMAT_MONO16, RecorderAudioStore.SAMPLE_RATE);
            if (selected != 0L) return selected;
        }
        return ALC11.alcCaptureOpenDevice((CharSequence) null, RecorderAudioStore.SAMPLE_RATE, AL10.AL_FORMAT_MONO16, RecorderAudioStore.SAMPLE_RATE);
    }

    private static String readPlasmoVoiceInputDevice() {
        Path config = Path.of("config", "plasmovoice", "client.toml");
        if (!Files.isRegularFile(config)) return null;
        Pattern inputDevice = Pattern.compile("(?m)^\\s*input_device\\s*=\\s*\\\"([^\\\"]*)\\\"\\s*$");
        try {
            Matcher matcher = inputDevice.matcher(Files.readString(config));
            return matcher.find() ? matcher.group(1) : null;
        } catch (IOException ignored) {
            return null;
        }
    }
    private RecorderClient() { }
}
