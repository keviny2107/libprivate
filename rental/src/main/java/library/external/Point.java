package library.external;

public class Point {

    private Long id;
    private Long bookId;
    private Long rentalId;
    private String reqState;
    private Long point;

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
