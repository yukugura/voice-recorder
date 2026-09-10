package jp.makot.voiceterminal.storage;

import net.neoforged.fml.loading.FMLPaths;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-owned PCM files for the simple one-shot recorder. */
public final class RecorderAudioStore {
    public static final int SAMPLE_RATE = 48_000;
    public static final int MAX_SECONDS = 60;
    public static final int MAX_BYTES = SAMPLE_RATE * MAX_SECONDS * Short.BYTES;
    private static final Path ROOT = FMLPaths.CONFIGDIR.get().resolve("voice-terminal").resolve("recorders");
    private static final ConcurrentHashMap<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    public static boolean begin(UUID owner, UUID id) {
        return SESSIONS.putIfAbsent(id, new Session(owner)) == null;
    }

    public static boolean append(UUID owner, UUID id, byte[] pcm) {
        Session session = SESSIONS.get(id);
        if (session == null || !session.owner.equals(owner) || session.data.size() + pcm.length > MAX_BYTES) return false;
        session.data.writeBytes(pcm);
        return true;
    }

    public static boolean finish(UUID owner, UUID id) {
        Session session = SESSIONS.remove(id);
        if (session == null || !session.owner.equals(owner) || session.data.size() == 0) return false;
        try {
            Files.createDirectories(ROOT);
            Files.write(ROOT.resolve(id + ".pcm"), session.data.toByteArray());
            return true;
        } catch (IOException ignored) { return false; }
    }

    public static byte[] read(UUID id) throws IOException { return Files.readAllBytes(ROOT.resolve(id + ".pcm")); }
    public static void cancel(UUID id) { SESSIONS.remove(id); }

    private record Session(UUID owner, ByteArrayOutputStream data) {
        private Session(UUID owner) { this(owner, new ByteArrayOutputStream()); }
    }
    private RecorderAudioStore() { }
}
