package com.mkwinformatica.cursomc.services;

import org.springframework.mail.SimpleMailMessage;

import com.mkwinformatica.cursomc.domain.Pedido;

public interface EmailService {

	void sendOrderConfirmationEmail(Pedido obj);
	
	void sendEmail(SimpleMailMessage msg);
}
