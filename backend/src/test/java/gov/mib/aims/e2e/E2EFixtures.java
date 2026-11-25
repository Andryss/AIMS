package gov.mib.aims.e2e;

import gov.mib.aims.backend.entity.AlienEntity;
import gov.mib.aims.backend.entity.AppUserEntity;
import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.entity.StoredFileEntity;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.FileDescriptor;
import gov.mib.aims.backend.model.IncidentEventType;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.repository.AlienRepository;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.repository.StoredFileRepository;
import gov.mib.aims.backend.services.file.FileStorage;
import gov.mib.aims.backend.services.history.EntityHistoryService;
import gov.mib.aims.backend.services.mapping.IncidentSnapshotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/** Подготовка реальных записей с коммитом перед действиями HTTP-клиента. */
@RequiredArgsConstructor
@Transactional
public class E2EFixtures {

    private final AppUserRepository users;
    private final IncidentRepository incidents;
    private final AlienRepository aliens;
    private final StoredFileRepository files;
    private final FileStorage storage;
    private final EntityHistoryService history;
    private final IncidentSnapshotMapper snapshots;

    /** Учетная запись для входа через пользовательский интерфейс. */
    public record Account(long id, String login, String password) {
        @Override
        public String toString() {
            return login;
        }
    }

    /** Данные формы в локальном времени браузера. */
    public record IncidentInput(IncidentEventType eventType, String location, LocalDateTime detectedAt,
                                String description) { }

    /** Исходное состояние инцидента для действий через интерфейс. */
    public record PreparedIncident(long id, IncidentStatus status, IncidentInput input,
                                   List<Long> attachmentFileIds) { }

    /** Возвращает готовую учётную запись из auth-seed.sql. */
    public Account account(String login, String password) {
        AppUserEntity user = users.findByLogin(login).orElseThrow(() ->
                new IllegalStateException("В тестовых данных отсутствует пользователь " + login));
        return new Account(user.getId(), login, password);
    }

    /** Возвращает исходные данные инцидента; сценарий может передать собственный IncidentInput. */
    public IncidentInput incidentInput() {
        return new IncidentInput(IncidentEventType.CONTACT_SUSPECT, "Москва, полигон E2E",
                LocalDateTime.of(2026, 10, 1, 12, 0), "Замечен неизвестный объект");
    }

    /** Имя тестового изображения для загрузки через интерфейс. */
    public String imageName() {
        return "incident-evidence.png";
    }

    /** Абсолютный путь файла для Selenium sendKeys. */
    public Path imagePath() {
        try {
            return Path.of(Objects.requireNonNull(getClass().getResource("/e2e/" + imageName())).toURI());
        } catch (URISyntaxException error) {
            throw new IllegalStateException("Некорректный путь тестового изображения", error);
        }
    }

    /** Возвращает готовый вид из alien-seed.sql по точному имени. */
    public AlienEntity alien(String name) {
        return aliens.findAll().stream().filter(item -> item.getName().equals(name))
                .findFirst().orElseThrow(() ->
                        new IllegalStateException("В тестовых данных отсутствует вид " + name));
    }

    /** Сохраняет файл и его метаданные для использования в предусловиях сценария. */
    public StoredFileEntity storeFile(Account author, Path source, String contentType) {
        String fileName = source.getFileName().toString();
        try (InputStream content = Files.newInputStream(source)) {
            FileDescriptor descriptor = storage.store(fileName, contentType, content, Files.size(source));
            return files.saveAndFlush(StoredFileEntity.builder().storageId(descriptor.storageId())
                    .fileName(fileName).contentType(contentType).fileSize(descriptor.sizeBytes())
                    .createdAt(descriptor.storedAt()).createdByUserId(author.id()).build());
        } catch (IOException error) {
            throw new UncheckedIOException("Не удалось подготовить файл " + fileName, error);
        }
    }

    /** Создаёт инцидент с заданными данными, статусом и вложениями и записывает начальную историю. */
    public PreparedIncident createIncident(Account author, IncidentStatus status, IncidentInput input,
                                           ZoneId browserZone, List<Long> attachmentFileIds) {
        LocalDateTime detectedAtUtc = input.detectedAt().atZone(browserZone)
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        IncidentEntity incident = incidents.saveAndFlush(IncidentEntity.builder()
                .status(status).eventType(input.eventType()).location(input.location()).detectedAt(detectedAtUtc)
                .description(input.description()).attachmentFileIds(List.copyOf(attachmentFileIds))
                .createdByUserId(author.id()).createdAt(detectedAtUtc).updatedAt(detectedAtUtc).build());
        history.recordChange(EntityType.INCIDENT, incident.getId(), snapshots.toStorage(incident), author.id());
        return new PreparedIncident(incident.getId(), status, input, List.copyOf(attachmentFileIds));
    }
}
