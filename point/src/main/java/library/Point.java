package library;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Point_table")
public class Point {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long bookId;
    private Long rentalId;
    private String reqState;
    private Long point;

    @PostPersist
    public void onPostPersist(){
        if(this.reqState.equals("returned"))
        {
            // 반납해서 적립을 했어요.
            Saved saved = new Saved();
            BeanUtils.copyProperties(this, saved);
            saved.publishAfterCommit();
        }
    }

    @PreUpdate
    public void onPreUpdate() {
        if(this.point < 0)
        {
            this.setPoint(0L);
            // 포인트가 0인데 결제하려고 해? 안돼. 결제해.
            library.external.Payment payment = new library.external.Payment();
            // mappings goes here
            payment.setId(this.rentalId);
            payment.setMemberId(this.id);
            payment.setBookId(this.bookId);
            payment.setReqState("reserve");
            PointApplication.applicationContext.getBean(library.external.PaymentService.class)
                    .payship(payment);
        }
        else {
            Used used = new Used();
            BeanUtils.copyProperties(this, used);
            used.publishAfterCommit();
        }

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }
    public Long getRentalId() {
        return rentalId;
    }

    public void setRentalId(Long rentalId) {
        this.rentalId = rentalId;
    }
    public String getReqState() {
        return reqState;
    }

    public void setReqState(String reqState) {
        this.reqState = reqState;
    }
    public Long getPoint() {
        return point;
    }

    public void setPoint(Long point) {
        this.point = point;
    }




}
