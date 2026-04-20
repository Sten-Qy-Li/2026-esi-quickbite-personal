package ee.ut.esi.quickbite.menu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class Price {

    @Column(name = "price_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "price_currency", nullable = false, length = 3)
    private String currency;

    protected Price() {
    }

    public Price(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
}
