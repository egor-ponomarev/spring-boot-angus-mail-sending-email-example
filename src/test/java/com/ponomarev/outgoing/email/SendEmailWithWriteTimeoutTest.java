package com.ponomarev.outgoing.email;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SendEmailWithWriteTimeoutTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(SendEmailWithWriteTimeoutTest.class);

	@RegisterExtension
	private static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP);

	private static final String FROM = "egor@medium.post";
	private static final String TO = "admin@medium.post";
	private static final String PASSWORD = RandomStringUtils.randomAlphabetic(6);

	@Test
	public void testEmailNotSentDueToTimeout() {
		SmtpServer smtpServer = GREEN_MAIL.getSmtp();
		GREEN_MAIL.setUser(FROM, PASSWORD);
		ScheduledThreadPoolExecutor ses = new ScheduledThreadPoolExecutor(1);
		Properties props = getMailProperties("localhost", smtpServer.getPort(), 1, ses);

		Session session = Session.getInstance(props);
		MessagingException exception = null;
		try {
			MimeMessage mimeMessage = createMimeMessage(session, FROM, TO);
			Transport.send(mimeMessage, FROM, PASSWORD);
		} catch (MessagingException e) {
			exception = e;
			LOGGER.error("Couldn't send email {}", e.getMessage(), e);
		}

		Assertions.assertNotNull(exception);
		Assertions.assertEquals("IOException while sending message", exception.getMessage());
		Assertions.assertEquals("Write timed out", exception.getNextException().getMessage());
	}

	@Test
	public void testEmailSentOK() {
		SmtpServer smtpServer = GREEN_MAIL.getSmtp();
		GREEN_MAIL.setUser(FROM, PASSWORD);

		ScheduledThreadPoolExecutor ses = new ScheduledThreadPoolExecutor(1);
		Properties props = getMailProperties("localhost", smtpServer.getPort(), 1000, ses);

		Session session = Session.getInstance(props);
		MessagingException exception = null;
		try {
			MimeMessage mimeMessage = createMimeMessage(session, FROM, TO);
			Transport.send(mimeMessage, FROM, PASSWORD);
		} catch (MessagingException e) {
			exception = e;
			LOGGER.error("Couldn't send email {}", e.getMessage(), e);
		}

		Assertions.assertNull(exception);
	}

	private Properties getMailProperties(String smtpHost,
										 int smtpPort,
										 int writeTimeout,
										 ScheduledThreadPoolExecutor ses) {
		Properties props = new Properties();
		props.put("mail.smtp.host", smtpHost);
		props.put("mail.smtp.port", smtpPort);
		props.put("mail.smtp.auth", String.valueOf(true));
		props.put("mail.smtp.writetimeout", writeTimeout);
		props.put("mail.smtp.executor.writetimeout", ses);
		return props;
	}

	private MimeMessage createMimeMessage(Session session,
										  String from,
										  String to) throws MessagingException {
		MimeMessage mimeMessage = new MimeMessage(session);
		mimeMessage.setSubject("Hello world");
		mimeMessage.setContent(createBigText(), "text/html; charset=UTF-8");
		mimeMessage.setFrom(from);
		mimeMessage.setRecipients(Message.RecipientType.TO, to);
		return mimeMessage;
	}

	private String createBigText() {
		String str = RandomStringUtils.randomAlphabetic(100);
		return StringUtils.repeat(str, 1_000_000);
	}

}
