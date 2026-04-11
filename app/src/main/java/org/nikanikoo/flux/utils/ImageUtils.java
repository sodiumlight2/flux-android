package org.nikanikoo.flux.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.nikanikoo.flux.R;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageUtils {

    public static final int AVATAR_SIZE = 120;
    public static final int POST_IMAGE_WIDTH = 800;
    public static final int POST_IMAGE_HEIGHT = 600;
    public static final int THUMBNAIL_SIZE = 200;

    public static void loadAvatar(String url, ImageView imageView) {
        createPicassoRequest(url)
                .placeholder(R.drawable.camera_200)
                .error(R.drawable.camera_200)
                .resize(AVATAR_SIZE, AVATAR_SIZE)
                .centerCrop()
                .into(imageView);
    }

    public static void loadPostImage(String url, ImageView imageView) {
        createPicassoRequest(url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(imageView);
    }

    public static void loadThumbnail(String url, ImageView imageView) {
        createPicassoRequest(url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .resize(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                .centerCrop()
                .into(imageView);
    }

    private static RequestCreator createPicassoRequest(String url) {
        return Picasso.get()
                .load(url)
                .noFade();
    }

    public static Bitmap getOptimizedBitmap(Context context, Uri uri, int maxWidth, int maxHeight) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            int inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            
            inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            return bitmap;
        } catch (Exception e) {
            Logger.e("ImageUtils", "Error loading optimized bitmap", e);
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        
        return inSampleSize;
    }

    public static boolean isValidImageUrl(String url) {
        return url != null && !url.isEmpty() && 
               (url.startsWith("http://") || url.startsWith("https://"));
    }

    public static String extractOptimalImageUrl(JSONObject photo) {
        if (photo == null) {
            return "";
        }

        try {
            if (photo.has("sizes")) {
                JSONArray sizes = photo.getJSONArray("sizes");
                String url = extractFromSizes(sizes);
                if (!url.isEmpty()) {
                    return url;
                }
            }
            return "";
        } catch (Exception e) {
            Logger.e("ImageUtils", "Error extracting image URL", e);
            return "";
        }
    }

    public static List<String> extractImageUrls(JSONArray attachments) {
        List<String> imageUrls = new ArrayList<>();
        
        if (attachments == null) {
            return imageUrls;
        }

        try {
            for (int i = 0; i < attachments.length(); i++) {
                JSONObject attachment = attachments.getJSONObject(i);
                String type = attachment.optString("type", "");
                
                if ("photo".equals(type) && attachment.has("photo")) {
                    JSONObject photo = attachment.getJSONObject("photo");
                    String url = extractOptimalImageUrl(photo);
                    if (!url.isEmpty()) {
                        imageUrls.add(url);
                    }
                }
            }
        } catch (Exception e) {
            Logger.e("ImageUtils", "Error extracting image URLs from attachments", e);
        }

        return imageUrls;
    }

    private static String extractFromSizes(JSONArray sizes) {
        if (sizes == null || sizes.length() == 0) {
            return "";
        }

        String zUrl = "";
        String yUrl = "";
        String xUrl = "";

        try {
            for (int i = 0; i < sizes.length(); i++) {
                JSONObject size = sizes.getJSONObject(i);
                String type = size.optString("type", "");
                String url = size.optString("url", "");

                if (url.isEmpty()) continue;

                switch (type) {
                    case "z":
                        zUrl = url;
                        break;
                    case "y":
                        yUrl = url;
                        break;
                    case "x":
                        xUrl = url;
                        break;
                }
            }

            if (!zUrl.isEmpty()) return zUrl;
            if (!yUrl.isEmpty()) return yUrl;
            if (!xUrl.isEmpty()) return xUrl;

        } catch (Exception e) {
            Logger.e("ImageUtils", "Error extracting from sizes", e);
        }

        return "";
    }

    public static String extractMaxResImageUrl(JSONObject photo) {
        if (photo == null) {
            return "";
        }

        try {
            if (photo.has("sizes")) {
                JSONArray sizes = photo.getJSONArray("sizes");
                String maxResUrl = "";
                String zUrl = "";
                String yUrl = "";
                String xUrl = "";

                for (int i = 0; i < sizes.length(); i++) {
                    JSONObject size = sizes.getJSONObject(i);
                    String type = size.optString("type", "");
                    String url = size.optString("url", "");

                    if (url.isEmpty()) continue;

                    switch (type) {
                        case "UPLOADED_MAXRES":
                            maxResUrl = url;
                            break;
                        case "z":
                            zUrl = url;
                            break;
                        case "y":
                            yUrl = url;
                            break;
                        case "x":
                            xUrl = url;
                            break;
                    }
                }

                if (!maxResUrl.isEmpty()) return maxResUrl;
                if (!zUrl.isEmpty()) return zUrl;
                if (!yUrl.isEmpty()) return yUrl;
                if (!xUrl.isEmpty()) return xUrl;
            }
        } catch (Exception e) {
            Logger.e("ImageUtils", "Error extracting max res URL", e);
        }

        return "";
    }

}