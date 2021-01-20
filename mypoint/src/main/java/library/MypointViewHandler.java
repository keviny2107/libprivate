package library;

import library.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class MypointViewHandler {


    @Autowired
    private MypointRepository mypointRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenSaved_then_CREATE_1 (@Payload Saved saved) {
        try {
            if (saved.isMe() && saved.getId() != null) {
                // view 객체 생성
                Mypoint mypoint  = new Mypoint();
                // view 객체에 이벤트의 Value 를 set 함
                mypoint.setId(saved.getId());
                mypoint.setPoint(saved.getPoint());
                // view 레파지 토리에 save
                mypointRepository.save(mypoint);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenUsed_then_UPDATE_1(@Payload Used used) {
        try {
            if (used.isMe()) {
                // view 객체 조회
                Optional<Mypoint> Optional = mypointRepository.findById(used.getId());
                if( Optional.isPresent()) {
                    Mypoint mypoint  = Optional.get();
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    mypoint.setPoint(used.getPoint());
                    // view 레파지 토리에 save
                    mypointRepository.save(mypoint);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenSaved_then_UPDATE_2(@Payload Saved saved) {
        try {
            if (saved.isMe()) {
                // view 객체 조회
                Optional<Mypoint> Optional = mypointRepository.findById(saved.getId());
                if( Optional.isPresent()) {
                    Mypoint mypoint  = Optional.get();
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    mypoint.setPoint(saved.getPoint());
                    // view 레파지 토리에 save
                    mypointRepository.save(mypoint);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}