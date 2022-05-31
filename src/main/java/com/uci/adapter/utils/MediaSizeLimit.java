package com.uci.adapter.utils;

import com.uci.utils.bot.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MediaSizeLimit {
    private final Double imageSize;
    private final Double audioSize;
    private final Double videoSize;
    private final Double documentSize;

    public MediaSizeLimit(@Value("${maxSizeForImage:}") Double imageSize,
                          @Value("${maxSizeForAudio:}") Double audioSize,
                          @Value("${maxSizeForVideo:}") Double videoSize,
                          @Value("${maxSizeForDocument:}") Double documentSize) {
        this.imageSize = imageSize;
        this.audioSize = audioSize;
        this.videoSize = videoSize;
        this.documentSize = documentSize;
    }

    public Double getMaxSizeForMedia(String mimeType){
        if(FileUtil.isFileTypeImage(mimeType) && imageSize != null){
            return (1024 * 1024 * imageSize);
        }
        else if(FileUtil.isFileTypeAudio(mimeType) && audioSize !=null){
            return (1024 * 1024 * audioSize);
        }
        else if(FileUtil.isFileTypeVideo(mimeType) && videoSize != null){
            return (1024 * 1024 * videoSize);
        }
        else if(FileUtil.isFileTypeDocument(mimeType) && documentSize != null){
            return (1024 * 1024 * documentSize);
        }
        else{
            return null;
        }
    }
}