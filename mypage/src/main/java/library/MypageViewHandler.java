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
public class MypageViewHandler {


    @Autowired
    private MypageRepository mypageRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenReserved_then_CREATE_1 (@Payload Reserved reserved) {
        try {
            if (reserved.isMe()) {
                System.out.println("##### listener in : " + reserved.toJson());
                // view 객체 생성
                Mypage mypage = new Mypage();
                // view 객체에 이벤트의 Value 를 set 함
                mypage.setId(reserved.getId());
                mypage.setMemberId(reserved.getMemberId());
                mypage.setBookId(reserved.getBookId());
                mypage.setBookStatus("reserved");
                // view 레파지토리 save
                mypageRepository.save(mypage);
                // Mypage Create 되었단 메시지 출력.. 삭제해도됨.
                System.out.println("##### listener out : " + reserved.toJson());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenStatusUpdated_then_UPDATE_1(@Payload StatusUpdated statusUpdated) {
        try {
            System.out.println("########## listener: " + statusUpdated.toJson());
            if (statusUpdated.isMe()) {
                // view 객체 조회
                System.out.println("############ listener in : " + statusUpdated.toJson());
                Optional<Mypage> mypageOptional = mypageRepository.findById(statusUpdated.getRendtalId());
                Mypage mypage = mypageOptional.get();

                //for(Mypage mypage  : mypageList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                mypage.setBookStatus(statusUpdated.getBookStatus());
                    // view 레파지 토리에 save
                    mypageRepository.save(mypage);
                //}

                // Mypage Update 되었단 메시지 출력.. 삭제해도됨.
                System.out.println("##### listener Update : " + statusUpdated.toJson());
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
