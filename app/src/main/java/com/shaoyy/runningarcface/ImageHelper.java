package com.shaoyy.runningarcface;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by sim on 2018/2/25.
 */


public class ImageHelper {
    public static final int COLOR_FormatI420 = 1;
    public static final int COLOR_FormatNV21 = 2;
    public static boolean VERBOSE = true;
    private static final String TAG = "ImageHelper";

    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
    }

    public static byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        if (VERBOSE) Log.v(TAG, "get data from " + planes.length + " planes");
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            if (VERBOSE) {
                Log.v(TAG, "pixelStride " + pixelStride);
                Log.v(TAG, "rowStride " + rowStride);
                Log.v(TAG, "width " + width);
                Log.v(TAG, "height " + height);
                Log.v(TAG, "buffer size " + buffer.remaining());
            }
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
            if (VERBOSE) Log.v(TAG, "Finished reading data from plane " + i);
        }
        return data;
    }
    public static boolean getBitmap2NV21(Bitmap bitmap,byte[] bytes){
        int width = bitmap.getWidth(),
            height = bitmap.getHeight(),
            index,indexUV=width*height;
        if(bytes.length<width*height*3/2) return false;
        int r,g,b,y,u,v;
        int argb[] = new int[height*width];
        bitmap.getPixels(argb,0,width,0,0,width,height);
        if(argb[0]==-16777216) return false ;
        index=0;
        for(int i = 0;i<height;i++)
            for(int j = 0;j<width;j++){
                b = (argb[index]&0xFF0000) >>> 16;
                g = (argb[index]&0xFF00) >> 8;
                r = (argb[index]&0xFF);

                y = ( (  66 * r + 129 * g +  25 * b + 128) >> 8) +  16;
                v = ( ( -38 * r -  74 * g + 112 * b + 128) >> 8) + 128;
                u = ( ( 112 * r -  94 * g -  18 * b + 128) >> 8) + 128;

                bytes[index] = (byte) ((y < 0) ? 0 : ((y > 255) ? 255 : y));
                if (i % 2 == 0 && index % 2 == 0) {
                    bytes[indexUV++] = (byte)((v<0) ? 0 : ((v > 255) ? 255 : v));
                    bytes[indexUV++] = (byte)((u<0) ? 0 : ((u > 255) ? 255 : u));
                }
                index++;
            }
        return true;
    }
}
