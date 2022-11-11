package com.uci.adapter.cdn;

import java.io.InputStream;

public interface FileCdnProvider {
    public String getFileSignedUrl(String name);

    public String uploadFileFromPath(String filePath, String name);
}
