package library;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="Mypoint_table")
public class Mypoint {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        private Long point;


        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
        public Long getPoint() {
            return point;
        }

        public void setPoint(Long point) {
            this.point = point;
        }

}
