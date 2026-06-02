package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.FilesApi;
import gov.mib.aims.backend.generated.model.FileUploadResponse;
import gov.mib.aims.backend.model.FileDownload;
import gov.mib.aims.backend.services.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST-контроллер загрузки и скачивания файлов.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class FilesApiImpl implements FilesApi {

    private final FileService fileService;

    @Override
    public FileUploadResponse uploadFile(MultipartFile file) {
        log.info("POST /api/v1/files fileName={}", file != null ? file.getOriginalFilename() : null);
        return fileService.upload(file);
    }

    @Override
    public Resource downloadFile(Long id) {
        log.info("GET /api/v1/files/{}", id);
        FileDownload download = fileService.download(id);
        return new ByteArrayResource(download.content()) {
            @Override
            public String getFilename() {
                return download.fileName();
            }
        };
    }
}
