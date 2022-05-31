package com.uci.adapter.utils;


import com.uci.utils.bot.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.MediaCategory;
import messagerosa.core.model.StylingTag;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

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


}
