package com.uci.adapter.utils;


import com.uci.utils.bot.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.MediaCategory;
import messagerosa.core.model.StylingTag;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.print.attribute.standard.Media;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class CommonUtils {
    public Integer isUrlExists(String urlString) {
        try {
            if (urlString == null) {
                return null;
            }
            URL url = new URL(urlString);
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            return huc.getResponseCode();
        } catch (Exception ex) {
            log.error("Error :" + ex.getMessage());
            return null;
        }
    }

    public String convertMessageType(String msgType) {
        switch (msgType) {
            case "image_url":
                return StylingTag.IMAGE.toString().toUpperCase();
            case "audio_url":
                return StylingTag.AUDIO.toString().toUpperCase();
            case "video_url":
                return StylingTag.VIDEO.toString().toUpperCase();
            case "document_url":
                return StylingTag.DOCUMENT.toString().toUpperCase();
            default:
                return null;
        }
    }

    public String convertMediaCategoryToMessageType(MediaCategory category) {
        switch (category) {
            case IMAGE_URL:
                return StylingTag.IMAGE.toString().toUpperCase();
            case AUDIO_URL:
                return StylingTag.AUDIO.toString().toUpperCase();
            case VIDEO_URL:
                return StylingTag.VIDEO.toString().toUpperCase();
            case FILE_URL:
                return StylingTag.DOCUMENT.toString().toUpperCase();
            default:
                return null;
        }
    }

    public static MediaCategory getMediaCategory(String mimeType) {
        MediaCategory category = null;
        if (FileUtil.isFileTypeImage(mimeType)) {
            category = MediaCategory.IMAGE;
        } else if (FileUtil.isFileTypeAudio(mimeType)) {
            category = MediaCategory.AUDIO;
        } else if (FileUtil.isFileTypeVideo(mimeType)) {
            category = MediaCategory.VIDEO;
        } else if (FileUtil.isFileTypeDocument(mimeType)) {
            category = MediaCategory.FILE;
        }
        return category;
    }

    /**
     * Get Media Category from Styling Tag
     * @param tag
     * @return
     */
    public static MediaCategory getMediaCategoryFromStylingTag(StylingTag tag) {
        MediaCategory category = null;
        if (tag.equals(StylingTag.IMAGE)) {
            category = MediaCategory.IMAGE;
        } else if (tag.equals(StylingTag.AUDIO)) {
            category = MediaCategory.AUDIO;
        } else if (tag.equals(StylingTag.VIDEO)) {
            category = MediaCategory.VIDEO;
        } else if (tag.equals(StylingTag.DOCUMENT)) {
            category = MediaCategory.FILE;
        } else if (tag.equals(StylingTag.IMAGE_URL)) {
            category = MediaCategory.IMAGE_URL;
        } else if (tag.equals(StylingTag.AUDIO_URL)) {
            category = MediaCategory.AUDIO_URL;
        } else if (tag.equals(StylingTag.VIDEO_URL)) {
            category = MediaCategory.VIDEO_URL;
        } else if (tag.equals(StylingTag.DOCUMENT_URL)) {
            category = MediaCategory.FILE_URL;
        }
        return category;
    }

    /**
     * Check if styling tag is image/audio/video type
     * @return
     */
    public static Boolean isStylingTagCdnMediaType(StylingTag stylingTag) {
        if(stylingTag.equals(StylingTag.IMAGE) || stylingTag.equals(StylingTag.AUDIO) || stylingTag.equals(StylingTag.VIDEO) || stylingTag.equals(StylingTag.DOCUMENT)) {
            return true;
        }
        return false;
    }

    /**
     * Check if styling tag is image/audio/video type
     * @return
     */
    public static Boolean isStylingTagPublicMediaType(StylingTag stylingTag) {
        if(stylingTag.equals(StylingTag.IMAGE_URL) || stylingTag.equals(StylingTag.AUDIO_URL) || stylingTag.equals(StylingTag.VIDEO_URL) || stylingTag.equals(StylingTag.DOCUMENT_URL)) {
            return true;
        }
        return false;
    }

    /**
     * Check if styling tag is list/quick reply button
     * @return
     */
    public static Boolean isStylingTagIntercativeType(StylingTag stylingTag) {
        if(stylingTag.equals(StylingTag.LIST) || stylingTag.equals(StylingTag.QUICKREPLYBTN)) {
            return true;
        }
        return false;
    }

    /**
     * Check if styling tag is image/audio/video type
     * @return
     */
    public static Boolean isMediaCategoryCdnMediaType(MediaCategory category) {
        if(category.equals(MediaCategory.IMAGE) || category.equals(MediaCategory.AUDIO) || category.equals(MediaCategory.VIDEO) || category.equals(MediaCategory.FILE)) {
            return true;
        }
        return false;
    }

    /**
     * Check if styling tag is image/audio/video type
     * @return
     */
    public static Boolean isMediaCategoryPublicMediaType(MediaCategory category) {
        if(category.equals(MediaCategory.IMAGE_URL) || category.equals(MediaCategory.AUDIO_URL) || category.equals(MediaCategory.VIDEO_URL) || category.equals(MediaCategory.FILE_URL)) {
            return true;
        }
        return false;
    }
}
