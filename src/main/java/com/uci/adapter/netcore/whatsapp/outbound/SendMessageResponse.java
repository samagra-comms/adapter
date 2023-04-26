package com.uci.adapter.netcore.whatsapp.outbound;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageResponse implements Serializable {

    @Getter
    @Setter
    public class Data implements Serializable {

        @JsonAlias({"id"})
        private String identifier;
    }

    private String status;
    private String message;

    private Data data;
    
    @Getter
    @Setter
    public class Error implements Serializable {

        @JsonAlias({"code"})
        private String code;
        
        @JsonAlias({"message"})
        private String message;
    }
    
    private Error error;
}
