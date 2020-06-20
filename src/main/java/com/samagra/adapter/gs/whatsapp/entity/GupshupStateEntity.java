package com.samagra.adapter.gs.whatsapp.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Entity
@NoArgsConstructor
@Table(name = "gupshup_state")
public class GupshupStateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "phone_no")
    private String phoneNo;

    @Column(name = "state")
    private String xmlPrevious;

    @Column(name = "previous_path")
    private String previousPath;

    @Column(name = "bot_form_name")
    private String botFormName;

}
