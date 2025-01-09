package kr.hhplus.be.server.domain.concert;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class ConcertSeat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long seatNo;
    private String status;
    private Long price;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) // 연관계층이 많기 때문에 지연로딩으로 한다
    @JoinColumn(name = "concertSchedule_id")
    private ConcertSchedule concertSchedule;


    public void reserve(User user) {
        // available -> reserved
        this.status = "reserved";
        // set userId
        this.user = user;
    }

    public void setCompletedStatus() {
        this.status = "completed";
    }

    public void setReservedStatus() {
        this.status = "reserved";
    }
}
