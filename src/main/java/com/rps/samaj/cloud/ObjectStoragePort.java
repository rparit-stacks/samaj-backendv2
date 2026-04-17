package com.rps.samaj.cloud;

import java.io.InputStream;

public interface ObjectStoragePort {

    StorageResult store(String folder, String objectKey, InputStream data, long contentLength, String contentType);

    void deleteByStorageKey(String storageKey);
}
