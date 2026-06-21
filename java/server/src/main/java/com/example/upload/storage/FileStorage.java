package com.example.upload.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * 저장 추상화(포트). 구현체가 실제 저장 위치(로컬 FS, 추후 S3 등)를 결정한다.
 */
public interface FileStorage {

    /**
     * {@code key}로 식별되는 위치에 스트림 내용을 저장한다.
     *
     * @param key     저장 식별자(예: UUID)
     * @param content 저장할 내용 스트림 (호출 측이 닫지 않아도 구현이 닫는다)
     * @return 기록된 바이트 수 (스트리밍 업로드는 크기를 사전에 모르므로 이 값을 사용)
     */
    long store(String key, InputStream content) throws IOException;
}
