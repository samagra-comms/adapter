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
    /**
     * Check if text is a url
     * @param text
     * @return
     */
    public static Boolean isTextAUrl(String text) {
        if(text.startsWith("http://") || text.startsWith("https://")) {
            return true;
        }
        return false;
    }

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

    /**
     * Get Media Category by mime type
     * @param mimeType
     * @return
     */
    public static MediaCategory getMediaCategoryByMimeType(String mimeType) {
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
        } else if (tag.equals(StylingTag.FILE)) {
            category = MediaCategory.FILE;
        }
        return category;
    }

    /**
     * Check if styling tag is image/audio/video type
     * @return
     */
    public static Boolean isStylingTagMediaType(StylingTag stylingTag) {
        if(stylingTag.equals(StylingTag.IMAGE) || stylingTag.equals(StylingTag.AUDIO) || stylingTag.equals(StylingTag.VIDEO) || stylingTag.equals(StylingTag.FILE)) {
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
    public static Boolean isMediaCategory(MediaCategory category) {
        if(category.equals(MediaCategory.IMAGE) || category.equals(MediaCategory.AUDIO) || category.equals(MediaCategory.VIDEO) || category.equals(MediaCategory.FILE)) {
            return true;
        }
        return false;
    }
}
