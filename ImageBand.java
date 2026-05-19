package com.aboaziza.bouhterrain;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.content.ContentResolver;
import java.io.InputStream;

public final class ImageBand {
    public final int width;
    public final int height;
    public final float[] gray;

    private ImageBand(int width, int height, float[] gray) {
        this.width = width;
        this.height = height;
        this.gray = gray;
    }

    public static ImageBand fromUri(ContentResolver resolver, Uri uri, int maxSide) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = resolver.openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        int sample = 1;
        while (Math.max(bounds.outWidth / sample, bounds.outHeight / sample) > maxSide) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bmp;
        try (InputStream in = resolver.openInputStream(uri)) {
            bmp = BitmapFactory.decodeStream(in, null, options);
        }
        if (bmp == null) throw new IllegalArgumentException("Unable to decode raster band image");

        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[] pixels = new int[w * h];
        bmp.getPixels(pixels, 0, w, 0, 0, w, h);
        float[] gray = new float[w * h];

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            int r = (c >> 16) & 255;
            int g = (c >> 8) & 255;
            int b = c & 255;
            gray[i] = (0.299f * r + 0.587f * g + 0.114f * b) / 255.0f;
        }
        bmp.recycle();
        return new ImageBand(w, h, gray);
    }
}
