package library;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MypointRepository extends CrudRepository<Mypoint, Long> {


}