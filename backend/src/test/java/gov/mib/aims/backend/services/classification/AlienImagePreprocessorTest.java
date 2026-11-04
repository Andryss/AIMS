package gov.mib.aims.backend.services.classification;

import gov.mib.aims.backend.config.AlienClassifierProperties;
import gov.mib.aims.backend.exception.BaseException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Совпадение RGB, EXIF, масштабирования и нормализации с эталонами Python.
 */
class AlienImagePreprocessorTest {

    @ParameterizedTest
    @ValueSource(strings = {"source-01.png", "source-02.png", "source-03.png", "portrait.png",
            "grayscale.png", "grayscale16.png", "rgba.png", "oriented.jpg"})
    void matchesExportedTensor(String image) throws Exception {
        var processor = new AlienImagePreprocessor(new AlienClassifierProperties());
        byte[] bytes = getClass().getResourceAsStream("/alien-classifier/" + image).readAllBytes();
        String name = image.substring(0, image.lastIndexOf('.'));
        try (var expected = getClass().getResourceAsStream("/alien-classifier/" + name + ".input.f32")) {
            var buffer = ByteBuffer.wrap(expected.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
            float[] values = new float[buffer.remaining()];
            buffer.get(values);
            float[] actual = processor.prepare(bytes);
            assertThat(actual).hasSameSizeAs(values);
            double maxError = 0;
            for (int index = 0; index < values.length; index++) {
                maxError = Math.max(maxError, Math.abs(values[index] - actual[index]));
            }
            assertThat(maxError).as(image).isLessThan(0.000001);
        }
    }

    @org.junit.jupiter.api.Test
    void skipsCompressedPngMetadataWithoutInflatingIt() throws Exception {
        var source = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(1, 1,
                java.awt.image.BufferedImage.TYPE_INT_RGB), "PNG", source);
        byte[] png = source.toByteArray();
        var text = new java.io.ByteArrayOutputStream();
        text.write(new byte[]{'n', 'o', 't', 'e', 0, 0});
        try (var deflate = new java.util.zip.DeflaterOutputStream(text)) {
            byte[] block = new byte[4096];
            for (int index = 0; index < 8192; index++) {
                deflate.write(block);
            }
        }
        byte[] data = text.toByteArray();
        byte[] type = {'z', 'T', 'X', 't'};
        var crc = new java.util.zip.CRC32();
        crc.update(type);
        crc.update(data);
        var encoded = new java.io.ByteArrayOutputStream();
        encoded.write(png, 0, 33);
        encoded.write(ByteBuffer.allocate(4).putInt(data.length).array());
        encoded.write(type);
        encoded.write(data);
        encoded.write(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
        encoded.write(png, 33, png.length - 33);
        byte[] input = encoded.toByteArray();
        var processor = new AlienImagePreprocessor(new AlienClassifierProperties());
        float[] expected = processor.prepare(png);
        var bean = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        long thread = Thread.currentThread().getId();
        long before = bean.getThreadAllocatedBytes(thread);
        float[] result = processor.prepare(input);
        long allocated = bean.getThreadAllocatedBytes(thread) - before;

        assertThat(result).isEqualTo(expected);
        assertThat(allocated).isLessThan(8 * 1024 * 1024);
    }

    @ParameterizedTest
    @ValueSource(strings = {"corrupt", "oversized", "too-many-pixels"})
    void rejectsInvalidOrOversizedImages(String variant) throws Exception {
        var properties = new AlienClassifierProperties();
        byte[] bytes = getClass().getResourceAsStream("/alien-classifier/source-01.png").readAllBytes();
        if (variant.equals("corrupt")) {
            bytes = new byte[]{1, 2, 3};
        } else if (variant.equals("oversized")) {
            properties.setMaxImageBytes(1);
        } else {
            properties.setMaxPixels(1);
        }
        byte[] content = bytes;
        assertThatThrownBy(() -> new AlienImagePreprocessor(properties).prepare(content))
                .isInstanceOf(BaseException.class).extracting("code").isEqualTo(400);
    }
}
