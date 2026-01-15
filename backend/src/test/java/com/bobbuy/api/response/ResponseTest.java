package com.bobbuy.api.response;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ResponseTest {
    @Test
    void apiResponseSuccess() {
        ApiResponse<String> resp = ApiResponse.success("Data");
        assertThat(resp.getStatus()).isEqualTo("success");
        assertThat(resp.getData()).isEqualTo("Data");
        assertThat(resp.getMeta()).isNull();
    }

    @Test
    void apiResponseSuccessWithMeta() {
        ApiMeta meta = new ApiMeta(10);
        ApiResponse<String> resp = ApiResponse.success("Data", meta);
        assertThat(resp.getMeta().getTotal()).isEqualTo(10);
    }

    @Test
    void apiErrorTest() {
        ApiError error = new ApiError("CODE", "Message");
        assertThat(error.getErrorCode()).isEqualTo("CODE");
        assertThat(error.getMessage()).isEqualTo("Message");
        assertThat(error.getStatus()).isEqualTo("error");
    }

    @Test
    void apiExceptionTest() {
        ApiException ex = new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "key", "arg1");
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(ex.getMessageKey()).isEqualTo("key");
        assertThat(ex.getMessageArgs()).containsExactly("arg1");
    }
}
