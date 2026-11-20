package sk.kasv.fx.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** One chat message between the customer and the handyman of an order. */
@Entity
@Table(name = "order_messages")
public class OrderMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public OrderMessage() {}
    public OrderMessage(Order order, User sender, String text, LocalDateTime createdAt) {
        this.order = order; this.sender = sender; this.text = text; this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Order getOrder() { return order; }
    public User getSender() { return sender; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
