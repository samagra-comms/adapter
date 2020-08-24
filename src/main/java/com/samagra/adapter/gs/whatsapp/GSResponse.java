package com.samagra.adapter.gs.whatsapp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public  class GSResponse {
    private String id;
    private String phone;
    private String details;
    private String status;
}
