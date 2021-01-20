package library;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MypageRepository extends CrudRepository<Mypage, Long> {

    // 회원ID(memberId), 도서ID(bookId)로 Mypage 조회
    //List<Mypage> findById(Long Id);

}
