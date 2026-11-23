package br.org.memorias.application;

import java.io.BufferedInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static br.org.memorias.application.InputChecks.bad;
import static br.org.memorias.application.InputChecks.required;
import br.org.memorias.persistence.AttachmentRepository;
import br.org.memorias.storage.FileStorage;

@Service
public class AttachmentService {
    private static final Logger LOG = LoggerFactory.getLogger(AttachmentService.class);
    private static final long FILE_LIMIT = 20L * 1024 * 1024;
    private static final long VIDEO_LIMIT = 300L * 1024 * 1024;
    private final AttachmentRepository attachments;
    private final FileStorage files;

    public AttachmentService(AttachmentRepository attachments, FileStorage files) {
        this.attachments = attachments;
        this.files = files;
    }

    public void save(long id, MultipartFile file, List<String> createdKeys)   
    {
        if (file.isEmpty()) throw bad("O arquivo não pode estar vazio.");
        if (file.getSize() > VIDEO_LIMIT) throw bad("Vídeos MP4 devem ter até 300 MB; os demais arquivos, até 20 MB.");
        String filename = file.getOriginalFilename() == null ? "arquivo" : file.getOriginalFilename();
        filename = filename.replace('\\', '/');
        filename = required(filename.substring(filename.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", ""), 240,
                "Nome do arquivo");
        String key = UUID.randomUUID().toString();
        try (var input = new BufferedInputStream(file.getInputStream())) {
            input.mark(32);
            byte[] header = input.readNBytes(16);
            input.reset();
            String type = identify(header);
            long limit = type.equals("video/mp4") ? VIDEO_LIMIT : FILE_LIMIT;
            if (file.getSize() > limit) throw bad("Vídeos MP4 devem ter até 300 MB; os demais arquivos, até 20 MB.");
            createdKeys.add(key);
            files.put(key, input, file.getSize(), type);
            attachments.insert(id, key, filename, type);
        } catch (ResponseStatusException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível armazenar o arquivo.",
                    failure);
        }
    }

    private String identify(byte[] header) {
        if (header.length < 8)
            throw bad("Arquivo inválido.");
        String text = new String(header, StandardCharsets.ISO_8859_1);
        if (text.startsWith("%PDF-"))
            return "application/pdf";
        if ((header[0] & 255) == 255 && (header[1] & 255) == 216 && (header[2] & 255) == 255)
            return "image/jpeg";
        if (text.startsWith("\u0089PNG\r\n\u001a\n"))
            return "image/png";
        if (text.startsWith("RIFF") && text.startsWith("WEBP", 8))
            return "image/webp";
        if (text.startsWith("RIFF") && text.startsWith("WAVE", 8))
            return "audio/wav";
        if (text.startsWith("ID3") || ((header[0] & 255) == 255 && (header[1] & 224) == 224))
            return "audio/mpeg";
        if (text.startsWith("ftyp", 4))
            return "video/mp4";
        throw bad("Formato não aceito. Use JPEG, PNG, WebP, PDF, MP3, WAV ou MP4.");
    }

    void remove(List<String> createdKeys) {
        for (String key : createdKeys) {
            try {
                files.remove(key);
            } catch (Exception cleanup) {
                LOG.warn("Falha ao limpar arquivo {}", key, cleanup);
            }
        }
    }
}
