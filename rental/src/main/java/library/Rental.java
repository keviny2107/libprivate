package library;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Rental_table")
public class Rental {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;            //예약번호
    private Long memberId;      // 사용자번호
    private Long bookId;        // 책번호
    private String reqState;    // 요청: "reserve", "cancel", "rental", "return"
    private String reqPay;      // 결제방식: "pay", "point"

    @PostPersist
    public void onPostPersist(){
        Reserved reserved = new Reserved();
        BeanUtils.copyProperties(this, reserved);
        reserved.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.
        if(this.reqPay.equals("pay"))
        {
            // 일반 결제로 진행(point가 아니면 다 일반결제로 가자)
            library.external.Payment payment = new library.external.Payment();

            payment.setId(this.id);
            payment.setMemberId(this.memberId);
            payment.setBookId(this.bookId);
            payment.setReqState("reserve");

            RentalApplication.applicationContext.getBean(library.external.PaymentService.class)
                    .payship(payment);
        }
    }

    @PostUpdate
    public void onPostUpdate(){

        if(this.reqState.equals("cancel") && this.reqPay.equals("point"))
        {
            // 포인트 결제취소 로 진행
            library.external.Point point = new library.external.Point();
            // mappings goes here
            point.setId(this.memberId);
            point.setRentalId(this.id);
            point.setBookId(this.bookId);
            point.setReqState("cancel");

            RentalApplication.applicationContext.getBean(library.external.PointService.class)
            .useship(point);
        }else if (this.reqState.equals("cancel") ) {
            // 일반 결제 취소
            Cancelled cancelled = new Cancelled();
            BeanUtils.copyProperties(this, cancelled);
            cancelled.publishAfterCommit();
            System.out.println("cancelled" + cancelled.toJson());
        }else if (this.reqState.equals("rental") ) {
            // 대여            
            Rentaled rentaled = new Rentaled();
            BeanUtils.copyProperties(this, rentaled);
            rentaled.publishAfterCommit();
            System.out.println("rentaled" + rentaled.toJson());
        }else if (this.reqState.equals("return") ) {
            // 반납
            Returned returned = new Returned();
            BeanUtils.copyProperties(this, returned);
            returned.publishAfterCommit();
            System.out.println("returned" + returned.toJson());
        }


    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }
    public String getReqState() {
        return reqState;
    }

    public void setReqState(String reqState) {
        this.reqState = reqState;
    }
    public String getReqPay() {
        return reqPay;
    }

    public void setReqPay(String reqPay) {
        this.reqPay = reqPay;
    }




}
