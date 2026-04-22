package com.campus.storage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

@ExtendWith(MockitoExtension.class)
class SdkMinioObjectOperationsTests {

    @Mock
    private MinioClient client;

    @Test
    void getObjectMapsNoSuchObjectToFileNotFoundException() throws Exception {
        when(client.getObject(any(GetObjectArgs.class)))
                .thenThrow(noSuchObject("preview-artifacts/pptx/9/fingerprint.pdf"));

        SdkMinioObjectOperations operations = new SdkMinioObjectOperations(client);

        assertThatThrownBy(() -> operations.getObject(
                "campus-platform",
                "preview-artifacts/pptx/9/fingerprint.pdf"))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void getObjectKeepsNonMissingMinioFailuresAsIoException() throws Exception {
        when(client.getObject(any(GetObjectArgs.class)))
                .thenThrow(new IOException("boom"));

        SdkMinioObjectOperations operations = new SdkMinioObjectOperations(client);

        assertThatThrownBy(() -> operations.getObject(
                "campus-platform",
                "preview-artifacts/pptx/9/fingerprint.pdf"))
                .isInstanceOf(IOException.class)
                .isNotInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("boom");
    }

    private ErrorResponseException noSuchObject(String objectKey) {
        ErrorResponse error = new ErrorResponse(
                "NoSuchObject",
                "Object does not exist",
                "campus-platform",
                objectKey,
                "/" + objectKey,
                "request-id",
                "host-id");
        Response response = new Response.Builder()
                .request(new Request.Builder().url("http://127.0.0.1:9000/" + objectKey).build())
                .protocol(Protocol.HTTP_1_1)
                .code(404)
                .message("Not Found")
                .build();
        return new ErrorResponseException(error, response, null);
    }
}
