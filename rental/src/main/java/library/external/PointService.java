
package library.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="point", url="${api.point.url}")
//@FeignClient(name="point", url="http://point:8080")
public interface PointService {

    @RequestMapping(method= RequestMethod.POST, path="/points/{id}")
    public void useship(@RequestBody Point point);
}
