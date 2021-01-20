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
    BookRepository bookRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaid_(@Payload Paid paid){
        // 결제완료(예약)

        if(paid.isMe()){
            System.out.println("##### listener  : " + paid.toJson());

            Book book = new Book();

            book.setId(paid.getBookId());
            book.setMemberId(paid.getMemberId());
            book.setRendtalId(paid.getId());
            book.setBookStatus("reserved");

            bookRepository.save(book);
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverRefunded_(@Payload Refunded refunded){
        // 예약취소
        if(refunded.isMe()){
            System.out.println("##### listener  : " + refunded.toJson());

            Optional<Book> bookOptional = bookRepository.findById(refunded.getId());
            Book book = bookOptional.get();

            book.setId(refunded.getBookId());
            book.setMemberId(refunded.getMemberId());
            book.setRendtalId(refunded.getId());

            book.setBookStatus("refunded");

            bookRepository.save(book);
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverRentaled_(@Payload Rentaled rentaled){

        if(rentaled.isMe()){
            // 대여
            System.out.println("##### listener  : " + rentaled.toJson());

            Optional<Book> bookOptional = bookRepository.findById(rentaled.getBookId());
            Book book = bookOptional.get();

            book.setId(rentaled.getBookId());
            book.setMemberId(rentaled.getMemberId());
            book.setRendtalId(rentaled.getId());

            book.setBookStatus("rentaled");

            bookRepository.save(book);
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverReturned_(@Payload Returned returned){

        if(returned.isMe()){
            System.out.println("##### listener  : " + returned.toJson());
            // 반납
            Optional<Book> bookOptional = bookRepository.findById(returned.getBookId());
            Book book = bookOptional.get();

            book.setId(returned.getBookId());
            book.setMemberId(returned.getMemberId());
            book.setRendtalId(returned.getId());

            book.setBookStatus("returned");

            bookRepository.save(book);
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverUsed_(@Payload Used used){

        if(used.isMe()){
            System.out.println("##### listener  : " + used.toJson());

            // 포인트 결제(에약)
            Book book = new Book();

            book.setId(used.getBookId());
            book.setMemberId(used.getId());
            book.setRendtalId(used.getRentalId());
            book.setBookStatus("reserved");

            bookRepository.save(book);
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverUsecanceled_(@Payload Usecanceled usecanceled){

        if(usecanceled.isMe()){
            System.out.println("##### listener  : " + usecanceled.toJson());

            // 포인트 결제취소(에약)
            Optional<Book> bookOptional = bookRepository.findById(usecanceled.getId());
            Book book = bookOptional.get();

            book.setId(usecanceled.getBookId());
            book.setMemberId(usecanceled.getId());
            book.setRendtalId(usecanceled.getRentalId());

            book.setBookStatus("refunded");

            bookRepository.save(book);
        }
    }


}
