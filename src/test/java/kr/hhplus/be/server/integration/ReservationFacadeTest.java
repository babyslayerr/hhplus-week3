package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.domain.concert.ConcertSchedule;
import kr.hhplus.be.server.domain.concert.ConcertScheduleRepository;
import kr.hhplus.be.server.domain.concert.ConcertSeat;
import kr.hhplus.be.server.domain.concert.ConcertSeatRepository;
import kr.hhplus.be.server.domain.queue.Queue;
import kr.hhplus.be.server.domain.queue.QueueService;
import kr.hhplus.be.server.domain.reservation.Reservation;
import kr.hhplus.be.server.domain.reservation.ReservationRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import kr.hhplus.be.server.facade.reservation.ReservationFacade;
import kr.hhplus.be.server.infrastructure.queue.QueueRepositoryImpl;
import kr.hhplus.be.server.presentation.reservation.dto.ReservationResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.shaded.org.yaml.snakeyaml.tokens.Token;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@SpringBootTest
public class ReservationFacadeTest {

    @Autowired
    private QueueService queueService;

    @Autowired
    private ConcertSeatRepository concertSeatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConcertScheduleRepository concertScheduleRepository;

    @Autowired
    private ReservationFacade reservationFacade;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private QueueRepositoryImpl queueRepositoryImpl;

    @Test
    void 콘서트좌석을_예약하면_예약내역이_반환된다() {
        // given - 필요 데이터 삽입
        User user = userRepository.save(User.builder()
                .balance(10000L)
                .username("testUser")
                .build());
        ConcertSchedule concertSchedule = concertScheduleRepository.save(ConcertSchedule.builder()
                .concertDate(LocalDate.now())
                .build());
        ConcertSeat concertSeat = concertSeatRepository.save(ConcertSeat.builder()
                .seatNo(1L)
                .price(5000L)
                .concertSchedule(concertSchedule)
                .status("available")
                .build());


        // when
        ReservationResponse response = reservationFacade.reserveSeat(user.getId(), concertSchedule.getId(), concertSeat.getSeatNo());

        // then
        Assertions.assertNotNull(response);
        Assertions.assertEquals(5000L,response.getPrice());
        Assertions.assertEquals("reserved",response.getStatus());
        Assertions.assertEquals(5000L,response.getConcertSeat().getPrice());
    }

    @Test
    void 콘서트_좌석을_결제한다(){
        // given - 필요 데이터 삽입
        // 토큰 발급
        String uuid = queueService.getToken();
        // 유저 저장
        User user = userRepository.save(User.builder()
                .balance(10000L)
                .username("testUser")
                .build());
        // 콘서트 스케줄 저장
        ConcertSchedule concertSchedule = concertScheduleRepository.save(ConcertSchedule.builder()
                .concertDate(LocalDate.now())
                .build());
        // 콘서트 좌석 저장
        ConcertSeat concertSeat = concertSeatRepository.save(ConcertSeat.builder()
                .seatNo(1L)
                .price(5000L)
                .concertSchedule(concertSchedule)
                .build());
        // 예약 처리
        ReservationResponse response = reservationFacade.reserveSeat(user.getId(), concertSchedule.getId(), concertSeat.getSeatNo());

        // given
        Long userId = user.getId();
        Long concertSeatId = concertSeat.getId();
        Long reservationId = response.getId();
        String tokenUuid = uuid;


        // When
        reservationFacade.makeSeatPayment(userId, concertSeatId, reservationId, tokenUuid);

        // Then
        // 예약 상태 검증
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();
        Assertions.assertEquals("completed",reservation.getStatus());

        // 좌석 상태 검증
        ConcertSeat seat = concertSeatRepository.findById(concertSeatId).orElseThrow();
        Assertions.assertEquals("completed",seat.getStatus());

        // 결제 잔액 확인
        User savedUser = userRepository.findById(userId).orElseThrow();
        Assertions.assertEquals(5000L,savedUser.getBalance());

        // Token 삭제 확인
        Optional<Queue> removedUuid = queueRepositoryImpl.findByUuid(uuid);
        Assertions.assertEquals(true,removedUuid.isEmpty());

    }


//    @Test
//    void 동시성_테스트_좌석_결제() throws InterruptedException, ExecutionException {
//
//        // given - 필요 데이터 삽입
//        // 토큰 발급
//        String uuid = queueService.getToken();
//        // 유저 저장
//        User user = userRepository.save(User.builder()
//                .balance(10000L)
//                .username("testUser")
//                .build());
//        // 콘서트 스케줄 저장
//        ConcertSchedule concertSchedule = concertScheduleRepository.save(ConcertSchedule.builder()
//                .concertDate(LocalDate.now())
//                .build());
//        // 콘서트 좌석 저장
//        ConcertSeat concertSeat = concertSeatRepository.save(ConcertSeat.builder()
//                .seatNo(1L)
//                .price(5000L)
//                .concertSchedule(concertSchedule)
//                .build());
//
//        int threadCount = 10;
//        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//
//        List<Future<Boolean>> results = new ArrayList<>();
//
//        for (int i = 0; i < threadCount; i++) {
//            results.add(executorService.submit(() -> {
//                latch.countDown(); // 모든 스레드가 준비될 때까지 대기
//                latch.await();
//                try {
//                    // 좌석 예약
//                    reservationFacade.reserveSeat(user.getId(), concertSchedule.getId(), concertSeat.getSeatNo());
//                    return true; // 성공
//                } catch (Exception e) {
//                    return false; // 실패
//                }
//            }));
//        }
//
//        executorService.shutdown();
//        executorService.awaitTermination(10, TimeUnit.SECONDS);
//
//        // 테스트 결과 검증
//        int successCount = 0;
//        for (Future<Boolean> result : results) {
//            if (result.get()) {
//                successCount++;
//            }
//        }
//
//        // 한 번만 결제가 성공해야 함
//        Assertions.assertEquals(1, successCount);
//
//    }

}
