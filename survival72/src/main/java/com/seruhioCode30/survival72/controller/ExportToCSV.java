package com.seruhioCode30.survival72.controller;

import com.seruhioCode30.survival72.model.Subscriber;
import com.seruhioCode30.survival72.repository.SubscriberRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

@RestController
@RequestMapping("/api/export")
public class ExportToCSV {

    // Inyecta tu repositorio aquí
    private final SubscriberRepository subscriberRepository;

    public ExportToCSV(SubscriberRepository subscriberRepository) {
        this.subscriberRepository = subscriberRepository;
    }

    @GetMapping("/toCSV")
    public String exportToCSV() {
        try (FileWriter fileWriter = new FileWriter("subscribers.csv");
             PrintWriter printWriter = new PrintWriter(fileWriter)) {

            // Encabezado
            printWriter.println("ID,Nombre,Apellidos,Email,Fecha de Suscripción,Temas de Interés");

            // Datos de la base
            List<Subscriber> subscribers = subscriberRepository.findAll();
            for (Subscriber subscriber : subscribers) {
                printWriter.printf("%d,%s,%s,%s,%s,%s%n",
                        subscriber.getId(),
                        subscriber.getFirstName(),
                        subscriber.getLastName(),
                        subscriber.getEmail(),
                        subscriber.getSubscriptionDate(),
                        subscriber.getTopicsOfInterest());
            }
            return "Exportación completada. Archivo generado como 'subscribers.csv'.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error durante la exportación: " + e.getMessage();
        }
    }
}
