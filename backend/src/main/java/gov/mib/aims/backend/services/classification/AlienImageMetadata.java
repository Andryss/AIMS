package gov.mib.aims.backend.services.classification;

import gov.mib.aims.backend.exception.Errors;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Читает только ориентацию EXIF, не распаковывая произвольные метаданные изображения.
 */
record AlienImageMetadata(byte[] content, Integer orientation) {

    static final int ORIENTATION_NORMAL = 1;
    static final int FLIP_HORIZONTAL = 2;
    static final int ROTATE_180 = 3;
    static final int FLIP_VERTICAL = 4;
    static final int TRANSPOSE = 5;
    static final int ROTATE_90 = 6;
    static final int TRANSVERSE = 7;
    static final int ROTATE_270 = 8;

    private static final int BYTE_MASK = 0xFF;
    private static final int PNG_SIGNATURE_BYTES = Long.BYTES;
    private static final int PNG_CHUNK_TYPE_BYTES = 4;
    private static final int PNG_CHUNK_HEADER_BYTES = Integer.BYTES + PNG_CHUNK_TYPE_BYTES;
    private static final int PNG_CHUNK_OVERHEAD_BYTES = PNG_CHUNK_HEADER_BYTES + Integer.BYTES;
    private static final int JPEG_MARKER_PREFIX = 0xFF;
    private static final int JPEG_START = 0xD8;
    private static final int JPEG_START_SCAN = 0xDA;
    private static final int JPEG_END = 0xD9;
    private static final int JPEG_TEMPORARY = 0x01;
    private static final int JPEG_RESTART_FIRST = 0xD0;
    private static final int JPEG_RESTART_LAST = 0xD7;
    private static final int JPEG_APP1 = 0xE1;
    private static final int JPEG_MARKER_BYTES = 2;
    private static final byte[] EXIF_PREAMBLE = {'E', 'x', 'i', 'f', 0, 0};
    private static final int TIFF_LITTLE_ENDIAN = 0x4949;
    private static final int TIFF_BIG_ENDIAN = 0x4D4D;
    private static final int TIFF_MAGIC = 42;
    private static final int TIFF_MAGIC_OFFSET = Short.BYTES;
    private static final int TIFF_IFD_OFFSET = Integer.BYTES;
    private static final int TIFF_HEADER_BYTES = 8;
    private static final int TIFF_ENTRY_BYTES = 12;
    private static final int TIFF_ORIENTATION_TAG = 274;
    private static final int TIFF_SHORT_TYPE = 3;
    private static final int TIFF_ORIENTATION_VALUE_COUNT = 1;
    private static final int TIFF_ENTRY_TYPE_OFFSET = Short.BYTES;
    private static final int TIFF_ENTRY_COUNT_OFFSET = Integer.BYTES;
    private static final int TIFF_ENTRY_VALUE_OFFSET = 8;

    private static final long PNG_SIGNATURE = 0x89504E470D0A1A0AL;
    private static final Set<String> PNG_IMAGE_CHUNKS = Set.of("IHDR", "PLTE", "tRNS", "IDAT", "IEND");

    static AlienImageMetadata read(byte[] bytes) {
        if (bytes.length >= PNG_SIGNATURE_BYTES && ByteBuffer.wrap(bytes).getLong() == PNG_SIGNATURE) {
            return png(bytes);
        }
        return new AlienImageMetadata(bytes, jpegOrientation(bytes));
    }

    private static AlienImageMetadata png(byte[] bytes) {
        var result = new ByteArrayOutputStream(bytes.length);
        result.write(bytes, 0, PNG_SIGNATURE_BYTES);
        Integer orientation = null;
        int offset = PNG_SIGNATURE_BYTES;
        while (offset <= bytes.length - PNG_CHUNK_OVERHEAD_BYTES) {
            int length = ByteBuffer.wrap(bytes, offset, Integer.BYTES).getInt();
            if (length < 0 || length > bytes.length - offset - PNG_CHUNK_OVERHEAD_BYTES) {
                throw Errors.validationError("Повреждённое изображение PNG");
            }
            String type = new String(bytes, offset + Integer.BYTES, PNG_CHUNK_TYPE_BYTES, StandardCharsets.US_ASCII);
            if (type.equals("eXIf")) {
                orientation = tiffOrientation(bytes, offset + PNG_CHUNK_HEADER_BYTES, length);
            }
            if (PNG_IMAGE_CHUNKS.contains(type)) {
                result.write(bytes, offset, length + PNG_CHUNK_OVERHEAD_BYTES);
            }
            offset += length + PNG_CHUNK_OVERHEAD_BYTES;
            if (type.equals("IEND")) {
                return new AlienImageMetadata(result.toByteArray(), orientation);
            }
        }
        throw Errors.validationError("Повреждённое изображение PNG");
    }

    static boolean swapsAxes(Integer orientation) {
        return orientation != null && orientation >= TRANSPOSE && orientation <= ROTATE_270;
    }

    private static boolean exifPreamble(byte[] bytes, int offset) {
        for (int index = 0; index < EXIF_PREAMBLE.length; index++) {
            if (bytes[offset + index] != EXIF_PREAMBLE[index]) {
                return false;
            }
        }
        return true;
    }

    private static Integer jpegOrientation(byte[] bytes) {
        if (bytes.length < JPEG_MARKER_BYTES + Short.BYTES
                || (bytes[0] & BYTE_MASK) != JPEG_MARKER_PREFIX
                || (bytes[1] & BYTE_MASK) != JPEG_START) {
            return null;
        }
        int offset = JPEG_MARKER_BYTES;
        while (offset < bytes.length) {
            if ((bytes[offset++] & BYTE_MASK) != JPEG_MARKER_PREFIX) {
                return null;
            }
            while (offset < bytes.length && (bytes[offset] & BYTE_MASK) == JPEG_MARKER_PREFIX) {
                offset++;
            }
            if (offset >= bytes.length) {
                return null;
            }
            int marker = bytes[offset++] & BYTE_MASK;
            if (marker == JPEG_START_SCAN || marker == JPEG_END) {
                return null;
            }
            if (marker == JPEG_TEMPORARY || marker >= JPEG_RESTART_FIRST && marker <= JPEG_RESTART_LAST) {
                continue;
            }
            if (offset > bytes.length - Short.BYTES) {
                return null;
            }
            int length = ((bytes[offset] & BYTE_MASK) << Byte.SIZE) | (bytes[offset + 1] & BYTE_MASK);
            if (length < Short.BYTES || length > bytes.length - offset) {
                return null;
            }
            if (marker == JPEG_APP1 && length >= Short.BYTES + EXIF_PREAMBLE.length
                    && exifPreamble(bytes, offset + Short.BYTES)) {
                int prefix = Short.BYTES + EXIF_PREAMBLE.length;
                Integer orientation = tiffOrientation(bytes, offset + prefix, length - prefix);
                if (orientation != null) {
                    return orientation;
                }
            }
            offset += length;
        }
        return null;
    }

    private static Integer tiffOrientation(byte[] bytes, int start, int length) {
        if (length < TIFF_HEADER_BYTES) {
            return null;
        }
        int order = ((bytes[start] & BYTE_MASK) << Byte.SIZE) | (bytes[start + 1] & BYTE_MASK);
        if (order != TIFF_LITTLE_ENDIAN && order != TIFF_BIG_ENDIAN) {
            return null;
        }
        var input = ByteBuffer.wrap(bytes, start, length).slice()
                .order(order == TIFF_LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        long directory = Integer.toUnsignedLong(input.getInt(TIFF_IFD_OFFSET));
        if (input.getShort(TIFF_MAGIC_OFFSET) != TIFF_MAGIC
                || directory < TIFF_HEADER_BYTES || directory > length - Short.BYTES) {
            return null;
        }
        int count = Short.toUnsignedInt(input.getShort((int) directory));
        int offset = (int) directory + Short.BYTES;
        if (count > (length - offset) / TIFF_ENTRY_BYTES) {
            return null;
        }
        for (int index = 0; index < count; index++, offset += TIFF_ENTRY_BYTES) {
            if (input.getShort(offset) == TIFF_ORIENTATION_TAG
                    && input.getShort(offset + TIFF_ENTRY_TYPE_OFFSET) == TIFF_SHORT_TYPE
                    && input.getInt(offset + TIFF_ENTRY_COUNT_OFFSET) == TIFF_ORIENTATION_VALUE_COUNT) {
                int orientation = Short.toUnsignedInt(input.getShort(offset + TIFF_ENTRY_VALUE_OFFSET));
                return orientation >= ORIENTATION_NORMAL && orientation <= ROTATE_270 ? orientation : null;
            }
        }
        return null;
    }
}
