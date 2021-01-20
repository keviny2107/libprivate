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
    PaymentRepository paymentRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCancelled_(@Payload Cancelled cancelled){

        if(cancelled.isMe()){
            System.out.println("##### listener  : " + cancelled.toJson());

            Optional<Payment> paymentOptional = paymentRepository.findById(cancelled.getId());
            Payment payment = paymentOptional.get();

            payment.setId(cancelled.getId());
            payment.setMemberId(cancelled.getMemberId());
            payment.setBookId(cancelled.getBookId());
            payment.setReqState(cancelled.getReqState());

            paymentRepository.save(payment);

            System.out.println("##### listener Ship : " + cancelled.toJson());
        }
    }

}
