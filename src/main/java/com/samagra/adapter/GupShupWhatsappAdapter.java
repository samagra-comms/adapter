package com.samagra.adapter;

import javax.xml.bind.JAXBException;

import org.springframework.web.client.RestTemplate;

import com.samagra.common.Request.GSWhatsAppMessage;

import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;

public class GupShupWhatsappAdapter {

	public static XMessage convertMessageToXMsg(GSWhatsAppMessage message) throws JAXBException {
		SenderReceiverInfo from = SenderReceiverInfo.builder().userIdentifier(message.getApp()).build();
		SenderReceiverInfo to = SenderReceiverInfo.builder().userIdentifier(message.getPayload().getSource()).build();

		XMessagePayload xmsgPayload = XMessagePayload.builder().text(message.getPayload().getPayload().getText())
				.build();

		XMessage xmessage = XMessage.builder().to(to).from(from).channelURI("whatsapp").providerURI("gupshup")
				.messageId(message.getPayload().getId()).timestamp(message.getTimestamp().toString())
				.payload(xmsgPayload).build();
		return xmessage;
	}

	public static RestTemplate convertToRestTemplate(XMessage xmsg) {
		
		return null;
	}
}
