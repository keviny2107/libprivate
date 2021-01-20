package library;

import library.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import library.external.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.awt.print.Book;
import java.util.Optional;

@Service
public class PolicyHandler{
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @Autowired
    PointRepository pointRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverStatusUpdated_(@Payload StatusUpdated statusUpdated){
        System.out.println("##### listener  : " + statusUpdated.toJson());
        if(statusUpdated.isMe()){
            if(statusUpdated.getBookStatus().equals("returned"))
            {
                // 최초 생성인지 아닌지..
                Optional<Point> pointOptional = pointRepository.findById(statusUpdated.getMemberId());

                Point point = new Point();
                if( pointOptional.isPresent()) {
                    // 기존데이터가 있어?
                    point = pointOptional.get();
                } else {
                    point.setPoint(0L);
                }

                System.out.println("##### point  : " + point.getPoint());

                point.setId(statusUpdated.getMemberId());
                point.setPoint(point.getPoint()+1);
                point.setReqState(statusUpdated.getBookStatus());

                pointRepository.save(point);
            }

        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverReserved_(@Payload Reserved reserved){

        // 사용...
        if(reserved.isMe() && reserved.getReqPay().equals("point")){
            System.out.println("##### listener  : " + reserved.toJson());

            // 최초 생성인지 아닌지..
            Optional<Point> pointOptional = pointRepository.findById(reserved.getMemberId());

            Point point = new Point();
            if( pointOptional.isPresent()) {
                // 기존데이터가 있어?
                point = pointOptional.get();
            } else {
                point.setPoint(0L);
            }

            System.out.println("##### point  : " + point.getPoint());

            point.setId(reserved.getMemberId());
            point.setRentalId(reserved.getId());
            point.setBookId(reserved.getBookId());
            point.setPoint(point.getPoint() - 1);
            point.setReqState("reserved");

            pointRepository.save(point);

        }
    }

}
