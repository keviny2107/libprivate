package library;

import library.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PolicyHandler{
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){
    }

    @Autowired
    RentalRepository rentalRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverStatusUpdated_(@Payload StatusUpdated statusUpdated){

        if(statusUpdated.isMe()){
            System.out.println("##### listener  : " + statusUpdated.toJson());

            Optional<Rental> rentalOptional = rentalRepository.findById(statusUpdated.getRendtalId());
            Rental rental = rentalOptional.get();
            rental.setReqState(statusUpdated.getBookStatus());

            rentalRepository.save(rental);

            System.out.println("##### listener Ship : " + statusUpdated.toJson());
        }
    }

}
