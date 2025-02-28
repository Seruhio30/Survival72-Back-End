package com.seruhioCode30.survival72.controller;

import com.seruhioCode30.survival72.model.Subscriber;
import com.seruhioCode30.survival72.repository.SubscriberRepository;
import com.seruhioCode30.survival72.service.EmailService;
import com.seruhioCode30.survival72.service.NewsletterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@CrossOrigin(origins = "http://localhost:5500") // Permitir solicitudes desde tu frontend
@RestController
@RequestMapping("/api/subscribers")
public class SubscriberController {

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private EmailService emailService;

    @PostMapping("/subscribe")
    public Subscriber subscribe(@RequestBody Subscriber subscriber) {
        subscriber.setSubscriptionDate(LocalDate.now());
        Subscriber savedSubscriber = subscriberRepository.save(subscriber);

        // Enviar correo de confirmación
        String subject = "Bienvenido a Survival72";
        String body = "Gracias por suscribirte a Survival72, " + subscriber.getFirstName() + " " + subscriber.getLastName() +
                ". Recibirás actualizaciones importantes sobre preparación para emergencias.";
        emailService.sendSubscriptionEmail(subscriber.getEmail(), subject, body);

        return savedSubscriber;
    }

    // Obtener todos los suscriptores
    @GetMapping
    public List<Subscriber> getAllSubscribers() {
        return subscriberRepository.findAll();
    }

    // Actualizar intereses
    @PutMapping("/update")
    public ResponseEntity<String> updateSubscriber(@RequestBody Subscriber updatedSubscriber) {
        Optional<Subscriber> subscriberOptional = subscriberRepository.findByEmail(updatedSubscriber.getEmail());

        if (subscriberOptional.isPresent()) {
            Subscriber existingSubscriber = subscriberOptional.get();
            existingSubscriber.setTopicsOfInterest(updatedSubscriber.getTopicsOfInterest());
            subscriberRepository.save(existingSubscriber);
            return ResponseEntity.ok("Intereses actualizados con éxito.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se encontró el suscriptor.");
        }
    }

    // Cancelar suscripción
    @DeleteMapping("/cancel")
    public ResponseEntity<String> cancelSubscription(@RequestParam String email) {
        Optional<Subscriber> subscriberOptional = subscriberRepository.findByEmail(email);

        if (subscriberOptional.isPresent()) {
            subscriberRepository.delete(subscriberOptional.get());
            return ResponseEntity.ok("Suscripción cancelada con éxito.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se encontró el suscriptor.");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateSubscriber(@PathVariable Long id, @RequestBody Subscriber updatedSubscriber) {
        Optional<Subscriber> subscriberOptional = subscriberRepository.findById(id);

        if (subscriberOptional.isPresent()) {
            Subscriber existingSubscriber = subscriberOptional.get();

            // Obtener los temas existentes
            String existingTopics = existingSubscriber.getTopicsOfInterest();
            String newTopics = updatedSubscriber.getTopicsOfInterest();

            // Combinar temas de interés existentes con los nuevos
            if (newTopics != null && !newTopics.isEmpty()) {
                if (existingTopics == null || existingTopics.isEmpty()) {
                    existingSubscriber.setTopicsOfInterest(newTopics);
                } else {
                    // Evitar duplicados al concatenar
                    String[] existingTopicsArray = existingTopics.split(",\\s*");
                    String[] newTopicsArray = newTopics.split(",\\s*");

                    Set<String> combinedTopics = new LinkedHashSet<>(Arrays.asList(existingTopicsArray));
                    combinedTopics.addAll(Arrays.asList(newTopicsArray));

                    existingSubscriber.setTopicsOfInterest(String.join(", ", combinedTopics));
                }
            }

            subscriberRepository.save(existingSubscriber);
            return ResponseEntity.ok("Suscriptor actualizado con éxito.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se encontró el suscriptor.");
        }
    }
    @RestController
    @RequestMapping("/test-newsletter")
    public class NewsletterTestController {

        @Autowired
        private NewsletterService newsletterService;

        @GetMapping("/send")
        public ResponseEntity<String> sendNewsletter() {
            newsletterService.sendMonthlyNewsletter();
            return ResponseEntity.ok("Boletines enviados.");
        }
    }

}


