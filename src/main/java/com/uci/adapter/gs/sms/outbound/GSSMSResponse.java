package com.uci.adapter.gs.sms.outbound;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GSSMSResponse {
    private String id;
    private String phone;
    private String details;
    private String status;
}
