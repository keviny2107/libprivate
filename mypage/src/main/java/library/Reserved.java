package library;

public class Reserved extends AbstractEvent {

    private Long id;
    private Long memberId;
    private Long bookId;
    private String reqState;
    private String reqPay;

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