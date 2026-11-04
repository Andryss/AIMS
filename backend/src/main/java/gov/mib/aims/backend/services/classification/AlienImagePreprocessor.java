package gov.mib.aims.backend.services.classification;

import gov.mib.aims.backend.config.AlienClassifierProperties;
import gov.mib.aims.backend.exception.Errors;
import lombok.RequiredArgsConstructor;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * RGB, EXIF и bilinear с antialias, совместимые с экспортом Pillow/Torchvision.
 */
@RequiredArgsConstructor
public class AlienImagePreprocessor {

    private static final int RESIZE_SHORT_SIDE = 256;
    static final int CROP_SIZE = 224;
    static final int CHANNEL_COUNT = 3;
    private static final int CHANNEL_MAX = 255;
    private static final int GRAYSCALE_RGB_MULTIPLIER = 0x010101;
    private static final double PIXEL_CENTER_OFFSET = 0.5;
    private static final double ROUNDING_OFFSET = 0.5;
    private static final int PRECISION = 22;
    private static final float[] MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] STD = {0.229f, 0.224f, 0.225f};

    private final AlienClassifierProperties properties;

    /**
     * Проверяет фактический формат и размеры до декодирования, возвращает RGB-тензор NCHW.
     */
    public float[] prepare(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > properties.getMaxImageBytes()) {
            throw Errors.validationError("Изображение пустое или превышает допустимый размер");
        }

        var metadata = AlienImageMetadata.read(bytes);
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(metadata.content()))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw Errors.validationError("Выберите изображение PNG или JPEG");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("PNG") && !format.equalsIgnoreCase("JPEG")) {
                    throw Errors.validationError("Выберите изображение PNG или JPEG");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                checkDimensions(width, height);
                BufferedImage image = reader.read(0);
                Integer orientation = metadata.orientation();
                int[] rgb = pixels(image, orientation);
                if (AlienImageMetadata.swapsAxes(orientation)) {
                    int previousWidth = width;
                    width = height;
                    height = previousWidth;
                }

                int resizedWidth = width <= height ? RESIZE_SHORT_SIDE
                        : (int) ((long) width * RESIZE_SHORT_SIDE / height);
                int resizedHeight = height <= width ? RESIZE_SHORT_SIDE
                        : (int) ((long) height * RESIZE_SHORT_SIDE / width);
                checkDimensions(resizedWidth, resizedHeight);
                rgb = resize(rgb, width, height, resizedWidth, true);
                rgb = resize(rgb, resizedWidth, height, resizedHeight, false);
                return normalize(rgb, resizedWidth, resizedHeight);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw Errors.validationError("Не удалось прочитать изображение PNG или JPEG");
        }
    }

    private void checkDimensions(int width, int height) {
        if (width <= 0 || height <= 0 || (long) width * height > properties.getMaxPixels()) {
            throw Errors.validationError("Разрешение изображения превышает допустимое");
        }
    }

    private int[] pixels(BufferedImage image, Integer orientation) {
        int width = image.getWidth();
        int height = image.getHeight();
        int outputWidth = AlienImageMetadata.swapsAxes(orientation) ? height : width;
        int[] pixels = new int[width * height];
        boolean grayscale = image.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_GRAY
                && (image.getRaster().getSampleModel().getSampleSize(0) == Byte.SIZE
                || image.getRaster().getNumBands() == 1);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int outputX = x;
                int outputY = y;
                switch (orientation == null ? AlienImageMetadata.ORIENTATION_NORMAL : orientation) {
                    case AlienImageMetadata.FLIP_HORIZONTAL -> outputX = width - 1 - x;
                    case AlienImageMetadata.ROTATE_180 -> {
                        outputX = width - 1 - x;
                        outputY = height - 1 - y;
                    }
                    case AlienImageMetadata.FLIP_VERTICAL -> outputY = height - 1 - y;
                    case AlienImageMetadata.TRANSPOSE -> {
                        outputX = y;
                        outputY = x;
                    }
                    case AlienImageMetadata.ROTATE_90 -> {
                        outputX = height - 1 - y;
                        outputY = x;
                    }
                    case AlienImageMetadata.TRANSVERSE -> {
                        outputX = height - 1 - y;
                        outputY = width - 1 - x;
                    }
                    case AlienImageMetadata.ROTATE_270 -> {
                        outputX = y;
                        outputY = width - 1 - x;
                    }
                    default -> {
                        // Неизвестная ориентация соответствует исходному изображению.
                    }
                }
                int rgb = grayscale
                        ? Math.min(CHANNEL_MAX, image.getRaster().getSample(x, y, 0)) * GRAYSCALE_RGB_MULTIPLIER
                        : image.getRGB(x, y);
                pixels[outputY * outputWidth + outputX] = rgb;
            }
        }
        return pixels;
    }

    private int[] resize(int[] pixels, int width, int height, int target, boolean horizontal) {
        int original = horizontal ? width : height;
        if (original == target) {
            return pixels;
        }
        int outputWidth = horizontal ? target : width;
        int outputHeight = horizontal ? height : target;
        int[] output = new int[outputWidth * outputHeight];
        double scale = (double) original / target;
        double support = Math.max(1, scale);
        for (int position = 0; position < target; position++) {
            double center = (position + PIXEL_CENTER_OFFSET) * scale;
            int start = Math.max(0, (int) (center - support + ROUNDING_OFFSET));
            int end = Math.min(original, (int) (center + support + ROUNDING_OFFSET));
            double[] weights = new double[end - start];
            double total = 0;
            for (int index = start; index < end; index++) {
                weights[index - start] = Math.max(0, 1 - Math.abs((index - center + PIXEL_CENTER_OFFSET) / support));
                total += weights[index - start];
            }
            int[] coefficients = new int[weights.length];
            for (int index = 0; index < coefficients.length; index++) {
                coefficients[index] = (int) (ROUNDING_OFFSET + weights[index] / total * (1 << PRECISION));
            }
            for (int cross = 0; cross < (horizontal ? height : width); cross++) {
                int rgb = 0;
                for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
                    int sum = 1 << (PRECISION - 1);
                    for (int index = start; index < end; index++) {
                        int value = pixels[horizontal ? cross * width + index : index * width + cross];
                        sum += ((value >> (channel * Byte.SIZE)) & CHANNEL_MAX) * coefficients[index - start];
                    }
                    rgb |= Math.min(CHANNEL_MAX, Math.max(0, sum >> PRECISION)) << (channel * Byte.SIZE);
                }
                output[horizontal ? cross * outputWidth + position : position * outputWidth + cross] = rgb;
            }
        }
        return output;
    }

    private float[] normalize(int[] pixels, int width, int height) {
        int left = (int) Math.rint((width - CROP_SIZE) / 2.0);
        int top = (int) Math.rint((height - CROP_SIZE) / 2.0);
        float[] result = new float[CHANNEL_COUNT * CROP_SIZE * CROP_SIZE];
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            for (int y = 0; y < CROP_SIZE; y++) {
                for (int x = 0; x < CROP_SIZE; x++) {
                    int rgb = pixels[(y + top) * width + x + left];
                    int shift = (CHANNEL_COUNT - 1 - channel) * Byte.SIZE;
                    float value = ((rgb >> shift) & CHANNEL_MAX) / (float) CHANNEL_MAX;
                    int position = channel * CROP_SIZE * CROP_SIZE + y * CROP_SIZE + x;
                    result[position] = (value - MEAN[channel]) / STD[channel];
                }
            }
        }
        return result;
    }
}
