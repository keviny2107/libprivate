package library;

public class StatusUpdated extends AbstractEvent {

    private Long id;
    private String bookStatus;
    private Long memberID;
    private Long rendtalId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getBookStatus() {
        return bookStatus;
    }

    public void setBookStatus(String bookStatus) {
        this.bookStatus = bookStatus;
    }
    public Long getMemberId() {
        return memberID;
    }

    public void setMemberId(Long memberID) {
        this.memberID = memberID;
    }
    public Long getRendtalId() {
        return rendtalId;
    }

    public void setRendtalId(Long rendtalId) {
        this.rendtalId = rendtalId;
    }
}