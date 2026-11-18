package com.api.manojmobiles.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class StoreDeliveryProperties {

    private Store store = new Store();
    private Delivery delivery = new Delivery();

    @Getter
    @Setter
    public static class Store {
        private String name = "Manoj Mobiles";
        private String address = "Sector 18, Noida, Uttar Pradesh";
        private Double lat = 28.5355;
        private Double lng = 77.3910;
    }

    @Getter
    @Setter
    public static class Delivery {
        private Hyperlocal hyperlocal = new Hyperlocal();
        private Standard standard = new Standard();

        @Getter
        @Setter
        public static class Hyperlocal {
            private BigDecimal charge = BigDecimal.valueOf(49);
            private BigDecimal freeAbove = BigDecimal.valueOf(999);
            private Integer estimatedMins = 45;
            private Double maxRadiusKm = 15.0;
        }

        @Getter
        @Setter
        public static class Standard {
            private BigDecimal charge = BigDecimal.ZERO;
            private Integer estimatedDays = 4;
        }
    }
}
