# 도서 대여 서비스
 
# Table of contents
 
 - [도서 대여 서비스](#---)
   - [서비스 시나리오](#서비스-시나리오)  
   - [분석/설계](#분석설계)
   - [구현:](#구현-)
     - [DDD 의 적용](#ddd-의-적용)
     - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
     - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
   - [운영](#운영)
     - [CI/CD 설정](#cicd설정)
     - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
     - [오토스케일 아웃](#오토스케일-아웃)
     - [무정지 재배포](#무정지-재배포)
 - [기능 추가 포인트 시스템](#기능-추가-포인트-시스템)    
 
# 서비스 시나리오
 
 기능적 요구사항
 1. 사용자가 도서를 예약한다.
 1. 도서 예약 시 결제가 완료되어야 한다.
 1. 사용자가 예약 중인 도서를 대여한다.
 1. 사용자가 대여 중인 도서를 반납한다.
 1. 사용자가 예약을 취소할 수 있다.
 1. 도서 예약 취소 시에는 결제가 취소된다.
 1. 사용자가 예약/대여 상태를 확인할 수 있다.
 
 비기능적 요구사항
 1. 트랜잭션
     1. 결제가 되지 않은 경우 예약할 수 없다 (Sync 호출)
 1. 장애격리
     1. 도서관리 기능이 수행되지 않더라도 대여/예약은 365일 24시간 받을 수 있어야 한다  Async (event-driven), Eventual Consistency
     1. 결제 시스템이 과중되면 사용자를 잠시동안 받지 않고 예약을 잠시후에 하도록 유도한다  Circuit breaker, fallback
 1. 성능
     1. 사용자는 MyPage에서 본인 예약 및 대여 도서의 목록과 상태를 확인할 수 있어야한다 CQRS
 
 
 # 분석/설계
 
## Event Storming 결과
 * MSAEz 로 모델링한 이벤트스토밍 결과: 
 ![image](https://user-images.githubusercontent.com/53402465/104991785-e6c69180-5a62-11eb-9478-19b0582d4201.PNG)  



## 헥사고날 아키텍처 다이어그램 도출
    
![image](https://user-images.githubusercontent.com/53402465/104991783-e5956480-5a62-11eb-91e6-69020468ab61.PNG)


    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐


# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)

```
cd book
mvn spring-boot:run

cd mypage
mvn spring-boot:run 

cd payment
mvn spring-boot:run  

cd rental
mvn spring-boot:run
```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (예시는 book 마이크로 서비스). 이때 가능한 현업에서 사용하는 언어 (유비쿼터스 랭귀지)를 그대로 사용하려고 노력했다. 

```
package library;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Book_table")
public class Book {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long bookId;
    private String bookStatus;
    private Long memberId;
    private Long rendtalId;

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
    public String getBookStatus() {
        return bookStatus;
    }

    public void setBookStatus(String bookStatus) {
        this.bookStatus = bookStatus;
    }
    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
    public Long getRendtalId() {
        return rendtalId;
    }

    public void setRendtalId(Long rendtalId) {
        this.rendtalId = rendtalId;
    }
}


```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package library;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface BookRepository extends PagingAndSortingRepository<Book, Long>{
}
```

- 적용 후 REST API 의 테스트 시나리오

1. SAGA
2. CQRS
3. Correlation

```
# 사용자가 도서를 예약한다
http POST http://20.194.7.119:8080/rentals memberId=1 bookId=1
```

 ![image](https://user-images.githubusercontent.com/53402465/105122171-10da8b00-5b19-11eb-8ed0-664590206d60.png)

```
#Rental 내역 확인
http GET http://20.194.7.119:8080/rentals
```

 ![image](https://user-images.githubusercontent.com/53402465/105122172-10da8b00-5b19-11eb-8fcb-afa4002c7a42.png)

```
# 사용자 예약 후 결제확인
http GET http://20.194.7.119:8080/payments
```

![image](https://user-images.githubusercontent.com/53402465/105122173-11732180-5b19-11eb-843a-44b1c61fdbd2.png)

```
# 사용자 예약한 책 상태 확인
http GET http://20.194.7.119:8080/books
```

![image](https://user-images.githubusercontent.com/53402465/105122175-11732180-5b19-11eb-819d-6a8f95dd2036.png)

```
# 사용자 도서 예약취소
http PATCH http://20.194.7.119:8080/rentals/1 reqState="cancel" 
```

![image](https://user-images.githubusercontent.com/53402465/105122166-0f10c780-5b19-11eb-8693-f5626f980855.png)

```
# 결제취소 확인
http GET http://20.194.7.119:8080/rentals/1
```

![image](https://user-images.githubusercontent.com/53402465/105122169-1041f480-5b19-11eb-99c9-d4597c9fe0a8.png)

```
# 사용자 예약 취소한 책 상태 확인
http GET http://20.194.7.119:8080/books
```

![image](https://user-images.githubusercontent.com/53402465/105122170-1041f480-5b19-11eb-9496-20c40fcfeffb.png)

```
#마이페이지 확인
http GET http://20.194.7.119:8080/mypages/1
```

![image](https://user-images.githubusercontent.com/75401893/105123042-b4786b00-5b1a-11eb-9f8c-0b0b20a7e8d9.png)

```
# 사용자 도서 예약
http POST http://20.194.7.119:8080/rentals memberId=1 bookId=1 
```

![image](https://user-images.githubusercontent.com/53402465/105122636-ee953d00-5b19-11eb-8147-d9e68ba72f74.png)

```
# 사용자 도서 대여
http PATCH http://20.194.7.119:8080/rentals/2 reqState="rental" 
```

![image](https://user-images.githubusercontent.com/53402465/105122637-ee953d00-5b19-11eb-89f2-002a644ebcf0.png)

```
# 사용자 대여한 책 상태 확인
http GET http://20.194.7.119:8080/books/
```

![image](https://user-images.githubusercontent.com/53402465/105122640-efc66a00-5b19-11eb-9ca2-3671c7b3af80.png)

```
# 사용자 도서 반납
http PATCH http://20.194.7.119:8080/rentals/2 reqState="return" 
```

![image](https://user-images.githubusercontent.com/53402465/105122633-ed641000-5b19-11eb-8568-93f892300c96.png)

```
# 사용자 반납한 책 상태 확인
http GET http://20.194.7.119:8080/books
```

![image](https://user-images.githubusercontent.com/53402465/105122635-edfca680-5b19-11eb-81c2-5a3b8876ede8.png)

```
#마이페이지 확인
http GET http://20.194.7.119:8080/mypages/2
```

![image](https://user-images.githubusercontent.com/75401893/105123049-b8a48880-5b1a-11eb-833e-a44ac80e983a.png)


4. Request / Response

```

## 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 대여(rental)->결제(payment) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- 결제서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

```
# (rental) PaymentService.java 내용중

@FeignClient(name="payment", url="${api.payment.url}")
public interface PaymentService {

    @RequestMapping(method= RequestMethod.POST, path="/payments")//, fallback = PaymentServiceFallback.class)
    public void payship(@RequestBody Payment payment);

}

```

- 예약 이후(@PostPersist) 결제를 요청하도록 처리
```
# Rental.java

    @PostPersist
    public void onPostPersist(){
        Reserved reserved = new Reserved();
        BeanUtils.copyProperties(this, reserved);
        reserved.publishAfterCommit();


        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.
        library.external.Payment payment = new library.external.Payment();
        // mappings goes here
        payment.setId(this.id);
        payment.setMemberId(this.memberId);
        payment.setBookId(this.bookId);
        payment.setReqState("reserve");

        RentalApplication.applicationContext.getBean(library.external.PaymentService.class)
            .payship(payment);
    }
```
- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 주문도 못받는다는 것을 확인:


# 결제 (payment) 서비스를 잠시 내려놓음

#주문처리
http http://localhost:8081/rentals memberId=1 bookId=1  #Fail 
```
:
    ![image](https://user-images.githubusercontent.com/53402465/105120797-3e720500-5b16-11eb-8b2f-d51aea5def12.PNG)

```
#결제서비스 재기동
cd payment
mvn spring-boot:run

#주문처리
http http://localhost:8081/rentals memberId=1 bookId=1   #Success
```
:
    ![image](https://user-images.githubusercontent.com/53402465/105120799-3f0a9b80-5b16-11eb-883e-51588b5d6804.PNG)

- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)



## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트

결제 이후 도서관리(book)시스템으로 결제 완료 여부를 알려주는 행위는 비 동기식으로 처리하여 도서관리 시스템의 처리로 인해 결제주문이 블로킹 되지 않도록 처리한다.
- 이를 위하여 결제이력에 기록을 남긴 후에 곧바로 결제승인(paid)이 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
# Payment.java

@Entity
@Table(name="Payment_table")
public class Payment {

 ...
    @PostPersist
    public void onPostPersist(){
        Paid paid = new Paid();
        BeanUtils.copyProperties(this, paid);
        paid.publishAfterCommit();
 ...
}
```
- 도서관리 서비스는 결제완료 이벤트를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```
# PolicyHandler.java (book)
...

@Service
public class PolicyHandler{

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaid_(@Payload Paid paid){
        // 결제완료(예약)
        if(paid.isMe()){
            Book book = new Book();
            book.setId(paid.getBookId());
            book.setMemberId(paid.getMemberId());
            book.setRendtalId(paid.getId());
            book.setBookStatus("reserved");

            bookRepository.save(book);
        }
    }
}

```


도서관리 시스템은 대여/결제와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 도서관리시스템이 유지보수로 인해 잠시 내려간 상태라도 주문을 받는데 문제가 없다:
```
# 도서관리 서비스 (book) 를 잠시 내려놓음

#주문처리
http http://localhost:8081/rentals memberId=1 bookId=1  #Success  


```
#주문상태 확인

:
    ![image](https://user-images.githubusercontent.com/53402465/105119392-96f3d300-5b13-11eb-99b0-f9a79bdde8b7.PNG)

``` 

#상점 서비스 기동
cd book
mvn spring-boot:run

#주문상태 확인
http localhost:8080/rentals     # 모든 주문의 상태가 "reserved"으로 확인
```
:
    ![image](https://user-images.githubusercontent.com/53402465/105119394-978c6980-5b13-11eb-8159-65886bee3a81.PNG)


# 운영

## CI/CD 설정


각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD 플랫폼은 Azure를 사용하였으며, pipeline build script 는 각 프로젝트 폴더 이하에 cloudbuild.yml 에 포함되었다.


6. Deploy / Pipeline

![image](https://user-images.githubusercontent.com/75237785/105120584-d6232380-5b15-11eb-8422-bb9bfc0cb273.jpg)

![image](https://user-images.githubusercontent.com/75237785/105121326-41212a00-5b17-11eb-840f-d3c3bc369163.jpg)



## 동기식 호출 / 서킷 브레이킹 / 장애격리

7. Circuit Breaker

* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함

시나리오는 대여(rental)-->결제(payment) 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 결제 요청이 과도할 경우 CB 를 통하여 장애격리.

- Hystrix 를 설정:  요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
```
# application.yml

hystrix:
  command:
    # 전역설정
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610

```

- 피호출 서비스(결제:payment) 의 임의 부하 처리 - 400 밀리에서 증감 220 밀리 정도 왔다갔다 하게
```
# Payment.java 

    @PostPersist
    public void onPostPersist(){  //결제이력을 저장한 후 적당한 시간 끌기

        ...
        
        try {
            Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 100명
- 60초 동안 실시

```
$ siege -c100 -t60S -v --content-type "application/json" 'http://rental:8080/rentals POST {"memberId":1, "bookId":1}'
```
 
![image](https://user-images.githubusercontent.com/53402465/105115586-6d837900-5b0c-11eb-81be-448a9d34edea.jpg)
   
![image](https://user-images.githubusercontent.com/53402465/105115589-6f4d3c80-5b0c-11eb-9819-36eab2df1a12.jpg)

- 운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌. 
- 약 97%정도 정상적으로 처리되었음.

8. Autoscale (HPA)
### 오토스케일 아웃
앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 


- 결제서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다:
```
kubectl autoscale deploy payment --min=1 --max=10 --cpu-percent=15
```
- CB 에서 했던 방식대로 워크로드를 2분 동안 걸어준다.
```
siege -c100 -t120S -r10 --content-type "application/json" 'http://localhost:8081/orders POST {"item": "chicken"}'
```
- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:
```
kubectl get deploy pay -w
```
- 어느정도 시간이 흐른 후 스케일 아웃이 벌어지는 것을 확인할 수 있다:

![image](https://user-images.githubusercontent.com/53402465/105116790-c3f1b700-5b0e-11eb-8a8e-80016c453ebd.PNG)

- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다. 

![image](https://user-images.githubusercontent.com/53402465/105116542-50e84080-5b0e-11eb-8da0-33f742007e41.jpg)


9. Zero-downtime deploy (readiness probe)
## 무정지 재배포

* 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함

- seige 로 배포작업 직전에 워크로드를 모니터링 함.
- 새버전으로의 배포 시작

```
kubectl set image ...
```

- readiness 설정

![image](https://user-images.githubusercontent.com/53402465/105119450-b25ede00-5b13-11eb-947b-a2d6da8de334.jpg)

- seige 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인

![image](https://user-images.githubusercontent.com/53402465/105119446-b1c64780-5b13-11eb-9af5-c28364c8870c.jpg)

배포기간중 Availability 가 평소 100%에서 97% 대로 떨어지는 것을 확인. 
원인은 쿠버네티스가 성급하게 새로 올려진 서비스를 READY 상태로 인식하여 서비스 유입을 진행한 것이기 때문. 이를 막기위해 Readiness Probe 를 설정함:

- readiness 설정 수정

![image](https://user-images.githubusercontent.com/53402465/105119444-b12db100-5b13-11eb-9143-04f44194eb64.jpg)

```
kubectl apply -f kubernetes/deployment.yaml
```

- 동일한 시나리오로 재배포 한 후 Availability 확인:

![image](https://user-images.githubusercontent.com/53402465/105119438-af63ed80-5b13-11eb-981e-bb5b1c754cea.jpg)

배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.


# 기능 추가 포인트 시스템

# 추가 서비스 시나리오 
 기능적 요구사항
 1. 사용자가 도서를 반납하면 포인트가 적립된다.
 1. 사용자가 포인트로 도서를 예약할 수 있다.
 1. 포인트가 없으면 결제가 완료되어야 한다.
 1. 사용자가 본인의 포인트 조회를 할 수 있다.
 
  비기능적 요구사항
 1. 트랜잭션
     1. 포인트가 없을 때 결제가 되지 않은 경우 예약처리가 되지 않는다 (Sync 호출)
 1. 장애격리
     1. 도서관리 기능이 수행되지 않더라도 포인트시스템은 365일 24시간 처리가 가능하다  Async (event-driven), Eventual Consistency
     1. 결제 시스템이 과중되면 포인트처리를 잠시동안 처리하지 않는다. Circuit breaker, fallback
 1. 성능
     1. 사용자는 MyPoint 에서 본인 포인트를 확인할 수 있어야한다 CQRS
 
## Event Storming 추가
 * MSAEz 로 모델링한 이벤트스토밍 결과: 
  
 ![모델링](https://user-images.githubusercontent.com/53402465/105257932-1a70fb00-5bcc-11eb-847e-7b6247bf654d.PNG)

## 헥사고날 아키텍처 다이어그램 도출
    
(다이어그램 추가)

 ![00_핵사고날](https://user-images.githubusercontent.com/53402465/105277304-87918a00-5be6-11eb-815b-df34daa6e2cb.PNG)
 
    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐


# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)

```
cd book
mvn spring-boot:run

cd mypage
mvn spring-boot:run 

cd payment
mvn spring-boot:run  

cd rental
mvn spring-boot:run

cd point
mvn spring-boot:run

cd mypoint
mvn spring-boot:run
```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (예시는 point 마이크로 서비스).  

```
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

```

- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package library;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface PointRepository extends PagingAndSortingRepository<Point, Long>{

}
```
- 적용 후 REST API 의 테스트


```
# 사용자 도서 예약 (pay)
http POST http://20.194.22.143:8080/rentals memberId=1 bookId=1 reqPay="pay"
```
![01_예약](https://user-images.githubusercontent.com/53402465/105257934-1ba22800-5bcc-11eb-88cc-0a5ebbb7176a.PNG)
```
# 사용자 도서 반납 
http PATCH http://20.194.22.143:8080/rentals/1 memberId=1 bookId=1 reqPay="pay" reqState="return"
```
![02_반납](https://user-images.githubusercontent.com/53402465/105257936-1c3abe80-5bcc-11eb-9b16-300d02e31b55.PNG)
```
# 사용자 반납한 책 상태 확인 
http GET http://20.194.22.143:8080/books/1
```
![03_책반납상태](https://user-images.githubusercontent.com/53402465/105257937-1c3abe80-5bcc-11eb-8748-f56418a17260.PNG)
```
# 포인트 1 추가 확인 
http http://20.194.22.143:8080/points/1
```
![04_포인트1추가](https://user-images.githubusercontent.com/53402465/105257938-1cd35500-5bcc-11eb-9c5a-3dddf86c9904.PNG)
```
# 사용자 포인트 확인 
http http://20.194.22.143:8080/mypoints/1
```
![05_마이포인트1](https://user-images.githubusercontent.com/53402465/105257941-1d6beb80-5bcc-11eb-9bc1-468ca8c64d22.PNG)
```
# 사용자 도서 포인트로 예약 
http POST http://20.194.22.143:8080/rentals memberId=1 bookId=1 reqPay="point"
```
![06_포인트예약](https://user-images.githubusercontent.com/53402465/105257942-1d6beb80-5bcc-11eb-84fe-e9eeca5ac6f6.PNG)
```
# 포인트 차감 확인 
http http://20.194.22.143:8080/points/1
```
![07_포인트차감](https://user-images.githubusercontent.com/53402465/105257916-180ea100-5bcc-11eb-8373-df15f7e4cb63.PNG)
```
# 사용자 도서 포인트로 예약 (현 포인트 0) 
http POST http://20.194.22.143:8080/rentals memberId=1 bookId=2 reqPay="point"
```
![08_추가예약](https://user-images.githubusercontent.com/53402465/105257921-193fce00-5bcc-11eb-9759-7386a563b50b.PNG)
```
# 포인트 0 확인 
http http://20.194.22.143:8080/points/1
```
![09_포인트차감실패_예약성공](https://user-images.githubusercontent.com/53402465/105257924-193fce00-5bcc-11eb-8f51-1cd708b58887.PNG)
```
# 포인트가 0이라 결제로 
http http://20.194.22.143:8080/payments/2
```
![10_결제확인](https://user-images.githubusercontent.com/53402465/105257927-19d86480-5bcc-11eb-9843-53217da1c478.PNG)
```
# 도서 예약 확인 
http http://20.194.22.143:8080/books/2
```
![11_도서예약확인](https://user-images.githubusercontent.com/53402465/105257929-19d86480-5bcc-11eb-8e48-20bad0fbecc3.PNG)
```
# 사용자 포인트 확인 
http http://20.194.22.143:8080/mypoints/1
```
![12_마포인트0](https://user-images.githubusercontent.com/53402465/105257931-1a70fb00-5bcc-11eb-8220-3527179e55e1.PNG)

## 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 대여(rental)->결제(payment) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- 결제서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

```
# (point) PaymentService.java 내용중

@FeignClient(name="payment", url="${api.payment.url}")
public interface PaymentService {

    @RequestMapping(method= RequestMethod.POST, path="/payments")
    public void payship(@RequestBody Payment payment);

}

```

-  업데이트 이전(@PreUpdate) 결제를 요청하도록 처리
```
# Point.java

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
```
- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 주문도 못받는다는 것을 확인:

# 결제 (payment) 서비스를 잠시 내려놓음

#주문처리
```
http PATCH http://20.194.22.143:8080/points/1 bookId=1 rentalId=4 reqPay="point" point=-1  #Fail 
```
![13_동기식호출 down](https://user-images.githubusercontent.com/53402465/105260793-c36e2480-5bd1-11eb-8791-bca45eadd2b2.PNG)
```
#결제서비스 재기동
cd payment/kubernates
kubectl apply -f deployment.yml
kubectl apply -f service.yaml
```
```
#주문처리
http PATCH http://20.194.22.143:8080/points/1 bookId=1 rentalId=4 reqPay="point" point=-1   #Success
```
![14_동기호출 up](https://user-images.githubusercontent.com/53402465/105260794-c406bb00-5bd1-11eb-8735-edff38024002.PNG)

- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)

## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트

포인트 사용 이후 도서관리(book)시스템으로 결제 완료 여부를 알려주는 행위는 비 동기식으로 처리하여 도서관리 시스템의 처리로 인해 포인트 처리가 블로킹 되지 않도록 처리한다.
- 이를 위하여 기록을 남기기 전에 포인트 사용(used)이 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
# Point.java

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
```
- 도서관리 서비스는 결제완료 이벤트를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```
# PolicyHandler.java (book)
...

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverUsed_(@Payload Used used){

        if(used.isMe()){
            System.out.println("##### listener  : " + used.toJson());

            // 포인트 결제(에약)
            Book book = new Book();

            book.setId(used.getBookId());
            book.setMemberId(used.getId());
            book.setRendtalId(used.getRentalId());
            book.setBookStatus("reserved");

            bookRepository.save(book);
        }
    }

```


도서관리 시스템은 대여/결제와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 도서관리시스템이 유지보수로 인해 잠시 내려간 상태라도 주문을 받는데 문제가 없다:
```
# 도서관리 서비스 (book) 를 잠시 내려놓음

# 포인트 사용 처리
http PATCH http://20.194.22.143:8080/points/1 bookId=1 rentalId=4 reqPay="point" point=1  #Success  
```

![15_비동기down](https://user-images.githubusercontent.com/53402465/105268774-1ea21600-5bd6-11eb-943c-e12750421626.PNG)

``` 

#도서관리 서비스 기동
cd book/kubernates
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

# 책 상태 확인

```
![16_비동기up](https://user-images.githubusercontent.com/53402465/105268771-1e097f80-5bd6-11eb-855b-9f61e1d06b03.PNG)


##폴리그랏 퍼시스턴스

book 는 다른 서비스와 구별을 위해 별도 hsqldb를 사용 하였다. 이를 위해 book내 pom.xml에 dependency를 h2database에서 hsqldb로 변경 하였다.

```
#book의 pom.xml dependency를 수정하여 DB변경

  <!--
  <dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
  </dependency>
  -->

  <dependency>
    <groupId>org.hsqldb</groupId>
    <artifactId>hsqldb</artifactId>
    <version>2.4.0</version>
    <scope>runtime</scope>
  </dependency>
```
##GateWay

```
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: rental
          uri: http://localhost:8081
          predicates:
            - Path=/rentals/** 
        - id: payment
          uri: http://localhost:8082
          predicates:
            - Path=/payments/** 
        - id: mypage
          uri: http://localhost:8083
          predicates:
            - Path= /mypages/**
        - id: book
          uri: http://localhost:8084
          predicates:
            - Path=/books/** 
        - id: point
          uri: http://localhost:8085
          predicates:
            - Path=/points/** 
        - id: mypoint
          uri: http://localhost:8086
          predicates:
            - Path= /mypoints/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: rental
          uri: http://rental:8080
          predicates:
            - Path=/rentals/** 
        - id: payment
          uri: http://payment:8080
          predicates:
            - Path=/payments/** 
        - id: mypage
          uri: http://mypage:8080
          predicates:
            - Path= /mypages/**
        - id: book
          uri: http://book:8080
          predicates:
            - Path=/books/** 
        - id: point
          uri: http://point:8080
          predicates:
            - Path=/points/** 
        - id: mypoint
          uri: http://mypoint:8080
          predicates:
            - Path= /mypoints/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080

```

# 운영

## CI/CD 설정


각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD 플랫폼은 Azure를 사용하였으며, pipeline build script 는 각 프로젝트 폴더 이하에 cloudbuild.yml 에 포함되었다.


6. Deploy / Pipeline

![17_kubegetall](https://user-images.githubusercontent.com/53402465/105260791-c36e2480-5bd1-11eb-8fd9-ada97088d66b.PNG)


## 동기식 호출 / 서킷 브레이킹 / 장애격리

7. Circuit Breaker

* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함

시나리오는 대여(rental)-->결제(payment) 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 결제 요청이 과도할 경우 CB 를 통하여 장애격리.

- Hystrix 를 설정:  요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
```
# application.yml

hystrix:
  command:
    # 전역설정
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610

```

- 피호출 서비스(결제:payment) 의 임의 부하 처리 - 400 밀리에서 증감 220 밀리 정도 왔다갔다 하게
```
# Payment.java 

    @PostPersist
    public void onPostPersist(){  //결제이력을 저장한 후 적당한 시간 끌기

        ...
        
        try {
            Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 200명
- 60초 동안 실시

```
siege -c200 -t60S -r10 -v --content-type "application/json" 'http://point:8080/points/1 PATCH {"rentalId":1, "bookId":1, "point":-1}'
```
 
![18_부하](https://user-images.githubusercontent.com/53402465/105277305-88c2b700-5be6-11eb-869e-93978cd081c3.PNG)

- 운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌. 
- 약 96%정도 정상적으로 처리되었음.

8. Autoscale (HPA)
### 오토스케일 아웃
앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 


- 결제서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다:
```
kubectl autoscale deploy payment --min=1 --max=10 --cpu-percent=15
```
- CB 에서 했던 방식대로 워크로드를 2분 동안 걸어준다.
```
siege -c200 -t60S -r10 -v --content-type "application/json" 'http://point:8080/points/1 PATCH {"rentalId":1, "bookId":1, "point":-1}'
```
- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:
```
kubectl get deploy payment -w
```
- 어느정도 시간이 흐른 후 스케일 아웃이 벌어지는 것을 확인할 수 있다:

![19_모니터링](https://user-images.githubusercontent.com/53402465/105277306-88c2b700-5be6-11eb-8fdc-b6fb550bf984.PNG)

- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다. 

![20_부하성공](https://user-images.githubusercontent.com/53402465/105279431-db05d700-5bea-11eb-8085-b282567120ac.PNG)

9. Zero-downtime deploy (readiness probe)
## 무정지 재배포

* 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함

- seige 로 배포작업 직전에 워크로드를 모니터링 함.
- 새버전으로의 배포 시작

```
kubectl set image ...
```

- readiness 설정
```
spec:
  replicas: 1
  selector:
    matchLabels:
      app: payment
  template:
    metadata:
      labels:
        app: payment
    spec:
      containers:
        - name: payment
          image: skcc011.azurecr.io/payment:latest
          ports:
            - containerPort: 8080
#          readinessProbe:
#            httpGet:
#              path: '/actuator/health'
#              port: 8080
#            initialDelaySeconds: 10
#            timeoutSeconds: 2
#            periodSeconds: 5
#            failureThreshold: 10
#          livenessProbe:
#            httpGet:
#              path: '/actuator/health'
#              port: 8080
#            initialDelaySeconds: 120
#            timeoutSeconds: 2
#            periodSeconds: 5
#            failureThreshold: 5
```

- seige 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인

![21_배포시 오류](https://user-images.githubusercontent.com/53402465/105279440-dccf9a80-5bea-11eb-806f-6bde025febf0.PNG)

배포기간중 Availability 가 평소 100%에서 97% 대로 떨어지는 것을 확인. 
원인은 쿠버네티스가 성급하게 새로 올려진 서비스를 READY 상태로 인식하여 서비스 유입을 진행한 것이기 때문. 이를 막기위해 Readiness Probe 를 설정함:

- readiness 설정 수정
```
spec:
  replicas: 1
  selector:
    matchLabels:
      app: payment
  template:
    metadata:
      labels:
        app: payment
    spec:
      containers:
        - name: payment
          image: skcc011.azurecr.io/payment:latest
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```

```
kubectl apply -f kubernetes/deployment.yaml
```

- 동일한 시나리오로 재배포 한 후 Availability 확인:

![22_무정지재배포](https://user-images.githubusercontent.com/53402465/105279439-dc370400-5bea-11eb-9588-09a8130a5d4b.PNG)

배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.
