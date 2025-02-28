package com.seruhioCode30.survival72.service;

import com.seruhioCode30.survival72.model.Subscriber;
import com.seruhioCode30.survival72.repository.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.List;

@Service
public class NewsletterService {

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Scheduled(cron = "0 0 8 1 * ?") // Ejecuta cada primer día del mes a las 8 AM
    public void sendMonthlyNewsletter() {
        List<Subscriber> subscribers = subscriberRepository.findAll();

        for (Subscriber subscriber : subscribers) {
            try {
                sendEmail(subscriber);
            } catch (MessagingException e) {
                System.err.println("Error al enviar boletín a " + subscriber.getEmail() + ": " + e.getMessage());
            }
        }
    }

    private void sendEmail(Subscriber subscriber) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        String topics = subscriber.getTopicsOfInterest();
        String body = "<p>Hola " + subscriber.getFirstName() + ",</p>" +
                "<p>Aquí está tu boletín mensual sobre: " + topics + ".</p>" +
                "<p>[Incluye contenido relevante aquí]</p>" +
                "<p>¡Gracias por ser parte de Survival72!</p>" +
                "<p>Si deseas gestionar tus preferencias o cancelar tu suscripción, haz clic en el siguiente enlace:</p>" +
                "<a href='http://localhost:5500/update.html?email=" + subscriber.getEmail() + "' " +
                "style='color: white; background-color: red; padding: 10px 15px; text-decoration: none; border-radius: 5px;'>Cancelar Suscripción</a>";

        helper.setFrom("survival72cr@gmail.com");
        helper.setTo(subscriber.getEmail());
        helper.setSubject("Boletín mensual de Survival72");
        helper.setText(body, true); // `true` habilita el formato HTML

        mailSender.send(message);
        System.out.println("Boletín enviado a: " + subscriber.getEmail());
    }


}
