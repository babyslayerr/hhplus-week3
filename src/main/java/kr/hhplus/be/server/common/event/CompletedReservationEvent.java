package kr.hhplus.be.server.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CompletedReservationEvent {
    private final Long reservationId;
    private final Long userId;
    private final Long seatId;
    private final Long price;
}
