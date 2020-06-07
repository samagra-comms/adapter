package com.samagra.controller;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;
import messagerosa.core.models.XMessage;

@Slf4j
@RestController
@RequestMapping(value = "/inbound")
public class GupShupWhatsappConverter {
	@RequestMapping(value = "/gupshup/whatsapp", method = RequestMethod.POST)
	public XMessage gupShupWhatsApp(@Valid @RequestBody String message) throws JsonProcessingException {
		// call to xmsg converter in messageRosa..
		log.info("message received {}",message);
		XMessage xmessage = new XMessage();
		return xmessage;
	}
	
	@RequestMapping(value = "/gupshup/whatsapp/xmsg", method = RequestMethod.POST)
	public RestTemplate convertToAPI(XMessage xmsg) {
		return new RestTemplate();
	}
}
