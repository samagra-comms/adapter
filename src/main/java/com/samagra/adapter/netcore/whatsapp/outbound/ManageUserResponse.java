package com.samagra.adapter.netcore.whatsapp.outbound;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.sun.istack.Nullable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManageUserResponse {

    @Getter
    @Setter
    public class Error {
        private String code;
        private String message;
    }

    private String status;

    @Nullable
    private String message;

    @Nullable
    private Error error;
}
